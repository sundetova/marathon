package com.malinskiy.marathon.apple.bin.xcrun.simctl.service

import com.malinskiy.marathon.apple.bin.xcrun.simctl.SimctlService
import com.malinskiy.marathon.apple.cmd.CommandExecutor
import com.malinskiy.marathon.apple.cmd.CommandResult
import com.malinskiy.marathon.config.vendor.apple.ios.Codec
import com.malinskiy.marathon.config.vendor.apple.ios.Display
import com.malinskiy.marathon.config.vendor.apple.ios.Mask
import com.malinskiy.marathon.config.vendor.apple.TimeoutConfiguration
import com.malinskiy.marathon.config.vendor.apple.ios.Type
import com.malinskiy.marathon.log.MarathonLogging

class IoService(
    commandExecutor: CommandExecutor,
    private val timeoutConfiguration: TimeoutConfiguration,
) : SimctlService(commandExecutor) {
    private val logger = MarathonLogging.logger {}

    suspend fun screenshot(udid: String, destination: String, type: Type, display: Display, mask: Mask): Boolean {
        val result = safeExecute(
            timeout = timeoutConfiguration.screenshot,
            "io", udid, "screenshot", "--type=${type.value}", "--display=${display.value}", "--mask=${mask.value}", destination
        )
        return if (result?.successful == true) {
            true
        } else {
            logger.debug { "failed to capture screenshot, stdout=${result?.combinedStdout}, stderr=${result?.combinedStderr}" }
            false
        }
    }

    /**
     * recording video requires us to send SIGINT which is not available via JVM Process API
     * SIGINT also doesn't work using ssh because OpenSSH terminates the connection when the exec request's process receives SIGINT
     */
    suspend fun recordVideo(udid: String, remotePath: String, codec: Codec, display: Display, mask: Mask, pidfile: String): CommandResult? {
        return safeExecuteNohup(
            pidfile,
            timeoutConfiguration.video,
            "io", udid, "recordVideo",
            "--codec",
            codec.value,
            "--display",
            display.value,
            "--mask",
            mask.value,
            "--force",
            remotePath
        )
    }
}
