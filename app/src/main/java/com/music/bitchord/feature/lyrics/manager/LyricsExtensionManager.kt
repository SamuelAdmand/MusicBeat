package com.music.bitchord.feature.lyrics.manager

import android.content.Context
import com.music.bitchord.data.Http
import com.music.bitchord.data.lyrics.LyricsLog
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.feature.lyrics.domain.LyricsExtension
import com.music.bitchord.feature.lyrics.domain.LyricsExtensionManifest
import com.music.bitchord.feature.lyrics.domain.RemoteLyricsRegistry
import com.music.bitchord.feature.lyrics.engine.LyricsExtensionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.io.File

/**
 * Manages installed lyrics extensions, bundled assets, and remote GitHub synchronization.
 */
object LyricsExtensionManager {

    private const val TAG = "LyricsExtensionManager"
    private const val EXTENSIONS_DIR = "lyrics_extensions"
    private const val ASSETS_DIR = "lyrics_extensions"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _installedExtensions = MutableStateFlow<List<LyricsExtension>>(emptyList())
    val installedExtensions: StateFlow<List<LyricsExtension>> = _installedExtensions.asStateFlow()

    private val _dynamicSources = MutableStateFlow<List<com.music.bitchord.data.lyrics.LyricsSource>>(emptyList())
    val dynamicSources: StateFlow<List<com.music.bitchord.data.lyrics.LyricsSource>> = _dynamicSources.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private var appContext: Context? = null

