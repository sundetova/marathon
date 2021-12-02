package com.malinskiy.marathon.report.junit.model

data class TestCaseData(
    val classname: String,
    val name: String,
    val time: String,
    val skipped: StackTraceElement,
    val failure: StackTraceElement
)
