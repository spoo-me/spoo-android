package me.spoo.android.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.datastore.preferences.core.Preferences
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import java.text.NumberFormat
import kotlinx.coroutines.flow.first
import me.spoo.android.AppGraph
import me.spoo.android.MainActivity
import me.spoo.android.SpooApp
import me.spoo.android.data.StatsParams

/**
 * Home-screen widget: one big number over a chart, scoped per instance by
 * [WidgetConfig] (chosen on placement, editable via long-press). Fresh data
 * when the network cooperates, the per-instance cached snapshot otherwise —
 * stale data over spinners.
 */
class SpooWidget : GlanceAppWidget() {

    // Exact size so the chart bitmap is rendered 1:1 for the slot.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val graph = SpooApp.graph
        // TokenStore, not AuthManager state: a cold widget process may render
        // before restore() lands, and the token file is the durable truth.
        val signedIn = graph.tokenStore.read() != null ||
            graph.settingsRepository.settings.first().mockData

        val config = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
            .readWidgetConfig()
        val data = if (signedIn) fetchOrCached(context, graph, id, config) else null

        provideContent {
            GlanceTheme {
                if (data == null) {
                    SignedOutContent(context)
                } else {
                    ClicksContent(context, config, data.first, data.second)
                }
            }
        }
    }

    private suspend fun fetchOrCached(
        context: Context,
        graph: AppGraph,
        id: GlanceId,
        config: WidgetConfig,
    ): Pair<Long, List<Int>> {
        val fresh = runCatching {
            val repo = graph.linksRepository
            if (repo.links.value.isEmpty()) repo.refresh()
            val params = StatsParams(days = config.rangeDays, metric = config.metric)
            val stats = config.scope
                ?.let { repo.stats(it, params) }
                ?: repo.accountStats(params)
            stats.dailyClicks.sumOf { it.toLong() } to stats.dailyClicks
        }.getOrNull()

        if (fresh != null) {
            updateAppWidgetState(context, id) {
                it[WidgetKeys.CACHED_TOTAL] = fresh.first
                it[WidgetKeys.CACHED_SERIES] = fresh.second.joinToString(",")
            }
            return fresh
        }
        val prefs: Preferences = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        return (prefs[WidgetKeys.CACHED_TOTAL] ?: 0L) to
            (prefs[WidgetKeys.CACHED_SERIES]?.split(',')?.mapNotNull(String::toIntOrNull).orEmpty())
    }

    @androidx.compose.runtime.Composable
    private fun ClicksContent(
        context: Context,
        config: WidgetConfig,
        total: Long,
        series: List<Int>,
    ) {
        val size = LocalSize.current
        val density = context.resources.displayMetrics.density
        val chartHeight = size.height * 0.68f
        val accent = GlanceTheme.colors.primary.getColor(context).toArgb()

        val openApp = actionStartActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.surface)
                .clickable(openApp),
        ) {
            if (config.style != WidgetStyle.Number && series.size >= 2) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    Image(
                        provider = ImageProvider(
                            renderChartBitmap(
                                style = config.style,
                                series = series,
                                width = (size.width.value * density).toInt().coerceAtLeast(2),
                                height = (chartHeight.value * density).toInt().coerceAtLeast(2),
                                accent = accent,
                                density = density,
                            ),
                        ),
                        contentDescription = null,
                        modifier = GlanceModifier.fillMaxWidth().height(chartHeight),
                        contentScale = ContentScale.FillBounds,
                    )
                }
            }
            Column(
                modifier = GlanceModifier
                    .padding(horizontal = 18.dp)
                    .padding(
                        vertical = if (config.style == WidgetStyle.Number) 12.dp else 16.dp,
                    )
                    .let { if (config.style == WidgetStyle.Number) it.fillMaxSize() else it },
                verticalAlignment = if (config.style == WidgetStyle.Number) {
                    Alignment.CenterVertically
                } else {
                    Alignment.Top
                },
            ) {
                Text(
                    listOfNotNull(config.scope?.let { "/$it" }, config.metricLabel, config.rangeLabel)
                        .joinToString(" · "),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                    maxLines = 1,
                )
                val label = NumberFormat.getIntegerInstance().format(total)
                val compact = size.width.value < 220f || size.height.value < 100f
                Text(
                    label,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = when {
                            label.length <= 7 -> if (compact) 34.sp else 44.sp
                            label.length <= 10 -> if (compact) 27.sp else 36.sp
                            else -> if (compact) 21.sp else 28.sp
                        },
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun SignedOutContent(context: Context) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.surface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "spoo.me",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.height(10.dp))
            Button(
                text = "Sign in",
                onClick = actionStartActivity(
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                ),
            )
        }
    }
}

