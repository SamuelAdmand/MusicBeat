package com.music.bitchord.feature.artistimage.coil

import coil3.key.Keyer
import coil3.request.Options
import com.music.bitchord.feature.artistimage.model.ArtistImage

/**
 * Generates cache keys for [ArtistImage] requests so Coil caches resolved artist portraits.
 */
class ArtistImageKeyer : Keyer<ArtistImage> {
    override fun key(data: ArtistImage, options: Options): String {
        val normalizedName = data.name.trim().lowercase()
        return "artist::name=$normalizedName|large=${data.isLarge}|fallback=${data.fallbackUrl.orEmpty()}"
    }
}
