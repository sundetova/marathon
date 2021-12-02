package com.malinskiy.marathon.vendor

import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.execution.TestParser
import com.malinskiy.marathon.execution.bundle.TestBundleIdentifier
import com.malinskiy.marathon.log.MarathonLogConfigurator
import org.koin.core.module.Module

interface VendorConfiguration {
    fun logConfigurator(): MarathonLogConfigurator?
    fun testParser(): TestParser?
    fun deviceProvider(): DeviceProvider?
    fun testBundleIdentifier(): TestBundleIdentifier? = null

    fun modules(): List<Module> = emptyList()
} 
