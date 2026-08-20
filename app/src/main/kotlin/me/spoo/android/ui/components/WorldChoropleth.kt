package me.spoo.android.ui.components

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

/**
 * Country choropleth over clicks-by-country, hand-drawn like the rest of
 * the charts. Geometry: Simple World Map (Fritz Lekschas, CC BY-SA 3.0),
 * bundled as an asset and parsed once. Values are keyed by ISO-3166
 * alpha-2 codes, exactly as the stats API returns them.
 */
@Composable
fun WorldChoropleth(
    countries: Map<String, Int>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val map by produceState<WorldMap?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { WorldMapCache.load(context) }
    }

    val sea = MaterialTheme.colorScheme.surfaceContainerHigh
    val hot = MaterialTheme.colorScheme.primary
    val cold = MaterialTheme.colorScheme.primaryContainer
    val max = countries.values.maxOrNull()?.coerceAtLeast(1) ?: 1

    Canvas(modifier.aspectRatio(WorldMapCache.ASPECT)) {
        val world = map ?: return@Canvas
        val s = size.width / world.width
        scale(s, s, pivot = Offset.Zero) {
            translate(-world.minX, -world.minY) {
                world.countries.forEach { country ->
                    val clicks = countries[country.iso] ?: 0
                    val fill = if (clicks == 0) {
                        sea
                    } else {
                        lerp(cold, hot, clicks / max.toFloat())
                    }
                    drawPath(country.path, fill)
                }
            }
        }
    }
}

data class CountryPath(val iso: String, val path: Path)

data class WorldMap(
    val minX: Float,
    val minY: Float,
    val width: Float,
    val height: Float,
    val countries: List<CountryPath>,
)

object WorldMapCache {
    /** viewBox of the bundled SVG. */
    const val ASPECT = 784.077f / 458.627f

    @Volatile
    private var cached: WorldMap? = null

    fun load(context: Context): WorldMap {
        cached?.let { return it }
        val countries = mutableListOf<CountryPath>()
        var viewBox = floatArrayOf(0f, 0f, 784.077f, 458.627f)

        context.assets.open("world-map.svg").use { stream ->
            val parser = android.util.Xml.newPullParser()
            parser.setInput(stream, "UTF-8")
            // Multi-polygon countries are <g id="xx"> groups whose inner
            // paths carry no id of their own.
            var groupIso: String? = null
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "svg" -> parser.getAttributeValue(null, "viewBox")
                            ?.split(" ")
                            ?.mapNotNull { it.toFloatOrNull() }
                            ?.takeIf { it.size == 4 }
                            ?.let { viewBox = it.toFloatArray() }

                        "g" -> parser.getAttributeValue(null, "id")
                            ?.takeIf { it.length == 2 }
                            ?.let { groupIso = it }

                        "path" -> {
                            val own = parser.getAttributeValue(null, "id")?.takeIf { it.length == 2 }
                            val iso = own ?: groupIso
                            val d = parser.getAttributeValue(null, "d")
                            if (iso != null && d != null) {
                                runCatching {
                                    countries += CountryPath(
                                        iso = iso.lowercase(),
                                        path = PathParser().parsePathString(d).toPath(),
                                    )
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> if (parser.name == "g") groupIso = null
                }
                parser.next()
            }
        }

        return WorldMap(
            minX = viewBox[0],
            minY = viewBox[1],
            width = viewBox[2],
            height = viewBox[3],
            countries = countries,
        ).also { cached = it }
    }
}
