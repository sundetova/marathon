package com.malinskiy.marathon.cli.config.deserialize

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.malinskiy.marathon.cli.config.time.InstantTimeProvider
import com.malinskiy.marathon.exceptions.ConfigurationException
import com.malinskiy.marathon.execution.strategy.impl.flakiness.ProbabilityBasedFlakinessStrategy
import java.time.Duration
import java.time.Instant

class ProbabilityBasedFlakinessStrategyDeserializer(private val instantTimeProvider: InstantTimeProvider) :
    StdDeserializer<ProbabilityBasedFlakinessStrategy>(ProbabilityBasedFlakinessStrategy::class.java) {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): ProbabilityBasedFlakinessStrategy {
        val codec = p?.codec as ObjectMapper
        val node: JsonNode = codec.readTree(p) ?: throw ConfigurationException("Invalid sorting strategy")

        val minSuccessRate = node.findValue("minSuccessRate")?.asDouble()
            ?: throw ConfigurationException("Missing minimum success rate value")
        val maxCount = node.findValue("maxCount")?.asInt()
            ?: throw ConfigurationException("Missing maximum count value")

        val timeLimitValue = node.findValue("timeLimit")
            ?: throw ConfigurationException("Missing time limit value")
        val instant = codec.treeToValueOrNull(timeLimitValue, Instant::class.java)
            ?: codec.treeToValueOrNull(timeLimitValue, Duration::class.java)?.addToInstant(instantTimeProvider.referenceTime())
            ?: throw ConfigurationException("bbb")

        return ProbabilityBasedFlakinessStrategy(
            minSuccessRate = minSuccessRate,
            maxCount = maxCount,
            timeLimit = instant
        )
    }
}

private fun Duration.addToInstant(instant: Instant): Instant = instant.plus(this)
private fun <T> ObjectMapper.treeToValueOrNull(node: TreeNode, clazz: Class<T>): T? {
    val result: T
    try {
        result = treeToValue(node, clazz)
    } catch (e: InvalidFormatException) {
        return null
    }
    return result
}
