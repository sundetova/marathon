package com.malinskiy.marathon.android

import com.malinskiy.marathon.android.configuration.AllureConfiguration
import com.malinskiy.marathon.android.configuration.AndroidLogConfigurator
import com.malinskiy.marathon.android.configuration.DEFAULT_ALLURE_CONFIGURATION
import com.malinskiy.marathon.android.configuration.FileSyncConfiguration
import com.malinskiy.marathon.android.configuration.SerialStrategy
import com.malinskiy.marathon.android.configuration.ThreadingConfiguration
import com.malinskiy.marathon.android.configuration.TimeoutConfiguration
import com.malinskiy.marathon.android.di.androidModule
import com.malinskiy.marathon.android.model.AndroidTestBundle
import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.execution.TestParser
import com.malinskiy.marathon.execution.bundle.TestBundleIdentifier
import com.malinskiy.marathon.log.MarathonLogConfigurator
import com.malinskiy.marathon.vendor.VendorConfiguration
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

const val defaultInitTimeoutMillis = 30_000

const val DEFAULT_AUTO_GRANT_PERMISSION = false
const val DEFAULT_APPLICATION_PM_CLEAR = false
const val DEFAULT_TEST_APPLICATION_PM_CLEAR = false
const val DEFAULT_INSTALL_OPTIONS = ""
const val DEFAULT_WAIT_FOR_DEVICES_TIMEOUT = 30000L

data class AndroidConfiguration(
    val androidSdk: File,
    val applicationOutput: File?,
    val testApplicationOutput: File?,
    val outputs: List<AndroidTestBundle>? = null,
    val implementationModules: List<Module>,
    val autoGrantPermission: Boolean = DEFAULT_AUTO_GRANT_PERMISSION,
    val instrumentationArgs: Map<String, String> = emptyMap(),
    val applicationPmClear: Boolean = DEFAULT_APPLICATION_PM_CLEAR,
    val testApplicationPmClear: Boolean = DEFAULT_TEST_APPLICATION_PM_CLEAR,
    val adbInitTimeoutMillis: Int = defaultInitTimeoutMillis,
    val installOptions: String = DEFAULT_INSTALL_OPTIONS,
    val serialStrategy: SerialStrategy = SerialStrategy.AUTOMATIC,
    val screenRecordConfiguration: ScreenRecordConfiguration = ScreenRecordConfiguration(),
    val waitForDevicesTimeoutMillis: Long = DEFAULT_WAIT_FOR_DEVICES_TIMEOUT,
    val allureConfiguration: AllureConfiguration = DEFAULT_ALLURE_CONFIGURATION,
    val timeoutConfiguration: TimeoutConfiguration = TimeoutConfiguration(),
    val fileSyncConfiguration: FileSyncConfiguration = FileSyncConfiguration(),
    val threadingConfiguration: ThreadingConfiguration = ThreadingConfiguration(),
) : VendorConfiguration, KoinComponent {

    private val koinModules = listOf(androidModule) + implementationModules + module { single { this@AndroidConfiguration } }
    
    override fun testParser(): TestParser = get()

    override fun deviceProvider(): DeviceProvider = get()

    override fun testBundleIdentifier(): TestBundleIdentifier = get()

    override fun logConfigurator(): MarathonLogConfigurator = AndroidLogConfigurator()

    override fun modules() = koinModules

    fun testBundlesCompat(): List<AndroidTestBundle> {
        return mutableListOf<AndroidTestBundle>().apply {
            outputs?.let { addAll(it) }
            testApplicationOutput?.let {
                add(
                    AndroidTestBundle(
                        applicationOutput,
                        testApplicationOutput
                    )
                )
            }
        }.toList()
    }
}
