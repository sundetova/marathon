package com.malinskiy.marathon.android

import com.malinskiy.marathon.android.exception.InstallException
import com.malinskiy.marathon.android.extension.testBundlesCompat
import com.malinskiy.marathon.config.Configuration
import com.malinskiy.marathon.config.vendor.VendorConfiguration
import com.malinskiy.marathon.exceptions.DeviceSetupException
import com.malinskiy.marathon.execution.withRetry
import com.malinskiy.marathon.log.MarathonLogging
import java.io.File

class AndroidAppInstaller(configuration: Configuration) {

    companion object {
        private const val MAX_RETIRES = 3
        private const val MARSHMALLOW_VERSION_CODE = 23
    }

    private val logger = MarathonLogging.logger("AndroidAppInstaller")
    private val androidConfiguration = configuration.vendorConfiguration as VendorConfiguration.AndroidConfiguration

    /**
     * @throws DeviceSetupException if unable to prepare the device
     */
    suspend fun prepareInstallation(device: AndroidDevice) {
        val testBundles = androidConfiguration.testBundlesCompat()
        testBundles.forEach { bundle ->
            val apkParser = ApkParser()
            val applicationInfo = apkParser.parseInstrumentationInfo(bundle.testApplication)

            logger.debug { "Installing application output to ${device.serialNumber}" }
            bundle.application?.let {
                reinstall(device, applicationInfo.applicationPackage, it)
            }

            bundle.extraApplications?.let { extraApplications ->
                extraApplications.forEach { extraApplication ->
                    logger.debug { "Installing extra application to ${device.serialNumber}" }
                    val extraApplicationPackage = apkParser.parseAppPackageName(extraApplication)
                    reinstall(device, extraApplicationPackage, extraApplication)
                }
            }

            logger.debug { "Installing instrumentation package to ${device.serialNumber}" }
            reinstall(device, applicationInfo.instrumentationPackage, bundle.testApplication)
            logger.debug { "Prepare installation finished for ${device.serialNumber}" }
        }
    }

    /**
     * @throws DeviceSetupException if unable to reinstall (even with retries)
     */
    private suspend fun reinstall(device: AndroidDevice, appPackage: String, appApk: File) {
        withRetry(attempts = MAX_RETIRES, delayTime = 1000) {
            try {
                if (installed(device, appPackage)) {
                    logger.info("Uninstalling $appPackage from ${device.serialNumber}")
                    val uninstallMessage = device.safeUninstallPackage(appPackage)?.trim()
                    uninstallMessage?.let { logger.debug { it } }
                }
                logger.info("Installing $appPackage, ${appApk.absolutePath} to ${device.serialNumber}")
                val installMessage = device.installPackage(appApk.absolutePath, true, optionalParams(device))?.trim()
                installMessage?.let { logger.debug { it } }
                if (installMessage == null || !installMessage.contains("Success")) {
                    throw InstallException(installMessage ?: "")
                }
            } catch (e: InstallException) {
                logger.error(e) { "Error while installing $appPackage, ${appApk.absolutePath} on ${device.serialNumber}" }
                throw DeviceSetupException("Error while installing $appPackage on ${device.serialNumber}", e)
            }
        }
    }

    /**
     * @throws DeviceSetupException if unable to verify
     */
    private suspend fun installed(device: AndroidDevice, appPackage: String): Boolean {
        val lines = device.safeExecuteShellCommand("pm list packages")?.lines()
            ?: throw DeviceSetupException("Unable to verify that package $appPackage is installed")
        return lines.any { it == "package:$appPackage" }
    }

    private fun optionalParams(device: AndroidDevice): List<String> {
        return mutableListOf<String>().apply {
            if (device.apiLevel >= MARSHMALLOW_VERSION_CODE && androidConfiguration.autoGrantPermission) {
                add("-g")
            }
            if (androidConfiguration.installOptions.isNotEmpty()) {
                addAll(androidConfiguration.installOptions.split(" "))
            }
        }.toList()
    }
}
