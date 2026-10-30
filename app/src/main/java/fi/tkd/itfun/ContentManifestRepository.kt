package fi.tkd.itfun

import android.content.Context
import android.util.AtomicFile
import fi.tkd.itfun.data.db.REMOTE_MANIFEST_URL
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ManifestCheck(val manifest: JSONObject?, val failed: Boolean = false)

class ContentManifestRepository internal constructor(
    private val cacheFile: File,
    private val source: String,
    private val now: () -> Long = System::currentTimeMillis,
    private val fetch: suspend () -> JSONObject?
) {
    constructor(context: Context) : this(
        File(context.applicationContext.filesDir, "content_manifest_preview.json"),
        REMOTE_MANIFEST_URL,
        fetch = {
            val connection = URL(REMOTE_MANIFEST_URL).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            } finally {
                connection.disconnect()
            }
        }
    )

    suspend fun cached(): ManifestCheck = withContext(Dispatchers.IO) {
        mutex.withLock { readCache().toResult() }
    }

    suspend fun check(force: Boolean = false): ManifestCheck = withContext(Dispatchers.IO) {
        mutex.withLock {
            val previous = readCache()
            val time = now()
            val lastAttempt = previous?.optLong("attemptedAt") ?: 0L
            if (!force && previous != null && time >= lastAttempt && time - lastAttempt < DAY_MS) {
                return@withLock previous.toResult()
            }

            // Persist attempts too, so an offline sheet does not retry on every opening.
            val attempt = previous ?: JSONObject().put("source", source)
            attempt.put("attemptedAt", time).put("failed", true)
            writeCache(attempt)
            val manifest = try {
                fetch()?.also {
                    it.getInt("version")
                    it.getJSONArray("techniques")
                    it.getJSONArray("bodyParts")
                    it.getJSONObject("terms")
                    it.getJSONObject("patternvideodetails")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                null
            }
            if (manifest != null) {
                attempt.put("manifest", manifest).put("failed", false)
                writeCache(attempt)
                ManifestCheck(manifest)
            } else {
                ManifestCheck(previous?.optJSONObject("manifest"), failed = true)
            }
        }
    }

    private fun readCache(): JSONObject? = runCatching {
        AtomicFile(cacheFile).openRead().bufferedReader().use { JSONObject(it.readText()) }
            .takeIf { it.optString("source") == source }
    }.getOrNull()

    private fun writeCache(value: JSONObject) {
        val file = AtomicFile(cacheFile)
        val output = file.startWrite()
        try {
            output.write(value.toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(output)
        } catch (exception: Exception) {
            file.failWrite(output)
            throw exception
        }
    }

    private fun JSONObject?.toResult() = ManifestCheck(
        manifest = this?.optJSONObject("manifest"),
        failed = this?.optBoolean("failed") ?: false
    )

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
        val mutex = Mutex()
    }
}
