package com.music.bitchord.feature.lyrics.engine

import android.util.Base64
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.AsyncFunctionBinding
import com.dokar.quickjs.binding.FunctionBinding
import com.dokar.quickjs.binding.define
import com.music.bitchord.data.Http
import com.music.bitchord.data.lyrics.LyricsLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Sandboxed QuickJS VM executor for dynamic lyrics extensions.
 */
object LyricsExtensionExecutor {

    private const val TAG = "LyricsExtension"
    private const val EXECUTION_TIMEOUT_MS = 15_000L
    private const val SEARCH_TIMEOUT_MS = 30_000L

    private val engineMap = ConcurrentHashMap<String, QuickJs>()
    private val engineLocks = ConcurrentHashMap<String, Mutex>()

    private val httpClient by lazy {
        Http.client.newBuilder()
            .callTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    private fun getLock(extensionId: String): Mutex =
        engineLocks.computeIfAbsent(extensionId) { Mutex() }

    /**
     * Executes the `getLyrics` export of the specified extension.
     *
     * @param extensionId Identifier of the extension
     * @param scriptFile Local JavaScript file of the extension
     * @param title Song title
     * @param artist Song artist
     * @param durationMs Song duration in ms
     * @param album Album name (optional)
     * @param videoId Video ID (optional)
     * @return Raw lyrics string (LRC, TTML, plain text) or null
     */
    suspend fun getLyrics(
        extensionId: String,
        scriptFile: File,
        title: String,
        artist: String,
        durationMs: Long,
        album: String? = null,
        videoId: String? = null,
    ): String? = withContext(Dispatchers.Default) {
        val lock = getLock(extensionId)
        lock.withLock {
            withTimeoutOrNull(EXECUTION_TIMEOUT_MS) {
                try {
                    val qjs = getOrInitEngine(extensionId, scriptFile) ?: return@withTimeoutOrNull null
                    val trackJson = JSONObject().apply {
                        put("title", title)
                        put("artist", artist)
                        put("durationMs", durationMs)
                        put("album", album ?: "")
                        put("videoId", videoId ?: "")
                    }.toString()

                    LyricsLog.i(extensionId, "getLyrics query: title='$title', artist='$artist'")

                    qjs.evaluate<String>(
                        """
                        var __lyrics_result = undefined;
                        (async function() {
                            try {
                                if (module.exports && typeof module.exports.getLyrics === 'function') {
                                    var r = await module.exports.getLyrics($trackJson);
                                    __lyrics_result = (r !== undefined && r !== null) ? String(r) : 'null';
                                } else {
                                    console.warn('[LyricsExt] getLyrics not found on module.exports');
                                    __lyrics_result = 'null';
                                }
                            } catch(e) {
                                console.error('[LyricsExt Error] ' + (e && e.message ? e.message : String(e)));
                                __lyrics_result = 'null';
                            }
                        })();
                        """.trimIndent()
                    )

                    val result = qjs.evaluate<String>("__lyrics_result || 'null'")
                    LyricsLog.i(extensionId, "getLyrics result: ${result.take(100)}")
                    if (result == "null" || result.isBlank()) null else result
                } catch (e: Throwable) {
                    LyricsLog.w(extensionId, "Execution failed: ${e.message}")
                    null
                }
            }
        }
    }

    /**
     * Executes the `searchLyrics` export of the specified extension (for lyrics editor dialog).
     */
    suspend fun searchLyrics(
        extensionId: String,
        scriptFile: File,
        title: String,
        artist: String,
        album: String? = null,
    ): String? = withContext(Dispatchers.Default) {
        val lock = getLock(extensionId)
        lock.withLock {
            withTimeoutOrNull(SEARCH_TIMEOUT_MS) {
                try {
                    val qjs = getOrInitEngine(extensionId, scriptFile) ?: return@withTimeoutOrNull null
                    val queryJson = JSONObject().apply {
                        put("title", title)
                        put("artist", artist)
                        put("album", album ?: "")
                    }.toString()

                    LyricsLog.i(extensionId, "searchLyrics query: title='$title', artist='$artist'")

                    qjs.evaluate<String>(
                        """
                        var __search_result = undefined;
                        (async function() {
                            try {
                                if (module.exports && typeof module.exports.searchLyrics === 'function') {
                                    var r = await module.exports.searchLyrics($queryJson);
                                    __search_result = JSON.stringify(r);
                                } else {
                                    console.warn('[LyricsExt] searchLyrics not found on module.exports');
                                    __search_result = '[]';
                                }
                            } catch(e) {
                                console.error('[LyricsExt search error] ' + (e && e.message ? e.message : String(e)));
                                __search_result = '[]';
                            }
                        })();
                        """.trimIndent()
                    )

                    val result = qjs.evaluate<String>("__search_result || '[]'")
                    LyricsLog.i(extensionId, "searchLyrics result: ${result.take(200)}")
                    if (result == "[]" || result.isBlank()) null else result
                } catch (e: Throwable) {
                    LyricsLog.e(extensionId, "searchLyrics exception: ${e.message}")
                    null
                }
            }
        }
    }

    /**
     * Unloads and closes an engine when an extension is updated or disabled.
     */
    fun unload(extensionId: String) {
        val lock = getLock(extensionId)
        val qjs = engineMap.remove(extensionId)
        qjs?.let {
            runCatching { it.close() }
        }
    }

    /**
     * Unloads and closes all engines.
     */
    fun unloadAll() {
        engineMap.keys.toList().forEach { unload(it) }
    }

    private suspend fun getOrInitEngine(extensionId: String, scriptFile: File): QuickJs? {
        engineMap[extensionId]?.let { return it }
        if (!scriptFile.exists() || !scriptFile.canRead()) {
            LyricsLog.w(extensionId, "Script file not found: ${scriptFile.absolutePath}")
            return null
        }

        val code = runCatching { scriptFile.readText() }.getOrNull() ?: return null
        val qjs = QuickJs.create(Dispatchers.Default)
        qjs.maxStackSize = 512 * 1024L

        try {
            bindConsole(qjs, extensionId)
            bindHttp(qjs)
            bindBase64(qjs)

            // Setup CommonJS module environment
            qjs.evaluate<String>(
                """
                var module = { exports: {} };
                var exports = module.exports;
                """.trimIndent()
            )

            qjs.evaluate<String>(code)
            engineMap[extensionId] = qjs
            return qjs
        } catch (e: Throwable) {
            LyricsLog.e(extensionId, "Failed to initialize extension JS: ${e.message}")
            runCatching { qjs.close() }
            return null
        }
    }

    private fun bindConsole(qjs: QuickJs, extensionId: String) {
        qjs.define("console") {
            function("log", object : FunctionBinding<Unit> {
                override fun invoke(args: Array<Any?>) {
                    LyricsLog.i(extensionId, args.joinToString(" ") { it?.toString() ?: "null" })
                }
            })
            function("error", object : FunctionBinding<Unit> {
                override fun invoke(args: Array<Any?>) {
                    LyricsLog.e(extensionId, args.joinToString(" ") { it?.toString() ?: "null" })
                }
            })
            function("warn", object : FunctionBinding<Unit> {
                override fun invoke(args: Array<Any?>) {
                    LyricsLog.w(extensionId, args.joinToString(" ") { it?.toString() ?: "null" })
                }
            })
            function("info", object : FunctionBinding<Unit> {
                override fun invoke(args: Array<Any?>) {
                    LyricsLog.i(extensionId, args.joinToString(" ") { it?.toString() ?: "null" })
                }
            })
        }
    }

    private suspend fun bindBase64(qjs: QuickJs) {
        qjs.define("__base64") {
            function("atob", object : FunctionBinding<String> {
                override fun invoke(args: Array<Any?>): String {
                    val str = args.firstOrNull()?.toString() ?: return ""
                    return runCatching {
                        String(Base64.decode(str, Base64.DEFAULT), Charsets.UTF_8)
                    }.getOrDefault("")
                }
            })
            function("btoa", object : FunctionBinding<String> {
                override fun invoke(args: Array<Any?>): String {
                    val str = args.firstOrNull()?.toString() ?: return ""
                    return runCatching {
                        Base64.encodeToString(str.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                    }.getOrDefault("")
                }
            })
        }

        qjs.evaluate<Unit>(
            """
            var atob = function(str) { return __base64.atob(str); };
            var btoa = function(str) { return __base64.btoa(str); };
            """.trimIndent()
        )
    }

    private suspend fun bindHttp(qjs: QuickJs) {
        qjs.define("__native_lyrics") {
            asyncFunction("fetch", object : AsyncFunctionBinding<String> {
                override suspend fun invoke(args: Array<Any?>): String {
                    val rawUrl = args.getOrNull(0)?.toString() ?: throw IllegalArgumentException("fetch requires URL")
                    val method = args.getOrNull(1)?.toString() ?: "GET"
                    val headersJson = args.getOrNull(2)?.toString() ?: "{}"
                    val body = args.getOrNull(3)?.toString()

                    val (statusCode, responseBody) = executeHttp(rawUrl, method, headersJson, body)
                    val respObj = JSONObject().apply {
                        put("status", statusCode)
                        put("ok", statusCode in 200..299)
                        put("body", responseBody)
                    }
                    return respObj.toString()
                }
            })
            asyncFunction("setTimeout", object : AsyncFunctionBinding<String> {
                override suspend fun invoke(args: Array<Any?>): String {
                    val ms = args.getOrNull(1)?.toString()?.toLongOrNull() ?: 0L
                    delay(ms)
                    return "0"
                }
            })
        }

        // Polyfill standard fetch() over __native_lyrics.fetch
        qjs.evaluate<Unit>(
            """
            var fetch = async function(url, options) {
                var method = 'GET';
                var headers = '{}';
                var body = null;
                if (options) {
                    method = options.method || 'GET';
                    if (options.headers) {
                        if (typeof options.headers === 'string') {
                            headers = options.headers;
                        } else {
                            try { headers = JSON.stringify(options.headers); } catch(e) { headers = '{}'; }
                        }
                    }
                    if (options.body !== undefined && options.body !== null) {
                        body = typeof options.body === 'string' ? options.body : JSON.stringify(options.body);
                    }
                }
                var raw = JSON.parse(await __native_lyrics.fetch(url, method, headers, body));
                var respBody = raw.body;
                return {
                    ok: raw.ok,
                    status: raw.status,
                    statusText: raw.ok ? 'OK' : 'Error',
                    json: function() {
                        try { return JSON.parse(respBody); } catch(e) { throw new Error('Invalid JSON'); }
                    },
                    text: function() { return respBody; }
                };
            };

            var setTimeout = async function(fn, ms) {
                await __native_lyrics.setTimeout(null, ms || 0);
                if (typeof fn === 'function') fn();
                return 0;
            };
            var clearTimeout = function(id) {};
            """.trimIndent()
        )
    }

    private fun executeHttp(
        url: String,
        method: String,
        headersJson: String,
        body: String?,
    ): Pair<Int, String> = runCatching {
        val builder = Request.Builder().url(url)
        var hasUserAgent = false

        runCatching {
            val headersObj = JSONObject(headersJson)
            for (key in headersObj.keys()) {
                val value = headersObj.optString(key, "")
                builder.header(key, value)
                if (key.equals("user-agent", ignoreCase = true)) hasUserAgent = true
            }
        }

        if (!hasUserAgent) {
            builder.header("User-Agent", "BitChord (https://github.com/bitchord)")
        }

        if (method.equals("POST", ignoreCase = true)) {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            builder.post((body ?: "").toRequestBody(mediaType))
        } else {
            builder.get()
        }

        httpClient.newCall(builder.build()).execute().use { response ->
            val code = response.code
            val respBody = response.body?.string() ?: ""
            Pair(code, respBody)
        }
    }.getOrElse {
        LyricsLog.w(TAG, "executeHttp failed for $url: ${it.message}")
        Pair(0, "")
    }
}
