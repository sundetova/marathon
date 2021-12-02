package com.malinskiy.marathon.di

import com.google.gson.Gson
import com.malinskiy.marathon.Marathon
import com.malinskiy.marathon.analytics.TrackerFactory
import com.malinskiy.marathon.analytics.external.AnalyticsFactory
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.progress.ProgressReporter
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.time.SystemTimer
import com.malinskiy.marathon.time.Timer
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.time.Clock

val analyticsModule = module {
    single { Track() }
    single { TrackerFactory(get(), get(), get(), get(), get()).create() }
    single { AnalyticsFactory(get()).create() }
}

val coreModule = module {
    single { FileManager(get<Configuration>().outputDir) }
    single { Gson() }
    single<Clock> { Clock.systemDefaultZone() }
    single<Timer> { SystemTimer(get()) }
    single { ProgressReporter(get()) }
    single { Marathon(get(), get(), get(), get(), get(), get()) }
}

fun marathonStartKoin(configuration: Configuration): KoinApplication {
    val configurationModule = module {
        single { configuration }
    }

    return startKoin {
        modules(configurationModule)
        modules(coreModule)
        modules(analyticsModule)
        modules(configuration.vendorConfiguration.modules())
    }
}
