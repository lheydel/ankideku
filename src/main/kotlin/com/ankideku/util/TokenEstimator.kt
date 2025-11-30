package com.ankideku.util

import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.EncodingRegistry
import com.knuddels.jtokkit.api.EncodingType

/**
 * Token estimator using JTokkit (tiktoken port for JVM)
 * Uses cl100k_base encoding (GPT-4, Claude compatible)
 */
class TokenEstimator {
    private val registry: EncodingRegistry = Encodings.newDefaultEncodingRegistry()
    private val encoding = registry.getEncoding(EncodingType.CL100K_BASE)

    /**
     * Estimate token count for a string
     */
    fun estimate(text: String): Int {
        return encoding.countTokens(text)
    }

    /**
     * Estimate total tokens for a list of strings
     */
    fun estimate(texts: List<String>): Int {
        return texts.sumOf { estimate(it) }
    }
}
