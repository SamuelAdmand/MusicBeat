package com.music.bitchord.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future
import java.io.File

/**
 * Media3 [BitmapLoader] backed by Coil 3.
 *
 * Automatically resolves:
 * 1. Local audio files with embedded ID3/MP4/FLAC covers (via LocalAudioArtworkFetcher).
 * 2. MediaStore album art and directory cover sidecars.
 * 3. Remote streaming tracks from YouTube Music / JioSaavn via OkHttp and Coil's disk cache.
 *
 * Forces software bitmaps ([allowHardware] = false) so the resulting [Bitmap] can safely
 * cross Binder IPC boundaries into Android SystemUI for media notifications and lock screen controls.
 */
@UnstableApi
class CoilBitmapLoader(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : BitmapLoader {

    override fun supportsMimeType(mimeType: String): Boolean = true

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        return scope.future(Dispatchers.IO) {
            BitmapFactory.decodeByteArray(data, 0, data.size)
                ?: throw IllegalArgumentException("Could not decode bitmap from byte array")
        }
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        return scope.future(Dispatchers.IO) {
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(720, 720)
                .allowHardware(false)
                .build()
            val result = SingletonImageLoader.get(context).execute(request)
            val bitmap = (result as? SuccessResult)?.image?.toBitmap()
            bitmap ?: throw IllegalArgumentException("Could not load bitmap from uri: $uri")
        }
    }

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
        val artworkData = metadata.artworkData
        if (artworkData != null) {
            return decodeBitmap(artworkData)
        }
        val artworkUri = metadata.artworkUri
        if (artworkUri != null) {
            return loadBitmap(artworkUri)
        }
        val extras = metadata.extras
        val localUriStr = extras?.getString(EXTRA_LOCAL_URI)
        if (!localUriStr.isNullOrBlank()) {
            return loadBitmap(Uri.parse(localUriStr))
        }
        val localPath = extras?.getString(EXTRA_LOCAL_PATH)
        if (!localPath.isNullOrBlank()) {
            return loadBitmap(Uri.fromFile(File(localPath)))
        }
        return null
    }
}
