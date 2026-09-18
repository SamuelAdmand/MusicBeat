package com.music.bitchord.ui.components

import java.util.Locale

/**
 * Utility for extracting normalized section headers (e.g. "A", "B", ..., "#")
 * from song titles, artist names, or album titles for fast scroll index bubbles.
 */
object SectionIndexer {

    fun getSectionName(text: String?): String {
        if (text.isNullOrBlank()) return "#"
        val trimmed = text.trim()
        val normalized = when {
            trimmed.startsWith("The ", ignoreCase = true) -> trimmed.substring(4).trim()
            trimmed.startsWith("A ", ignoreCase = true) -> trimmed.substring(2).trim()
            trimmed.startsWith("An ", ignoreCase = true) -> trimmed.substring(3).trim()
            else -> trimmed
        }
        val firstChar = normalized.firstOrNull() ?: return "#"
        return if (firstChar.isLetter()) {
            firstChar.uppercase(Locale.ROOT)
        } else {
            "#"
        }
    }
}
