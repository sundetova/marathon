package com.malinskiy.marathon.analytics.external.influx

import com.malinskiy.marathon.analytics.internal.sub.TestEvent
import com.malinskiy.marathon.analytics.internal.sub.TrackerInternalAdapter
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.test.toSafeTestName
import org.influxdb.InfluxDB
import org.influxdb.dto.Point
import java.util.concurrent.TimeUnit

class InfluxDbTracker(
    private val influxDb: InfluxDB,
    private val dbName: String,
    private val rpName: String
) : TrackerInternalAdapter() {

    override fun trackTest(event: TestEvent) {
        //Report only success and failure
        if (event.testResult.status in arrayOf(TestStatus.FAILURE, TestStatus.PASSED)) {

            val testResult = event.testResult
            val device = event.device
            influxDb.write(
                dbName,
                rpName,
                Point.measurement("tests")
                    .time(event.instant.toEpochMilli(), TimeUnit.MILLISECONDS)
                    .tag("testname", testResult.test.toSafeTestName())
                    .tag("package", testResult.test.pkg)
                    .tag("class", testResult.test.clazz)
                    .tag("method", testResult.test.method)
                    .tag("deviceSerial", device.safeSerialNumber)
                    .addField("ignored", if (testResult.isIgnored) 1.0 else 0.0)
                    .addField("success", if (testResult.status == TestStatus.PASSED) 1.0 else 0.0)
                    .addField("duration", testResult.durationMillis())
                    .build()
            )
        }
    }

    override fun close() {
        influxDb.close()
    }
}
