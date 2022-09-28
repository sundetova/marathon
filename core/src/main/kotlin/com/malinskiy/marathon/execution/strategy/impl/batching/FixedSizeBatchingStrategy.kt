package com.malinskiy.marathon.execution.strategy.impl.batching

import com.malinskiy.marathon.analytics.external.Analytics
import com.malinskiy.marathon.config.strategy.BatchingStrategyConfiguration
import com.malinskiy.marathon.execution.bundle.TestBundle
import com.malinskiy.marathon.execution.bundle.TestBundleIdentifier
import com.malinskiy.marathon.execution.strategy.BatchingStrategy
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestBatch
import java.util.Queue

const val FIXED_BATCH_SIZE = 10

class FixedSizeBatchingStrategy(private val cnf: BatchingStrategyConfiguration.FixedSizeBatchingStrategyConfiguration) : BatchingStrategy {

    var fixedBatchSize = 0

    override fun process(
        queue: Queue<Test>,
        analytics: Analytics,
        testBundleIdentifier: TestBundleIdentifier?,
        filteredTestsCount: Int,
        deviceCount: Int
    ): TestBatch {

        if (queue.size < cnf.lastMileLength && queue.isNotEmpty()) {
            //We optimize last mile by disabling batching completely.
            // This allows us to parallelize the test runs at the end instead of running batches in series
            return TestBatch(listOf(queue.poll()))
        }

        var counter = 0
        var expectedBatchDuration = 0.0
        val unbatchableTests = mutableListOf<Test>()
        val result = mutableSetOf<Test>()
        var testBundle: TestBundle? = null
        val batchSize = if (filteredTestsCount / deviceCount > FIXED_BATCH_SIZE) {
            FIXED_BATCH_SIZE
        } else {
            filteredTestsCount / deviceCount + 1
        }
        fixedBatchSize = batchSize

        while (counter < batchSize && queue.isNotEmpty()) {
            counter++
            val item = queue.poll()
            if (result.contains(item)) {
                unbatchableTests.add(item)
            } else if (testBundle != null && testBundleIdentifier?.identify(item) != testBundle) {
                unbatchableTests.add(item)
            } else {
                result.add(item)
            }

            val durationMillis = cnf.durationMillis
            val percentile = cnf.percentile
            val timeLimit = cnf.timeLimit
            if (durationMillis != null && percentile != null && timeLimit != null) {
                //Check for expected batch duration. If we hit the duration limit - break
                //Important part is to add at least one test so that if one test is longer than a batch
                //We still have at least one test
                val expectedTestDuration = analytics.metricsProvider.executionTime(item, percentile, timeLimit)
                expectedBatchDuration += expectedTestDuration
                if (expectedBatchDuration >= durationMillis) break
            }
        }
        if (unbatchableTests.isNotEmpty()) {
            queue.addAll(unbatchableTests)
        }
        return TestBatch(result.toList())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FixedSizeBatchingStrategy

        if (fixedBatchSize != other.fixedBatchSize) return false

        return true
    }

    override fun hashCode(): Int {
        return fixedBatchSize
    }

    override fun toString(): String {
        return "FixedSizeBatchingStrategy(size=${fixedBatchSize}, durationMillis=${cnf.durationMillis}, percentile=${cnf.percentile}, timeLimit=${cnf.timeLimit}, lastMileLength=${cnf.lastMileLength})"
    }
}