    /**
     * Initializes the manager with the Application Context.
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        scope.launch {
            bootstrapBundledExtensions(context)
            loadInstalledExtensions(context)
            if (AppSettings.lyricsAutoUpdate.value) {
                syncFromRepository(context, force = false)
            }
        }
    }

    /**
     * Ensures all bundled extensions from assets exist in the internal storage
     * directory, and overwrites them when the bundled version is newer than
     * what's currently installed (e.g. after an app update).
     */
    private suspend fun bootstrapBundledExtensions(context: Context) = withContext(Dispatchers.IO) {
        val rootDir = File(context.filesDir, EXTENSIONS_DIR)
        val extDir = File(rootDir, "extensions")
        if (!extDir.exists()) extDir.mkdirs()

        runCatching {
            val assetManager = context.assets
            val hasNestedExtensions = assetManager.list("$ASSETS_DIR/extensions")?.isNotEmpty() == true
            val bundledDirs = if (hasNestedExtensions) {
                assetManager.list("$ASSETS_DIR/extensions") ?: emptyArray()
            } else {
                assetManager.list(ASSETS_DIR)?.filter { it != "registry.json" && it != "README.md" && !it.contains(".") }?.toTypedArray() ?: emptyArray()
            }

            for (extName in bundledDirs) {
                val targetDir = File(extDir, extName)
                if (!targetDir.exists()) targetDir.mkdirs()

                val manifestFile = File(targetDir, "manifest.json")
                val scriptFile = File(targetDir, "index.js")
                val assetPrefix = if (hasNestedExtensions) "$ASSETS_DIR/extensions/$extName" else "$ASSETS_DIR/$extName"

                // Read bundled manifest version to decide whether to overwrite
                val bundledVersion = runCatching {
                    assetManager.open("$assetPrefix/manifest.json").bufferedReader().use { reader ->
                        val manifest = json.decodeFromString<LyricsExtensionManifest>(reader.readText())
                        manifest.version
                    }
                }.getOrNull()

                val installedVersion = if (manifestFile.exists()) {
                    runCatching {
                        json.decodeFromString<LyricsExtensionManifest>(manifestFile.readText()).version
                    }.getOrNull()
                } else null

                val shouldOverwrite = !manifestFile.exists()
                    || !scriptFile.exists()
                    || (bundledVersion != null && (installedVersion == null || isNewerVersion(bundledVersion, installedVersion)))

                if (shouldOverwrite) {
                    runCatching {
                        assetManager.open("$assetPrefix/manifest.json").use { input ->
                            manifestFile.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                    runCatching {
                        assetManager.open("$assetPrefix/index.js").use { input ->
                            scriptFile.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                    // Clear cached engine so the new script is loaded fresh
                    LyricsExtensionExecutor.unload(extName)
                    LyricsLog.i(TAG, "Bootstrapped $extName: ${installedVersion ?: "new"} -> ${bundledVersion ?: "?"}")
                }
            }
        }.onFailure {
            LyricsLog.w(TAG, "Failed bootstrapping bundled extensions: ${it.message}")
        }
    }

    /**
     * Loads installed extensions from disk into memory.
     */
    suspend fun loadInstalledExtensions(context: Context) = withContext(Dispatchers.IO) {
        val extDir = File(context.filesDir, "$EXTENSIONS_DIR/extensions")
        if (!extDir.exists()) {
            _installedExtensions.value = emptyList()
            return@withContext
        }

        val list = mutableListOf<LyricsExtension>()
        extDir.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
            val manifestFile = File(dir, "manifest.json")
            val scriptFile = File(dir, "index.js")
            if (manifestFile.exists() && scriptFile.exists()) {
                runCatching {
                    val content = manifestFile.readText()
                    val manifest = json.decodeFromString<LyricsExtensionManifest>(content)
                    list += LyricsExtension(
                        id = manifest.id,
                        name = manifest.name,
                        version = manifest.version,
                        description = manifest.description,
                        author = manifest.author,
                        wordSynced = manifest.wordSynced,
                        defaultPriority = manifest.defaultPriority,
                        scriptFile = scriptFile,
                        isBundled = false,
                    )
                }.onFailure {
                    LyricsLog.w(TAG, "Failed to load extension in ${dir.name}: ${it.message}")
                }
            }
        }

        list.sortBy { it.defaultPriority }
        _installedExtensions.value = list
        val sources = list.map { com.music.bitchord.data.lyrics.LyricsSource.fromExtension(it) }
        _dynamicSources.value = sources
        AppSettings.refreshLyricsSources(sources)
        LyricsLog.i(TAG, "Loaded ${list.size} lyrics extensions: ${list.joinToString { it.name }}")
    }

    /**
     * Syncs extensions from the remote GitHub registry.
     */
    suspend fun syncFromRepository(
        context: Context? = null,
        force: Boolean = true,
    ): Result<Int> = withContext(Dispatchers.IO) {
        val ctx = context ?: appContext
            ?: return@withContext Result.failure(IllegalStateException("No context available"))
        if (_isSyncing.value) return@withContext Result.success(0)
        _isSyncing.value = true
        _syncMessage.value = "Checking for extension updates..."

        try {
            val repoUrl = AppSettings.lyricsExtensionRepoUrl.value.trim()
            if (repoUrl.isBlank()) {
                _syncMessage.value = "No repository URL configured"
                return@withContext Result.failure(IllegalStateException("No repository URL"))
            }

            LyricsLog.i(TAG, "Fetching registry from $repoUrl")
            val request = Request.Builder().url(repoUrl).build()
            val registryJson = Http.client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw Exception("HTTP ${resp.code} fetching registry")
                resp.body?.string() ?: throw Exception("Empty registry response")
            }

            val remoteRegistry = json.decodeFromString<RemoteLyricsRegistry>(registryJson)
            val currentMap = _installedExtensions.value.associateBy { it.id }
            val extDir = File(ctx.filesDir, "$EXTENSIONS_DIR/extensions")
            if (!extDir.exists()) extDir.mkdirs()

            // 1. Prune deleted extensions (folders on disk whose ID is no longer in the remote registry)
            val remoteIds = remoteRegistry.extensions.map { it.id }.toSet()
            extDir.listFiles()?.filter { it.isDirectory && it.name !in remoteIds }?.forEach { staleDir ->
                LyricsLog.w(TAG, "Pruning deleted extension: ${staleDir.name}")
                LyricsExtensionExecutor.unload(staleDir.name)
                staleDir.deleteRecursively()
            }

            var updatedCount = 0

            // 2. Download and update extensions
            for (remoteItem in remoteRegistry.extensions) {
                val current = currentMap[remoteItem.id]
                val needsUpdate = force || current == null || isNewerVersion(remoteItem.version, current.version)

                if (needsUpdate) {
                    _syncMessage.value = "Updating ${remoteItem.name} (${remoteItem.version})..."
                    LyricsLog.i(TAG, "Downloading update for ${remoteItem.id}: ${current?.version} -> ${remoteItem.version}")

                    val scriptUrl = remoteItem.script_url
                        ?: repoUrl.substringBeforeLast("/") + "/extensions/${remoteItem.id}/index.js"
                    val manifestUrl = remoteItem.manifest_url
                        ?: repoUrl.substringBeforeLast("/") + "/extensions/${remoteItem.id}/manifest.json"

                    val itemDir = File(extDir, remoteItem.id)
                    if (!itemDir.exists()) itemDir.mkdirs()

                    val downloadedScript = downloadString(scriptUrl)
                    val downloadedManifest = downloadString(manifestUrl)

                    if (downloadedScript != null && downloadedManifest != null) {
                        File(itemDir, "index.js").writeText(downloadedScript)
                        File(itemDir, "manifest.json").writeText(downloadedManifest)
                        LyricsExtensionExecutor.unload(remoteItem.id)
                        updatedCount++
                    }
                }
            }

            AppSettings.setLastLyricsExtensionSync(System.currentTimeMillis())
            loadInstalledExtensions(ctx)
            _syncMessage.value = if (updatedCount > 0) "$updatedCount extension(s) updated" else "All extensions up to date"
            Result.success(updatedCount)
        } catch (e: Throwable) {
            LyricsLog.e(TAG, "Sync failed: ${e.message}")
            _syncMessage.value = "Sync failed: ${e.localizedMessage}"
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    private fun downloadString(url: String): String? = runCatching {
        val req = Request.Builder().url(url).build()
        Http.client.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        }
    }.getOrNull()

    private fun isNewerVersion(remote: String, local: String): Boolean {
        if (remote == local) return false
        val rParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val lParts = local.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(rParts.size, lParts.size)) {
            val r = rParts.getOrElse(i) { 0 }
            val l = lParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    /**
     * Gets installed extension by ID.
     */
    fun getExtension(id: String): LyricsExtension? =
        _installedExtensions.value.firstOrNull { it.id.equals(id, ignoreCase = true) }
}
