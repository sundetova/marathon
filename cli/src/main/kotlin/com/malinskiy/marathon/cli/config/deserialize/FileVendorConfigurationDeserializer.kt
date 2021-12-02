package com.malinskiy.marathon.cli.config.deserialize

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.malinskiy.marathon.cli.args.FileAndroidConfiguration
import com.malinskiy.marathon.cli.args.FileIOSConfiguration
import com.malinskiy.marathon.cli.args.FileVendorConfiguration
import com.malinskiy.marathon.exceptions.ConfigurationException

const val TYPE_ANDROID = "Android"
const val TYPE_IOS = "iOS"

class FileVendorConfigurationDeserializer : StdDeserializer<FileVendorConfiguration>(FileVendorConfiguration::class.java) {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): FileVendorConfiguration {
        val codec = p?.codec as ObjectMapper
        val node: JsonNode = codec.readTree(p) ?: throw ConfigurationException("Missing vendor configuration")
        val type = node.get("type").asText()

        return when (type) {
            TYPE_IOS -> {
                (node as ObjectNode).remove("type")
                codec.treeToValue<FileIOSConfiguration>(node) ?: throw ConfigurationException("Missing vendor configuration")
            }
            TYPE_ANDROID -> {
                (node as ObjectNode).remove("type")
                codec.treeToValue<FileAndroidConfiguration>(node) ?: throw ConfigurationException("Missing vendor configuration")
            }
            else -> throw ConfigurationException(
                "Unrecognized vendor type $type. " +
                        "Valid options are $TYPE_ANDROID and $TYPE_IOS"
            )
        }
    }
}
