package com.malinskiy.marathon.exceptions

import com.bugsnag.Bugsnag
import com.malinskiy.marathon.config.AppType
import com.malinskiy.marathon.core.BuildConfig
import com.malinskiy.marathon.log.MarathonLogging
import java.util.Base64

private val log = MarathonLogging.logger {}

class BugsnagExceptionsReporter : ExceptionsReporter {
    private val bugsnag: Bugsnag? by lazy {
        val bugsnagToken = BuildConfig.BUGSNAG_TOKEN
        if (bugsnagToken.isNullOrEmpty()) {
            null
        } else {
            val bytes = Base64.getDecoder().decode(bugsnagToken)
            Bugsnag(String(bytes))
        }
    }

    override fun start(appType: AppType) {
        bugsnag?.apply {
            log.info { "Init BugSnag" }
            setAppType(appType.value)
            setAppVersion(BuildConfig.VERSION)
            /**
             * This exception is bloating our events. Sample stacktrace usually includes:
             *
             * java.lang.ThreadDeath
             * at java.lang.Thread.stop(Thread.java:853)
             * at org.jetbrains.kotlin.daemon.client.KotlinCompilerClient.startDaemon(KotlinCompilerClient.kt:447)
             * ...
             */
            setIgnoreClasses("java.lang.ThreadDeath")
            setProjectPackages("com.malinskiy")
            when (BuildConfig.RELEASE_MODE) {
                "RELEASE" -> setReleaseStage("production")
                "SNAPSHOT" -> setReleaseStage("development")
                else -> setReleaseStage("development")
            }
            setNotifyReleaseStages("production", "development")
        }
    }

    override fun end() {
        bugsnag?.apply {
           log.info { "Finish BugSnag" }
           close()
        }
    }
}
