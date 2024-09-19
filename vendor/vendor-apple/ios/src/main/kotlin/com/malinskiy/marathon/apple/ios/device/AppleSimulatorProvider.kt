package com.malinskiy.marathon.apple.ios.device

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.Gson
import com.malinskiy.marathon.actor.unboundedChannel
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.apple.AppleDevice
import com.malinskiy.marathon.apple.AppleTestBundleIdentifier
import com.malinskiy.marathon.apple.bin.AppleBinaryEnvironment
import com.malinskiy.marathon.apple.bin.xcrun.simctl.model.SimctlDevice
import com.malinskiy.marathon.apple.bin.xcrun.simctl.model.SimctlListDevicesOutput
import com.malinskiy.marathon.apple.ios.AppleSimulatorDevice
import com.malinskiy.marathon.apple.cmd.CommandExecutor
import com.malinskiy.marathon.apple.cmd.FileBridge
import com.malinskiy.marathon.apple.configuration.AppleTarget
import com.malinskiy.marathon.apple.configuration.Marathondevices
import com.malinskiy.marathon.apple.configuration.Transport
import com.malinskiy.marathon.apple.configuration.Worker
import com.malinskiy.marathon.apple.device.ConnectionFactory
import com.malinskiy.marathon.config.Configuration
import com.malinskiy.marathon.config.vendor.VendorConfiguration
import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.exceptions.NoDevicesException
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.time.Timer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.apache.commons.text.StringSubstitutor
import org.apache.commons.text.lookup.StringLookupFactory
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

