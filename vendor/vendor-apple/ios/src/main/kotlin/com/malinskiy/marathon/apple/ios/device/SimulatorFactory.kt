package com.malinskiy.marathon.apple.ios.device

import com.google.gson.Gson
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.apple.ios.AppleSimulatorDevice
import com.malinskiy.marathon.apple.AppleTestBundleIdentifier
import com.malinskiy.marathon.apple.bin.AppleBinaryEnvironment
import com.malinskiy.marathon.apple.cmd.CommandExecutor
import com.malinskiy.marathon.apple.cmd.FileBridge
import com.malinskiy.marathon.apple.configuration.Transport
import com.malinskiy.marathon.apple.logparser.parser.DeviceFailureException
import com.malinskiy.marathon.apple.logparser.parser.DeviceFailureReason
import com.malinskiy.marathon.apple.model.Sdk
import com.malinskiy.marathon.config.Configuration
import com.malinskiy.marathon.config.vendor.VendorConfiguration
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.time.Timer

class SimulatorFactory(
    private val configuration: Configuration,
    private val vendorConfiguration: VendorConfiguration.IOSConfiguration,
    private val testBundleIdentifier: AppleTestBundleIdentifier,
    private val gson: Gson,
    private val track: Track,
    private val timer: Timer,
) {
    private val logger = MarathonLogging.logger {}
    private val fileManager = FileManager(
        configuration.outputConfiguration.maxPath,
        configuration.outputConfiguration.maxFilename,
        configuration.outputDir
    )

    suspend fun create(
        transport: Transport,
        commandExecutor: CommandExecutor,
        fileBridge: FileBridge,
        udid: String,
    ): AppleSimulatorDevice {
        val bin = AppleBinaryEnvironment(commandExecutor, configuration, vendorConfiguration.timeoutConfiguration, gson)
        val simctlDevice = try {
            val simctlDevices = bin.xcrun.simctl.device.listDevices()
            simctlDevices.find { it.udid == udid }?.apply {
                if (isAvailable == false) {
                    throw DeviceFailureException(DeviceFailureReason.InvalidSimulatorIdentifier, "udid $udid is not available")
                }
            } ?: throw DeviceFailureException(DeviceFailureReason.InvalidSimulatorIdentifier, "udid $udid is missing")
        } catch (e: DeviceFailureException) {
            commandExecutor.close()
            throw e
        }

        val simulatorId = simctlDevice.deviceTypeIdentifier?.removePrefix("com.apple.CoreSimulator.SimDeviceType.") ?: ""
        val sdk = if (simulatorId.startsWith("iPhone")) {
            Sdk.IPHONESIMULATOR
        } else if (simulatorId.startsWith("iPad")) {
            Sdk.IPHONESIMULATOR
        } else if (simulatorId.startsWith("Apple-TV")) {
            Sdk.TV_SIMULATOR
        } else if (simulatorId.startsWith("Apple-Watch")) {
            Sdk.WATCH_SIMULATOR
        } else if (simulatorId.startsWith("Apple-Vision")) {
            Sdk.VISION_SIMULATOR
        } else {
            Sdk.IPHONESIMULATOR
        }

        val device = AppleSimulatorDevice(
            simctlDevice.udid,
            transport,
            sdk,
            bin,
            testBundleIdentifier,
            fileManager,
            configuration,
            vendorConfiguration,
            commandExecutor,
            fileBridge,
            track,
            timer
        )
        track.trackProviderDevicePreparing(device)
        {
            device.setup()
        }
        return device
    }
}
