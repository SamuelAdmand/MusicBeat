package com.music.bitchord.feature.localmusic.coil

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Size
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import coil3.toAndroidUri
import com.kyant.taglib.TagLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Coil 3 Fetcher that extracts genuine track-level embedded artwork directly
 * from local audio files via native TagLib / MediaMetadataRetriever, falling back
 * to MediaStore album art or directory cover sidecars.
 *
 * This prevents tracks sharing generic album names (e.g. "Music" or "Unknown")
 * from colliding under a single MediaStore album_id and showing the wrong album art.
 */
class LocalAudioArtworkFetcher(
    private val context: Context,
    private val uri: Uri,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Try extracting track-level embedded artwork via native TagLib or MediaMetadataRetriever
            val embeddedBitmap = extractEmbeddedCover(context, uri)
            if (embeddedBitmap != null) {
                return@withContext ImageFetchResult(
                    image = embeddedBitmap.asImage(),
                    isSampled = false,
                    dataSource = DataSource.DISK,
                )
            }

            val cleanUri = if (uri.scheme == "content") uri.buildUpon().clearQuery().build() else uri

            // 2. On Android 10+ (API 29+), attempt ContentResolver.loadThumbnail for the track
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cleanUri.scheme == "content") {
                val thumb = runCatching {
                    context.contentResolver.loadThumbnail(cleanUri, Size(512, 512), null)
                }.getOrNull()
                if (thumb != null) {
                    return@withContext ImageFetchResult(
                        image = thumb.asImage(),
                        isSampled = false,
                        dataSource = DataSource.DISK,
                    )
                }
            }

            // 3. Fallback to albumart/<albumId> if albumId query param is provided or if URI is albumart
            val albumId = uri.getQueryParameter("albumId")?.toLongOrNull()
                ?: if (uri.path?.contains("/audio/albumart") == true) {
                    uri.lastPathSegment?.toLongOrNull()
                } else null
            if (albumId != null && albumId > 0) {
                // Try Android 10+ album thumbnail first
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val albumUri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)
                    val albumThumb = runCatching {
                        context.contentResolver.loadThumbnail(albumUri, Size(512, 512), null)
                    }.getOrNull()
                    if (albumThumb != null) {
                        return@withContext ImageFetchResult(
                            image = albumThumb.asImage(),
                            isSampled = false,
                            dataSource = DataSource.DISK,
                        )
                    }
                }

                // Try legacy albumart ContentProvider URI
                val albumArtUri = Uri.parse("content://media/external/audio/albumart/$albumId")
                val albumBitmap = runCatching {
                    context.contentResolver.openInputStream(albumArtUri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.getOrNull()
                if (albumBitmap != null) {
                    return@withContext ImageFetchResult(
                        image = albumBitmap.asImage(),
                        isSampled = false,
                        dataSource = DataSource.DISK,
                    )
                }
            }

            // 4. Fallback to directory cover image sidecars (cover.jpg, folder.jpg, etc.)
            val folderBitmap = findFolderCover(context, uri)
            if (folderBitmap != null) {
                return@withContext ImageFetchResult(
                    image = folderBitmap.asImage(),
                    isSampled = false,
                    dataSource = DataSource.DISK,
                )
            }

            null
        }.getOrNull()
    }

    private fun extractEmbeddedCover(context: Context, uri: Uri): Bitmap? {
        // First try native TagLib (fast C++ tag parser)
        val pfd = openFileDescriptor(context, uri)
        if (pfd != null) {
            val tagLibBitmap = pfd.use { descriptor ->
                runCatching {
                    val coverPicture = TagLib.getFrontCover(descriptor.dup().detachFd())
                    val bytes = coverPicture?.data ?: return@use null
                    if (bytes.isEmpty()) return@use null
                    decodeCoverBitmap(bytes)
                }.getOrNull()
            }
            if (tagLibBitmap != null) return tagLibBitmap
        }

        // Second fallback: MediaMetadataRetriever
        return runCatching {
            val mmr = MediaMetadataRetriever()
            val cleanUri = if (uri.scheme == "content") uri.buildUpon().clearQuery().build() else uri
            if (cleanUri.scheme == "file") {
                mmr.setDataSource(cleanUri.path)
            } else {
                mmr.setDataSource(context, cleanUri)
            }
            val picBytes = mmr.embeddedPicture
            mmr.release()
            if (picBytes != null && picBytes.isNotEmpty()) {
                decodeCoverBitmap(picBytes)
            } else null
        }.getOrNull()
    }

    private fun decodeCoverBitmap(bytes: ByteArray, maxDim: Int = 1200): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
        val width = boundsOptions.outWidth
        val height = boundsOptions.outHeight
        if (width <= 0 || height <= 0) return null

        var sampleSize = 1
        while (width / sampleSize > maxDim * 1.5 || height / sampleSize > maxDim * 1.5) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
    }

    private fun openFileDescriptor(context: Context, uri: Uri): ParcelFileDescriptor? {
        val cleanUri = if (uri.scheme == "content") uri.buildUpon().clearQuery().build() else uri
        val pfd = runCatching {
            if (cleanUri.scheme == "file") {
                val file = File(cleanUri.path ?: return null)
                if (file.exists() && file.canRead()) {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                } else null
            } else {
                context.contentResolver.openFileDescriptor(cleanUri, "r")
            }
        }.getOrNull()
        if (pfd != null) return pfd

        val filePath = resolveFilePath(context, uri)
        if (!filePath.isNullOrBlank()) {
            val file = File(filePath)
            if (file.exists() && file.canRead()) {
                return runCatching {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                }.getOrNull()
            }
        }
        return null
    }

    private fun findFolderCover(context: Context, uri: Uri): Bitmap? {
        val filePath = resolveFilePath(context, uri) ?: return null
        val parent = File(filePath).parentFile ?: return null
        if (!parent.exists() || !parent.isDirectory) return null

        val candidates = listOf(
            "cover.jpg", "cover.png", "cover.jpeg",
            "folder.jpg", "folder.png", "folder.jpeg",
            "front.jpg", "front.png", "front.jpeg",
            "albumart.jpg", "albumart.png",
        )
        for (name in candidates) {
            val file = File(parent, name)
            if (file.exists() && file.isFile && file.canRead()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) return bitmap
            }
        }
        return null
    }

    private fun resolveFilePath(context: Context, uri: Uri): String? = runCatching {
        if (uri.scheme == "file") return uri.path
        if (uri.scheme != "content") return null
        val cleanUri = uri.buildUpon().clearQuery().build()
        val proj = arrayOf(MediaStore.Audio.Media.DATA)
        context.contentResolver.query(cleanUri, proj, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                if (idx != -1) cursor.getString(idx) else null
            } else null
        }
    }.getOrNull()

    companion object {
        fun isLocalAudioUri(uri: Uri): Boolean {
            val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return false
            if (scheme == "content") {
                val authority = uri.authority?.lowercase(Locale.ROOT) ?: ""
                val path = uri.path?.lowercase(Locale.ROOT) ?: ""
                return (authority == "media" || authority.endsWith(".media") || authority == "com.android.providers.media.documents") &&
                    (path.contains("/audio/media") || path.contains("/audio/"))
            }
            if (scheme == "file") {
                val path = uri.path?.lowercase(Locale.ROOT) ?: ""
                return path.endsWith(".mp3") || path.endsWith(".flac") ||
                    path.endsWith(".m4a") || path.endsWith(".ogg") ||
                    path.endsWith(".opus") || path.endsWith(".wav") ||
                    path.endsWith(".aac") || path.endsWith(".webm") ||
                    path.endsWith(".3gp")
            }
            return false
        }
    }

    class CoilUriFactory(private val context: Context) : Fetcher.Factory<coil3.Uri> {
        override fun create(
            data: coil3.Uri,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? {
            val androidUri = data.toAndroidUri()
            if (isLocalAudioUri(androidUri)) {
                return LocalAudioArtworkFetcher(context, androidUri, options)
            }
            return null
        }
    }

    class AndroidUriFactory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? {
            if (isLocalAudioUri(data)) {
                return LocalAudioArtworkFetcher(context, data, options)
            }
            return null
        }
    }
}
