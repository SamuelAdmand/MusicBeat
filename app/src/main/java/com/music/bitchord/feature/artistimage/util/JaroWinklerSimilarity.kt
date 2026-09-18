package com.music.bitchord.feature.artistimage.util

import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

/**
 * Utility for string normalization and Jaro-Winkler similarity calculation,
 * mirroring the matching algorithm in BoomingMusic to score candidate artist names.
 */
object JaroWinklerSimilarity {

    private const val DEFAULT_PREFIX_SCALE = 0.1f
    private const val MAX_PREFIX_LENGTH = 4

    /**
     * Normalizes a string by stripping diacritics, lowering case, and collapsing whitespace.
     */
    fun normalize(input: String): String {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
    }

    /**
     * Calculates Jaro-Winkler similarity between two strings, returning a value between 0.0 and 1.0.
     */
    fun similarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1f
        if (s1.isEmpty() || s2.isEmpty()) return 0f

        val matchDistance = max(s1.length, s2.length) / 2 - 1

        val s1Matches = BooleanArray(s1.length)
        val s2Matches = BooleanArray(s2.length)

        var matches = 0
        for (i in s1.indices) {
            val start = max(0, i - matchDistance)
            val end = min(i + matchDistance + 1, s2.length)

            for (j in start until end) {
                if (s2Matches[j]) continue
                if (s1[i] != s2[j]) continue
                s1Matches[i] = true
                s2Matches[j] = true
                matches++
                break
            }
        }

        if (matches == 0) return 0f

        var transpositions = 0
        var k = 0
        for (i in s1.indices) {
            if (!s1Matches[i]) continue
            while (!s2Matches[k]) k++
            if (s1[i] != s2[k]) transpositions++
            k++
        }

        val halfTranspositions = transpositions / 2f
        val jaro = (matches.toFloat() / s1.length +
                matches.toFloat() / s2.length +
                (matches - halfTranspositions) / matches) / 3f

        // Jaro-Winkler prefix scaling
        var prefixLength = 0
        val maxPrefix = min(MAX_PREFIX_LENGTH, min(s1.length, s2.length))
        for (i in 0 until maxPrefix) {
            if (s1[i] == s2[i]) prefixLength++ else break
        }

        return jaro + prefixLength * DEFAULT_PREFIX_SCALE * (1f - jaro)
    }
}
