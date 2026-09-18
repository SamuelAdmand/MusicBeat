package com.music.bitchord.feature.artistimage.coil

import coil3.ImageLoader
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.request.Options
import com.music.bitchord.feature.artistimage.data.DeezerArtistService
import com.music.bitchord.feature.artistimage.model.ArtistImage

/**
 * Coil 3 Fetcher for [ArtistImage] that fetches the artist's portrait photo from Deezer,
 * falling back to the track's album art if Deezer has no match or if offline.
 */
class ArtistImageFetcher(
    private val loader: ImageLoader,
    private val options: Options,
    private val image: ArtistImage,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val deezerUrl = DeezerArtistService.getArtistImageUrl(image.name, image.isLarge)
        val target = deezerUrl ?: image.fallbackUrl

        if (!target.isNullOrBlank()) {
            val mapped = loader.components.map(target, options)
            val output = loader.components.newFetcher(mapped, options, loader)
            val fetcher = output?.first
            if (fetcher != null) {
                return fetcher.fetch()
            }
        }
        return null
    }

    class Factory : Fetcher.Factory<ArtistImage> {
        override fun create(
            data: ArtistImage,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher {
            return ArtistImageFetcher(imageLoader, options, data)
        }
    }
}
