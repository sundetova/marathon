package com.malinskiy.marathon.gradle

import com.malinskiy.marathon.config.ScreenRecordingPolicy
import com.malinskiy.marathon.config.vendor.VendorConfiguration
import com.malinskiy.marathon.config.vendor.android.AdbEndpoint
import com.malinskiy.marathon.config.vendor.android.AllureConfiguration
import com.malinskiy.marathon.config.vendor.android.FileSyncConfiguration
import com.malinskiy.marathon.config.vendor.android.ScreenRecordConfiguration
import com.malinskiy.marathon.config.vendor.android.SerialStrategy
import com.malinskiy.marathon.config.vendor.android.TestAccessConfiguration
import com.malinskiy.marathon.config.vendor.android.TestParserConfiguration
import com.malinskiy.marathon.config.vendor.android.TimeoutConfiguration
import groovy.lang.Closure
import org.gradle.api.Project

open class MarathonExtension(project: Project) {
    var name: String = "Marathon"

    var vendor: VendorConfiguration.AndroidConfiguration.VendorType? = null
    var bugsnag: Boolean? = null

    var analyticsConfiguration: AnalyticsConfig? = null

    var poolingStrategy: PoolingStrategyConfiguration? = null
    var shardingStrategy: ShardingStrategyConfiguration? = null
    var sortingStrategy: SortingStrategyConfiguration? = null
    var batchingStrategy: BatchingStrategyConfiguration? = null
    var flakinessStrategy: FlakinessStrategyConfiguration? = null
    var retryStrategy: RetryStrategyConfiguration? = null
    var filteringConfiguration: FilteringPluginConfiguration? = null

    var baseOutputDir: String? = null

    var ignoreFailures: Boolean? = null
    var isCodeCoverageEnabled: Boolean? = null
    var fallbackToScreenshots: Boolean? = null
    var strictMode: Boolean? = null
    var uncompletedTestRetryQuota: Int? = null

    var testClassRegexes: Collection<String>? = null
    var includeSerialRegexes: Collection<String>? = null
    var excludeSerialRegexes: Collection<String>? = null

    var testBatchTimeoutMillis: Long? = null
    var testOutputTimeoutMillis: Long? = null
    var debug: Boolean? = null

    var screenRecordingPolicy: ScreenRecordingPolicy? = null

    var applicationPmClear: Boolean? = null
    var testApplicationPmClear: Boolean? = null
    var adbInitTimeout: Int? = null
    var installOptions: String? = null
    var serialStrategy: SerialStrategy? = null

    var screenRecordConfiguration: ScreenRecordConfiguration? = null

    var analyticsTracking: Boolean = false

    var deviceInitializationTimeoutMillis: Long? = null
    var waitForDevicesTimeoutMillis: Long? = null

    var allureConfiguration: AllureConfiguration? = null
    var timeoutConfiguration: TimeoutConfiguration? = null
    var fileSyncConfiguration: FileSyncConfiguration? = null

    var testParserConfiguration: TestParserConfiguration? = null

    //Android specific for now
    var autoGrantPermission: Boolean? = null
    var instrumentationArgs: MutableMap<String, String> = mutableMapOf()

    var testAccessConfiguration: TestAccessConfiguration? = null

    var adbServers: List<AdbEndpoint>? = null

    //Kotlin way
    fun analytics(block: AnalyticsConfig.() -> Unit) {
        analyticsConfiguration = AnalyticsConfig().also(block)
    }

    fun batchingStrategy(block: BatchingStrategyConfiguration.() -> Unit) {
        batchingStrategy = BatchingStrategyConfiguration().also(block)
    }

    fun flakinessStrategy(block: FlakinessStrategyConfiguration.() -> Unit) {
        flakinessStrategy = FlakinessStrategyConfiguration().also(block)
    }

    fun poolingStrategy(block: PoolingStrategyConfiguration.() -> Unit) {
        poolingStrategy = PoolingStrategyConfiguration().also(block)
    }

    fun retryStrategy(block: RetryStrategyConfiguration.() -> Unit) {
        retryStrategy = RetryStrategyConfiguration().also(block)
    }

    fun shardingStrategy(block: ShardingStrategyConfiguration.() -> Unit) {
        shardingStrategy = ShardingStrategyConfiguration().also(block)
    }

    fun sortingStrategy(block: SortingStrategyConfiguration.() -> Unit) {
        sortingStrategy = SortingStrategyConfiguration().also(block)
    }

    fun filteringConfiguration(block: FilteringPluginConfiguration.() -> Unit) {
        filteringConfiguration = FilteringPluginConfiguration().also(block)
    }

    fun instrumentationArgs(block: MutableMap<String, String>.() -> Unit) {
        instrumentationArgs = mutableMapOf<String, String>().also(block)
    }

    fun allureConfiguration(block: AllureConfiguration.() -> Unit) {
        allureConfiguration = AllureConfiguration().also(block)
    }

    fun timeoutConfiguration(block: TimeoutConfiguration.() -> Unit) {
        timeoutConfiguration = TimeoutConfiguration().also(block)
    }

    fun fileSyncConfiguration(block: FileSyncConfiguration.() -> Unit) {
        fileSyncConfiguration = FileSyncConfiguration().also(block)
    }

    //Groovy way
    fun analytics(closure: Closure<*>) {
        analyticsConfiguration = AnalyticsConfig()
        closure.delegate = analyticsConfiguration
        closure.call()
    }

    fun batchingStrategy(closure: Closure<*>) {
        batchingStrategy = BatchingStrategyConfiguration()
        closure.delegate = batchingStrategy
        closure.call()
    }

    fun flakinessStrategy(closure: Closure<*>) {
        flakinessStrategy = FlakinessStrategyConfiguration()
        closure.delegate = flakinessStrategy
        closure.call()
    }

    fun poolingStrategy(closure: Closure<*>) {
        poolingStrategy = PoolingStrategyConfiguration()
        closure.delegate = poolingStrategy
        closure.call()
    }

    fun retryStrategy(closure: Closure<*>) {
        retryStrategy = RetryStrategyConfiguration()
        closure.delegate = retryStrategy
        closure.call()
    }

    fun shardingStrategy(closure: Closure<*>) {
        shardingStrategy = ShardingStrategyConfiguration()
        closure.delegate = shardingStrategy
        closure.call()
    }

    fun sortingStrategy(closure: Closure<*>) {
        sortingStrategy = SortingStrategyConfiguration()
        closure.delegate = sortingStrategy
        closure.call()
    }

    fun filteringConfiguration(closure: Closure<*>) {
        filteringConfiguration = FilteringPluginConfiguration()
        closure.delegate = filteringConfiguration
        closure.call()
    }

    fun instrumentationArgs(closure: Closure<*>) {
        instrumentationArgs = mutableMapOf()
        closure.delegate = instrumentationArgs
        closure.call()
    }

    fun allureConfiguration(closure: Closure<*>) {
        allureConfiguration = AllureConfiguration()
        closure.delegate = allureConfiguration
        closure.call()
    }

    fun timeoutConfiguration(closure: Closure<*>) {
        timeoutConfiguration = TimeoutConfiguration()
        closure.delegate = timeoutConfiguration
        closure.call()
    }

    fun fileSyncConfiguration(closure: Closure<*>) {
        fileSyncConfiguration = FileSyncConfiguration()
        closure.delegate = fileSyncConfiguration
        closure.call()
    }
}
