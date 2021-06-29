package com.malinskiy.marathon.execution

import com.fasterxml.jackson.annotation.JsonProperty
import com.malinskiy.marathon.test.Test
import java.io.File

data class SimpleClassnameFilterFromFile(
    @JsonProperty("fileName") val fileName: String,
    @JsonProperty("regex") val regex: Regex
) : TestFilter {

    var testFileContent: String = readFileAsLinesUsingBufferedReader(fileName).get(0)
    override fun validate() = Unit

    override fun filter(tests: List<Test>): List<Test> {
        if (testFileContent.length > 5) {
            return tests.filter {
                testFileContent.contains(it.clazz)
            }
        } else {
            println("No file content found. Running by annotation $regex")
            return tests.filter { it.metaProperties.map { it.name }.any(regex::matches) }
        }
    }

    override fun filterNot(tests: List<Test>): List<Test> = tests.filterNot { testFileContent.contains(it.clazz) }

    override fun equals(other: Any?): Boolean {
        if (other !is SimpleClassnameFilter) return false
        return fileName.contentEquals(other.regex.toString())
    }

    override fun hashCode(): Int = testFileContent.hashCode()

    override fun toString(): String {
        return "SimpleClassnameFilterFromFile(fileName=$fileName)"
    }
}

fun readFileAsLinesUsingBufferedReader(fileName: String): List<String> = File(fileName).bufferedReader().readLines()
