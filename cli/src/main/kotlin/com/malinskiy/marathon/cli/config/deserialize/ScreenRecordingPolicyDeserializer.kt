package com.malinskiy.marathon.cli.config.deserialize

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.malinskiy.marathon.exceptions.ConfigurationException
import com.malinskiy.marathon.execution.policy.ScreenRecordingPolicy

class ScreenRecordingPolicyDeserializer : StdDeserializer<ScreenRecordingPolicy>(ScreenRecordingPolicy::class.java) {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): ScreenRecordingPolicy? {
        val value: String = p?.valueAsString ?: return null
        return when (value) {
            "ON_FAILURE" -> ScreenRecordingPolicy.ON_FAILURE
            "ON_ANY" -> ScreenRecordingPolicy.ON_ANY
            else -> throw ConfigurationException("Unrecognized screen recording policy $value")
        }
    }
}