/**
 * Chart bitmaps for Glance (no Canvas composables there). Wave is the
 * in-app WavyClicksChart redrawn with android.graphics: Catmull-Rom cubics
 * with clamped control Ys, gradient fill, round-capped stroke. Bars bucket
 * the series down to a hand-countable number of rounded columns.
 */
private fun renderChartBitmap(
    style: WidgetStyle,
    series: List<Int>,
    width: Int,
    height: Int,
    accent: Int,
    density: Float,
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    when (style) {
        WidgetStyle.Bars -> drawBars(canvas, series, width, height, accent, density)
        else -> drawWave(canvas, series, width, height, accent, density)
    }
    return bitmap
}

private fun drawWave(
    canvas: Canvas,
    series: List<Int>,
    width: Int,
    height: Int,
    accent: Int,
    density: Float,
) {
    val strokeWidth = 3f * density
    val inset = strokeWidth
    val chartHeight = height - inset * 2
    val max = series.max().coerceAtLeast(1).toFloat()
    val points = series.mapIndexed { i, clicks ->
        (width.toFloat() * i / (series.size - 1)) to (inset + chartHeight * (1f - clicks / max))
    }

    val path = Path().apply {
        moveTo(points.first().first, points.first().second)
        for (i in 0 until points.lastIndex) {
            val p0 = points.getOrElse(i - 1) { points[i] }
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points.getOrElse(i + 2) { p2 }
            cubicTo(
                p1.first + (p2.first - p0.first) / 6f,
                (p1.second + (p2.second - p0.second) / 6f).coerceIn(inset, height.toFloat()),
                p2.first - (p3.first - p1.first) / 6f,
                (p2.second - (p3.second - p1.second) / 6f).coerceIn(inset, height.toFloat()),
                p2.first, p2.second,
            )
        }
    }

    val fill = Path(path).apply {
        lineTo(width.toFloat(), height.toFloat())
        lineTo(0f, height.toFloat())
        close()
    }
    canvas.drawPath(
        fill,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                ColorUtils.setAlphaComponent(accent, 61), // 24%
                ColorUtils.setAlphaComponent(accent, 0),
                Shader.TileMode.CLAMP,
            )
        },
    )
    canvas.drawPath(
        path,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = accent
        },
    )
}

private fun drawBars(
    canvas: Canvas,
    series: List<Int>,
    width: Int,
    height: Int,
    accent: Int,
    density: Float,
) {
    // Bucket long series down so bars stay readable at widget scale.
    val maxBars = 24
    val buckets = if (series.size <= maxBars) {
        series
    } else {
        val per = series.size / maxBars.toFloat()
        List(maxBars) { i ->
            val from = (i * per).toInt()
            val to = (((i + 1) * per).toInt()).coerceAtMost(series.size)
            series.subList(from, to.coerceAtLeast(from + 1)).sum()
        }
    }
    val max = buckets.max().coerceAtLeast(1).toFloat()
    val slot = width.toFloat() / buckets.size
    val barWidth = slot * 0.62f
    val radius = minOf(barWidth / 2f, 3f * density)
    val minBar = 2f * density // zero-ish days still leave a tick, not a gap
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(accent, 217) // 85%
    }
    buckets.forEachIndexed { i, value ->
        val barHeight = (height * value / max).coerceAtLeast(minBar)
        val left = i * slot + (slot - barWidth) / 2f
        canvas.drawRoundRect(
            RectF(left, height - barHeight, left + barWidth, height.toFloat() + radius),
            radius, radius, paint,
        )
    }
}

/** The three picker shells; all render [SpooWidget], differing in prefill. */
class SpooWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpooWidget()
}

class BarsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpooWidget()
}

class CountWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpooWidget()
}
