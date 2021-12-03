package com.malinskiy.marathon.execution.filter

import com.malinskiy.marathon.config.TestFilterConfiguration
import com.malinskiy.marathon.execution.TestFilter
import com.malinskiy.marathon.test.Test
import java.io.File

data class SimpleClassnameFilterFromFile(val cnf: TestFilterConfiguration.SimpleClassnameFilterFromFile) : TestFilter {

    var testFileContent: String = readFileAsLinesUsingBufferedReader(cnf.fileName).get(0)

    override fun filter(tests: List<Test>): List<Test> {
        if (testFileContent.length > 5) {
            return tests.filter {
                testFileContent.contains(it.clazz)
            }
        } else if (cnf.regex != null) {
            println("No file content found. Running by annotation ${cnf.regex}")
            val regex:Regex = cnf.regex!!
            return tests.filter { it.metaProperties.map { it.name }.any(regex::matches) }
        } else {
            println("No regex provided, running all tests")
            return tests
        }
    }

    override fun filterNot(tests: List<Test>): List<Test> = tests.filterNot { testFileContent.contains(it.clazz) }

    override fun equals(other: Any?): Boolean {
        if (other !is SimpleClassnameFilterFromFile) return false
        return cnf.fileName.contentEquals(other.cnf.regex.toString())
    }

    override fun hashCode(): Int = testFileContent.hashCode()

    override fun toString(): String {
        return "SimpleClassnameFilterFromFile(fileName=${cnf.fileName})"
    }
}

fun readFileAsLinesUsingBufferedReader(fileName: String): List<String> = File(fileName).bufferedReader().readLines()
