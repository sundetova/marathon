package com.malinskiy.marathon.execution

import com.malinskiy.marathon.test.Test

interface TestParser {
    suspend fun extract(): List<Test>
}
