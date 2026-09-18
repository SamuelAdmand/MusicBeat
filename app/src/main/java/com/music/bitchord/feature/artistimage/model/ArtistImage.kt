package com.music.bitchord.feature.artistimage.model

/**
 * Model representing an artist whose portrait image should be fetched and cached by Coil.
 *
 * @param name The artist's name (e.g. "Ace of Base", "A.R. Rahman").
 * @param fallbackUrl Optional local album art or fallback URI to display if Deezer has no photo.
 * @param isLarge True if requesting a high-resolution portrait (500x500 / 1000x1000), false for list thumbnails (250x250).
 */
data class ArtistImage(
    val name: String,
    val fallbackUrl: String? = null,
    val isLarge: Boolean = true,
)
