package me.spoo.android.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.spoo.android.data.StatsDim
import me.spoo.android.ui.components.brandDomain
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Favicon bitmaps for the chart renderers. The renderers are synchronous
 * (they run inside composition), so icons are prefetched in the suspend
 * path (provideGlance / config save / preview fetch) into a small disk
 * cache and read back synchronously; a miss falls back to a monogram.
 */
object WidgetIconCache {
    private val memory = ConcurrentHashMap<String, Bitmap>()

    // Fluent has no model for a handful of multi-codepoint emoji; remember
    // the misses so a widget doesn't retry the asset on every render.
    private val missingEmoji = ConcurrentHashMap.newKeySet<Int>()

    /** The favicon host behind a dimension value, null when none applies. */
    fun hostFor(
        dim: StatsDim,
        label: String,
    ): String? =
        when (dim) {
            StatsDim.Browser, StatsDim.Os -> brandDomain(label)
            StatsDim.Referrer -> label.takeIf { it.contains('.') }
            StatsDim.Country -> null // flags are emoji, no fetch needed
            StatsDim.Device, StatsDim.UtmSource -> null // no favicon identity
        }

    fun get(
        context: Context,
        host: String,
    ): Bitmap? {
        memory[host]?.let { return it }
        val file = file(context, host)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.path)?.also { memory[host] = it }
    }

    suspend fun prefetch(
        context: Context,
        dim: StatsDim,
        labels: List<String>,
    ) = withContext(Dispatchers.IO) {
        labels.mapNotNull { hostFor(dim, it) }.distinct().forEach { host ->
            if (memory.containsKey(host) || file(context, host).exists()) return@forEach
            runCatching {
                val connection =
                    URL(
                        "https://t2.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON" +
                            "&fallback_opts=TYPE,SIZE&url=https://$host&size=64",
                    ).openConnection() as HttpURLConnection
                connection.connectTimeout = 3_000
                connection.readTimeout = 3_000
                connection.inputStream.use { input ->
                    val file = file(context, host)
                    file.parentFile?.mkdirs()
                    val tmp = File(file.path + ".tmp")
                    tmp.outputStream().use(input::copyTo)
                    tmp.renameTo(file)
                }
            }
        }
    }

    private fun file(
        context: Context,
        host: String,
    ) = File(context.cacheDir, "widget-icons/${host.replace('/', '_')}.png")

    /**
     * Bundled Fluent artwork for one emoji codepoint, null when Fluent has
     * no single-codepoint model for it. Assets are tiny webps; decoding
     * once per process is fine for label-sized use.
     */
    fun emoji(
        context: Context,
        codePoint: Int,
    ): Bitmap? {
        val key = "emoji/${codePoint.toString(16)}"
        memory[key]?.let { return it }
        if (codePoint in missingEmoji) return null
        val bitmap =
            runCatching {
                context.assets.open("$key.webp").use(BitmapFactory::decodeStream)
            }.getOrNull()
        if (bitmap == null) missingEmoji += codePoint else memory[key] = bitmap
        return bitmap
    }
}