class AppleSimulatorProvider(
    private val configuration: Configuration,
    private val vendorConfiguration: VendorConfiguration.IOSConfiguration,
    private val testBundleIdentifier: AppleTestBundleIdentifier,
    private val gson: Gson,
    private val objectMapper: ObjectMapper,
    private val track: Track,
    private val timer: Timer
) : DeviceProvider, CoroutineScope {

    private val logger = MarathonLogging.logger(AppleSimulatorProvider::class.java.simpleName)

    private val dispatcher =
        newFixedThreadPoolContext(vendorConfiguration.threadingConfiguration.deviceProviderThreads, "AppleDeviceProvider")
    override val coroutineContext: CoroutineContext
        get() = dispatcher

    private var monitoringJob: Job? = null

    private val devices = ConcurrentHashMap<String, AppleSimulatorDevice>()
    private val channel: Channel<DeviceProvider.DeviceEvent> = unboundedChannel()
    private val connectionFactory = ConnectionFactory(
        configuration,
        vendorConfiguration.ssh,
        vendorConfiguration.rsync,
        vendorConfiguration.timeoutConfiguration.reachability
    )
    private val environmentVariableSubstitutor = StringSubstitutor(StringLookupFactory.INSTANCE.environmentVariableStringLookup())
    private val simulatorFactory = SimulatorFactory(configuration, vendorConfiguration, testBundleIdentifier, gson, track, timer)

    override fun subscribe() = channel

    override suspend fun initialize() {
        logger.debug("Initializing AppleSimulatorProvider")
        val file = vendorConfiguration.devicesFile ?: File(System.getProperty("user.dir"), "Marathondevices")
        val devicesWithEnvironmentVariablesReplaced = environmentVariableSubstitutor.replace(file.readText())
        val workers: List<Worker> = try {
            objectMapper.readValue<Marathondevices>(devicesWithEnvironmentVariablesReplaced).workers
        } catch (e: JsonMappingException) {
            throw NoDevicesException("Invalid Marathondevices file ${file.absolutePath} format", e)
        }
        if (workers.isEmpty()) {
            throw NoDevicesException("No workers found in the ${file.absolutePath}")
        }
        val hosts: Map<Transport, List<AppleTarget>> = mutableMapOf<Transport, List<AppleTarget>>().apply {
            workers.map {
                put(it.transport, it.devices)
            }
        }

        logger.debug { "Establishing communication with [${hosts.keys.joinToString()}]" }
        val deferred = hosts.map { (transport, targets) ->
            async {
                initializeForTransport(targets, transport)
            }
        }
        awaitAll(*deferred.toTypedArray())

        monitoringJob = launch {
            while (isActive) {
                var recreate = mutableSetOf<AppleDevice>()
                devices.values.forEach { device ->
                    if (!device.commandExecutor.connected) {
                        channel.send(DeviceProvider.DeviceEvent.DeviceDisconnected(device))
                        device.dispose()
                        recreate.add(device)
                    }
                }
                val byTransport = recreate.groupBy { it.transport }
                byTransport.forEach { (transport, devices) ->
                    val plan = ProvisioningPlan(existingSimulators = devices.map {
                        logger.warn { "Re-provisioning ${it.serialNumber}" }
                        it.udid
                    }.toSet(), emptyList(), emptySet())
                    createExisting(plan, transport)
                }
                recreate.clear()
                delay(16)
            }
        }
        Unit
    }

    override suspend fun borrow(): Device {
        while (devices.isEmpty()) {
            delay(200)
        }
        return devices.values.random()
    }

    private suspend fun initializeForTransport(targets: List<AppleTarget>, transport: Transport) {
        val (commandExecutor, fileBridge) = connectionFactory.create(transport)
        if (commandExecutor == null) {
            return
        }

        val bin = AppleBinaryEnvironment(commandExecutor, configuration, vendorConfiguration.timeoutConfiguration, gson)
        val plan = plan(transport, bin, targets)
        val deferredExisting = createExisting(plan, transport)
        val deferredProvisioning = createNew(transport, plan, bin)
        (deferredExisting + deferredProvisioning).awaitAll()
        connectionFactory.dispose(commandExecutor)
    }

    private suspend fun createExisting(plan: ProvisioningPlan, transport: Transport): List<Deferred<Unit>> {
        return plan.existingSimulators.map { udid ->
            supervisorScope {
                async {
                    val (commandExecutor, fileBridge) = connectionFactory.create(transport)
                    if (commandExecutor == null) {
                        return@async
                    }
                    val simulator = createSimulator(udid, transport, commandExecutor, fileBridge)
                    connect(transport, simulator)
                }
            }
        }
    }

    private suspend fun createSimulator(
        udid: String,
        transport: Transport,
        commandExecutor: CommandExecutor,
        fileBridge: FileBridge,
    ): AppleSimulatorDevice {
        return simulatorFactory.create(transport, commandExecutor, fileBridge, udid)
    }

    private suspend fun createNew(
        transport: Transport,
        plan: ProvisioningPlan,
        bin: AppleBinaryEnvironment,
    ): List<Deferred<Unit>> {
        return plan.needsProvisioning.map { profile ->
            supervisorScope {
                async {
                    val simctlListDevicesOutput = bin.xcrun.simctl.device.list()
                    if (!verifySimulatorCanBeProvisioned(simctlListDevicesOutput, profile, transport)) {
                        return@async
                    }

                    val name = "${profile.newNamePrefix}-${UUID.randomUUID()}"
                    val deviceTypeId = profile.fullyQualifiedDeviceTypeId
                    val udid = bin.xcrun.simctl.simulator.create(
                        name,
                        deviceTypeId,
                        profile.fullyQualifiedRuntimeId
                    )
                    if (udid != null) {
                        val (commandExecutor, fileBridge) = connectionFactory.create(transport)
                        if (commandExecutor == null) {
                            return@async
                        }
                        val simulator = createSimulator(udid, transport, commandExecutor, fileBridge)
                        connect(transport, simulator)
                    } else {
                        logger.error { "Failed to create simulator for profile $profile" }
                    }
                }
            }
        }
    }

    private fun verifySimulatorCanBeProvisioned(
        simctlListDevicesOutput: SimctlListDevicesOutput,
        profile: AppleTarget.SimulatorProfile,
        transport: Transport
    ): Boolean {
        if (!simctlListDevicesOutput.devicetypes.any {
                it.identifier == profile.fullyQualifiedDeviceTypeId
            }) {
            logger.error { "device type ${profile.fullyQualifiedDeviceTypeId} is not available at $transport" }
            return false
        }
        if (profile.fullyQualifiedRuntimeId != null && !simctlListDevicesOutput.runtimes.any {
                it.identifier == profile.fullyQualifiedRuntimeId
            }) {
            logger.error { "runtime ${profile.fullyQualifiedRuntimeId} is not available at $transport" }
            return false
        }
        return true
    }

    /**
     * Per-host provisioning
     */
    suspend fun plan(transport: Transport, bin: AppleBinaryEnvironment, targets: List<AppleTarget>): ProvisioningPlan {
        val simulatorDevices: Map<String, SimctlDevice> = bin.xcrun.simctl.device.listDevices()
            .filter { it.isAvailable ?: false }
            .groupBy { it.udid }
            .mapValues { it.value.first() } //This will fail silently if the same udid is used twice 

        val simulators = mutableListOf<AppleTarget.Simulator>()
        val simulatorProfiles = mutableListOf<AppleTarget.SimulatorProfile>()
        val physical = mutableListOf<AppleTarget.Physical>()
        targets.forEach { device ->
            when (device) {
                is AppleTarget.Simulator -> simulators.add(device)
                is AppleTarget.Physical -> physical.add(device)
                is AppleTarget.SimulatorProfile -> simulatorProfiles.add(device)
                is AppleTarget.Host -> logger.warn { "iOS vendor runs do not use host device for testing. Skipping" }
            }
        }
        val availableUdids = simulatorDevices.keys
        val usedUdids = mutableSetOf<String>()
        simulators.forEach {
            if (!availableUdids.contains(it.udid)) {
                logger.error { "udid ${it.udid} is not available at $transport" }
            } else {
                usedUdids.add(it.udid)
            }
        }
        val unusedDevices = simulatorDevices.filterKeys { !usedUdids.contains(it) }.toMutableMap()
        val reuseUdid = mutableSetOf<String>()
        val createProfiles = mutableListOf<AppleTarget.SimulatorProfile>()
        simulatorProfiles.forEach { profile ->
            reuseExistingSimulator(unusedDevices - reuseUdid, profile)?.let {
                reuseUdid.add(it)
                unusedDevices.remove(it)
            } ?: createProfiles.add(profile)
        }

        //Maybe we should sanity-check if these are available
        val physicalUdids = physical.map { it.udid }.toSet()

        if (vendorConfiguration.lifecycleConfiguration.shutdownUnused) {
            unusedDevices.filter {
                it.value.state == SimctlDevice.State.Booted
            }.forEach {
                bin.xcrun.simctl.simulator.shutdown(it.key)
            }
        }

        return ProvisioningPlan(usedUdids + reuseUdid, createProfiles, physicalUdids)
    }

    /**
     * @return udid of reusable simulator or null if nothing matches
     */
    private fun reuseExistingSimulator(
        devices: Map<String, SimctlDevice>,
        profile: AppleTarget.SimulatorProfile,
    ): String? {
        return devices.values.find {
            it.deviceTypeIdentifier == profile.fullyQualifiedDeviceTypeId &&
                (profile.fullyQualifiedRuntimeId?.let { fqri -> fqri == it.runtime } ?: true)
        }?.udid
    }

    override suspend fun terminate() = withContext(NonCancellable) {
        withContext(NonCancellable) {
            logger.debug { "Terminating AppleSimulatorProvider" }
            monitoringJob?.cancel()
            channel.close()
            if (logger.isDebugEnabled) {
                // print out final summary on attempted simulator connections
                //printFailingSimulatorSummary()
            }
            val deferredDispose = devices.map { (uuid, device) ->
                async {
                    try {
                        dispose(device)
                        connectionFactory.dispose(device.commandExecutor)
                    } catch (e: Exception) {
                        //We don't really care during termination about exceptions
                    }
                    logger.debug("Disposed device ${device.udid}")
                }
            }
            deferredDispose.awaitAll()
            devices.clear()
        }
        dispatcher.close()
    }

//    suspend fun onDisconnect(device: AppleSimulatorDevice, remoteSimulator: AppleTarget.Simulator, reason: DeviceFailureReason) =
//        withContext(coroutineContext + CoroutineName("onDisconnect")) {
//            launch(context = coroutineContext + job + CoroutineName("disconnector")) {
//                try {
//                    if (devices.remove(device.serialNumber, device)) {
//                        dispose(device)
//                        notifyDisconnected(device)
//                    }
//                } catch (e: Exception) {
//                    logger.debug("Exception removing device ${device.udid}")
//                }
//            }
//
//            if (reason == DeviceFailureReason.InvalidSimulatorIdentifier) {
//                logger.error("device ${device.udid} does not exist on remote host")
//            } else if (RemoteSimulatorConnectionCounter.get(device.udid) < MAX_CONNECTION_ATTEMPTS) {
//                launch(context = coroutineContext + job + CoroutineName("reconnector")) {
//                    delay(499)
//                    RemoteSimulatorConnectionCounter.putAndGet(device.udid)
//                    simulatorFactory.createRemote(remoteSimulator)?.let {
//                        connect(it)
//                    }
//                }
//            }
//        }

    private fun dispose(device: AppleSimulatorDevice) {
        device.dispose()
    }

    private fun connect(transport: Transport, device: AppleSimulatorDevice) {
        //Should not use udid as key here to support multiple devices with the same udid across transports
        devices.put(device.serialNumber, device)
            ?.let {
                logger.error("replaced existing device $it with new $device.")
                dispose(it)
            }
        notifyConnected(device)
    }

    private fun notifyConnected(device: AppleSimulatorDevice) = launch(context = coroutineContext) {
        channel.send(element = DeviceProvider.DeviceEvent.DeviceConnected(device))
    }

    private fun notifyDisconnected(device: AppleSimulatorDevice) = launch(context = coroutineContext) {
        channel.send(element = DeviceProvider.DeviceEvent.DeviceDisconnected(device))
    }

//    private fun printFailingSimulatorSummary() {
//        simulators
//            .map { "${it.udid}@${it.transport}" to (RemoteSimulatorConnectionCounter.get(it.udid) - 1) }
//            .filter { it.second > 0 }
//            .sortedByDescending { it.second }
//            .forEach {
//                logger.debug(String.format("%3d %s", it.second, it.first))
//            }
//    }
}
