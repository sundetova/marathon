package com.malinskiy.marathon.execution.strategy.impl.retry

import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestShard
import com.malinskiy.marathon.execution.progress.PoolProgressAccumulator
import com.malinskiy.marathon.execution.strategy.RetryStrategy

class NoRetryStrategy : RetryStrategy {
    override fun process(
        devicePoolId: DevicePoolId,
        tests: Collection<TestResult>,
        testShard: TestShard,
        poolProgressAccumulator: PoolProgressAccumulator
    ): List<TestResult> {
        return emptyList()
    }

    override fun hashCode() = javaClass.canonicalName.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        val javaClass: Class<Any> = other.javaClass
        return this.javaClass.canonicalName == javaClass.canonicalName
    }

    override fun toString(): String {
        return "NoRetryStrategy()"
    }


}
