package com.music.bitchord.feature.artistimage.util

import com.music.bitchord.data.model.Song
import com.music.bitchord.feature.artistimage.model.ArtistEntry
import java.util.Locale

/**
 * Utility to split composite artist tags into individual artist credits
 * and aggregate library tracks under each distinct artist.
 */
object ArtistSplitter {

    /**
     * Famous artists/bands whose official names contain characters that would
     * otherwise look like multi-artist delimiters (e.g., "&", "/", ",").
     */
    private val INVIOLABLE_BANDS = listOf(
        "AC/DC",
        "Simon & Garfunkel",
        "Above & Beyond",
        "Earth, Wind & Fire",
        "Crosby, Stills, Nash & Young",
        "Hall & Oates",
        "Brooks & Dunn",
        "Kool & The Gang",
        "KC & The Sunshine Band",
        "Florence + The Machine",
        "Mumford & Sons",
        "Tom Petty and the Heartbreakers",
        "Huey Lewis & The News",
        "Bob Marley & The Wailers",
        "Sly & The Family Stone",
        "Arms and Sleepers",
        "Blood, Sweat & Tears",
        "Katrina and the Waves",
        "Derek & The Dominos",
        "Ziggy Marley & The Melody Makers",
        "Captain & Tennille",
        "Peaches & Herb",
        "Ashford & Simpson",
        "Sam & Dave",
        "Joan Jett & the Blackhearts",
    )

    /**
     * Regex matching multi-artist separators:
     * - Semicolons: ;
     * - Slashes and backslashes: / or \
     * - Pipes, bullets, dots: |, •, ·
     * - Commas: ,
     * - Whitespace-surrounded ampersand: &
     * - Whitespace-surrounded plus: +
     * - Collaboration words: feat., feat, ft., ft, featuring, with, vs., vs, x
     */
    private val DELIMITER_REGEX = Regex(
        """\s*(?:[;/\\|•·]|,\s*|\s+&\s+|\s+\+\s+|\s+(?:feat\.?|ft\.?|featuring|with|vs\.?|x)\s+)\s*""",
        RegexOption.IGNORE_CASE,
    )

    private val UNWANTED_PUNCTUATION = Regex("""^[^\p{L}\p{N}]+|[^\p{L}\p{N}]+$""")

    /**
     * Splits a raw artist string into distinct individual artist names.
     *
     * Example inputs:
     * - "Pritam, Tulsi Kumar & KK" -> ["Pritam", "Tulsi Kumar", "KK"]
     * - "Pritam; Arijit Singh" -> ["Pritam", "Arijit Singh"]
     * - "Arash, Helena" -> ["Arash", "Helena"]
     * - "Arash; Helena" -> ["Arash", "Helena"]
     * - "Roop Kumar Rathod, Sayeed Quadri,..." -> ["Roop Kumar Rathod", "Sayeed Quadri"]
     * - "AC/DC" -> ["AC/DC"]
     */
    fun split(rawArtist: String?): List<String> {
        val raw = rawArtist?.trim().orEmpty()
        if (raw.isBlank()) return emptyList()

        // Check if raw matches an inviolable band exactly
        val exactMatch = INVIOLABLE_BANDS.firstOrNull { it.equals(raw, ignoreCase = true) }
        if (exactMatch != null) return listOf(exactMatch)

        // Protect known inviolable bands before splitting
        var protectedString = raw
        val protectedMap = mutableMapOf<String, String>()
        for ((index, band) in INVIOLABLE_BANDS.withIndex()) {
            if (protectedString.contains(band, ignoreCase = true)) {
                val placeholder = "__BAND_${index}__"
                // Replace case-insensitively while preserving canonical band name
                val regex = Regex(Regex.escape(band), RegexOption.IGNORE_CASE)
                protectedString = regex.replace(protectedString, placeholder)
                protectedMap[placeholder] = band
            }
        }

        // Split by delimiters
        val rawTokens = protectedString.split(DELIMITER_REGEX)

        val results = mutableListOf<String>()
        for (token in rawTokens) {
            // Restore any protected band placeholders
            var restored = token
            for ((placeholder, band) in protectedMap) {
                if (restored.contains(placeholder)) {
                    restored = restored.replace(placeholder, band)
                }
            }

            val cleaned = cleanToken(restored)
            if (cleaned.isNotBlank() && !isPureNoise(cleaned)) {
                results.add(cleaned)
            }
        }

        // Filter out unknown markers if we have real artist names
        val filtered = if (results.size > 1) {
            results.filter { !isUnknownPlaceholder(it) }
        } else {
            results
        }

        return filtered.distinctBy { it.lowercase(Locale.ROOT) }
    }

    /**
     * Aggregates a list of songs by individual artist, distributing multi-artist tracks
     * to each contributing artist. Returns a list of [ArtistEntry] sorted alphabetically.
     */
    fun groupSongsByArtist(
        songs: List<Song>,
        unknownArtistFallback: String = "Unknown Artist",
    ): List<ArtistEntry> {
        // Map from lowercase artist name to pair of (canonical display name, list of songs)
        val artistMap = LinkedHashMap<String, Pair<String, MutableList<Song>>>()

        for (song in songs) {
            val splitArtists = split(song.artist)
            val names = if (splitArtists.isEmpty()) {
                listOf(song.artist.ifBlank { unknownArtistFallback })
            } else {
                splitArtists
            }

            for (artistName in names) {
                val key = artistName.lowercase(Locale.ROOT)
                val existing = artistMap[key]
                if (existing == null) {
                    artistMap[key] = Pair(artistName, mutableListOf(song))
                } else {
                    val canonicalName = selectBestCasing(existing.first, artistName)
                    val list = existing.second
                    if (!list.any { it.videoId == song.videoId }) {
                        list.add(song)
                    }
                    artistMap[key] = Pair(canonicalName, list)
                }
            }
        }

        return artistMap.values
            .map { (name, artistSongs) ->
                ArtistEntry(
                    name = name,
                    songs = artistSongs,
                    thumbnailUrl = artistSongs.firstNotNullOfOrNull { it.thumbnailUrl },
                    key = "artist:${name.lowercase(Locale.ROOT)}",
                )
            }
            .sortedBy { it.name.lowercase(Locale.ROOT) }
    }

    /**
     * Checks whether [songArtist] contains [targetArtist] as one of its contributing credits.
     */
    fun matchesArtist(songArtist: String, targetArtist: String): Boolean {
        val target = targetArtist.trim()
        if (target.isBlank()) return false
        val splitList = split(songArtist)
        return splitList.any { it.equals(target, ignoreCase = true) }
    }

    private fun cleanToken(token: String): String {
        var s = token.trim()
        // Strip surrounding quotes
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            s = s.substring(1, s.length - 1).trim()
        }
        // Strip leading/trailing punctuation characters (like trailing commas, dots, dashes)
        s = s.replace(UNWANTED_PUNCTUATION, "").trim()
        return s
    }

    private fun isPureNoise(token: String): Boolean {
        // Checks for tokens consisting only of punctuation or dots like "..."
        return token.all { !it.isLetterOrDigit() }
    }

    private fun isUnknownPlaceholder(name: String): Boolean {
        val lower = name.lowercase(Locale.ROOT)
        return lower == "<unknown>" || lower == "unknown" || lower == "unknown artist" || lower == "various artists"
    }

    private fun selectBestCasing(existing: String, candidate: String): String {
        val existingUpper = existing.count { it.isUpperCase() }
        val candidateUpper = candidate.count { it.isUpperCase() }
        return if (candidateUpper > existingUpper) candidate else existing
    }
}
