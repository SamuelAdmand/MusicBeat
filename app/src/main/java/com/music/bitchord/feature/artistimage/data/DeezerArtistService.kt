package com.music.bitchord.feature.artistimage.data

import com.music.bitchord.data.Http
import com.music.bitchord.feature.artistimage.util.JaroWinklerSimilarity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Service to search and resolve high-quality artist portrait photos from Deezer's public API,
 * as implemented in BoomingMusic.
 */
object DeezerArtistService {

    private const val SIMILARITY_THRESHOLD = 0.82f
    private val urlCache = ConcurrentHashMap<String, String>()
    private val negativeCache = ConcurrentHashMap.newKeySet<String>()

    /**
     * Resolves the official artist image URL from Deezer for the given [artistName].
     *
     * @param artistName The name of the artist.
     * @param isLarge Whether to return a high-res (500x500/1000x1000) or medium (250x250) image.
     * @return The remote image URL, or null if no matching artist photo was found.
     */
    suspend fun getArtistImageUrl(artistName: String, isLarge: Boolean = true): String? {
        val trimmed = artistName.trim()
        if (trimmed.isBlank() || isUnknownArtist(trimmed)) return null

        val cacheKey = "${trimmed.lowercase()}#$isLarge"
        if (negativeCache.contains(cacheKey)) return null
        urlCache[cacheKey]?.let { return it }

        val resolved = withContext(Dispatchers.IO) {
            fetchFromDeezer(trimmed, isLarge)
                ?: tryLeadArtist(trimmed, isLarge)
        }

        if (resolved != null) {
            urlCache[cacheKey] = resolved
        } else {
            negativeCache.add(cacheKey)
        }
        return resolved
    }

    private fun isUnknownArtist(name: String): Boolean {
        val lower = name.lowercase()
        return lower == "<unknown>" || lower == "unknown" || lower == "unknown artist" || lower == "various artists"
    }

    private fun tryLeadArtist(compositeName: String, isLarge: Boolean): String? {
        val lead = com.music.bitchord.feature.artistimage.util.ArtistSplitter.split(compositeName).firstOrNull()
        if (!lead.isNullOrBlank() && !lead.equals(compositeName, ignoreCase = true)) {
            return fetchFromDeezer(lead, isLarge)
        }
        return null
    }

    private fun fetchFromDeezer(query: String, isLarge: Boolean): String? {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.deezer.com/search/artist?q=$encoded&limit=5"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "BitChord/1.0 (Android)")
                .build()

            Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                parseBestImageUrl(body, query, isLarge)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseBestImageUrl(jsonString: String, requestedName: String, isLarge: Boolean): String? {
        val json = JSONObject(jsonString)
        val dataArray = json.optJSONArray("data") ?: return null
        if (dataArray.length() == 0) return null

        val normRequested = JaroWinklerSimilarity.normalize(requestedName)
        var bestScore = 0f
        var bestImageUrl: String? = null

        for (i in 0 until dataArray.length()) {
            val item = dataArray.getJSONObject(i)
            val candidateName = item.optString("name", "")
            val normCandidate = JaroWinklerSimilarity.normalize(candidateName)

            val score = if (normCandidate == normRequested) {
                1.0f
            } else {
                JaroWinklerSimilarity.similarity(normCandidate, normRequested)
            }

            if (score > bestScore) {
                val tentative = if (isLarge) {
                    item.optString("picture_xl").takeIf { it.isNotBlank() }
                        ?: item.optString("picture_big").takeIf { it.isNotBlank() }
                        ?: item.optString("picture_medium")
                } else {
                    item.optString("picture_medium").takeIf { it.isNotBlank() }
                        ?: item.optString("picture_big")
                }

                if (!tentative.isNullOrBlank() && !tentative.contains("/images/artist//")) {
                    bestScore = score
                    bestImageUrl = tentative
                }
            }
        }

        return if (bestScore >= SIMILARITY_THRESHOLD) bestImageUrl else null
    }
}
