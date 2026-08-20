package me.spoo.android.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.asAndroidPath
import androidx.core.graphics.ColorUtils
import java.text.NumberFormat
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import me.spoo.android.data.LinkStats
import me.spoo.android.data.StatsDim
import me.spoo.android.ui.components.WorldMapCache
import me.spoo.android.ui.components.countryDisplayName
import me.spoo.android.ui.components.flagEmoji

/** Theme ints resolved by the caller (Glance or compose preview). */
data class ChartPalette(
    val accent: Int,
    val onSurface: Int,
    val onSurfaceVariant: Int,
    /** Zero-value ground for the map. */
    val surfaceVariant: Int,
    /** Cold end of the choropleth ramp. */
    val accentContainer: Int,
)

/**
 * Chart bitmaps for the widgets (Glance has no Canvas composables) and the
 * config-screen preview, so what you configure is literally what renders.
 */
object WidgetChartRenderer {

    fun render(
        context: Context,
        config: WidgetConfig,
        data: WidgetData,
        width: Int,
        height: Int,
        density: Float,
        palette: ChartPalette,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(
            width.coerceAtLeast(2),
            height.coerceAtLeast(2),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        when (config.chart) {
            WidgetChart.Wave -> drawWave(canvas, data.series, palette, density)
            WidgetChart.Bars -> drawBars(canvas, data.series, palette, density)
            WidgetChart.Treemap ->
                drawTreemap(context, canvas, data.slices, config.effectiveDimension, palette, density)
            WidgetChart.Bubbles ->
                drawBubbles(context, canvas, data.slices, config.effectiveDimension, palette, density)
            WidgetChart.Map -> drawMap(context, canvas, data.slices, palette)
            WidgetChart.Number -> Unit
        }
        return bitmap
    }

    private val numbers: NumberFormat = NumberFormat.getIntegerInstance()

    private fun labelPaint(palette: ChartPalette, density: Float) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.onSurface
            textSize = 12f * density
        }

    private fun countPaint(palette: ChartPalette, density: Float) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.onSurfaceVariant
            textSize = 10.5f * density
            typeface = Typeface.MONOSPACE
        }

    private fun display(dim: StatsDim, label: String) = when (dim) {
        StatsDim.Country -> countryDisplayName(label)
        else -> label
    }

    /**
     * Identity mark for a dimension value: prefetched favicon (rounded),
     * flag emoji for countries, monogram circle when neither exists.
     */
    private fun drawIcon(
        canvas: Canvas,
        context: Context,
        dim: StatsDim,
        label: String,
        left: Float,
        top: Float,
        size: Float,
        palette: ChartPalette,
    ) {
        if (dim == StatsDim.Country) {
            val emoji = flagEmoji(label)
            if (emoji != null) {
                canvas.drawText(
                    emoji, left, top + size * 0.85f,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size },
                )
                return
            }
        }
        val icon = WidgetIconCache.hostFor(dim, label)
            ?.let { WidgetIconCache.get(context, it) }
        if (icon != null) {
            val rect = RectF(left, top, left + size, top + size)
            val clip = Path().apply {
                addRoundRect(rect, size * 0.22f, size * 0.22f, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clip)
            canvas.drawBitmap(icon, null, rect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            canvas.restore()
            return
        }
        canvas.drawCircle(
            left + size / 2, top + size / 2, size / 2,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ColorUtils.setAlphaComponent(palette.onSurface, 31)
            },
        )
        canvas.drawText(
            label.firstOrNull()?.uppercase() ?: "?",
            left + size / 2, top + size * 0.71f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.onSurface
                textSize = size * 0.56f
                textAlign = Paint.Align.CENTER
            },
        )
    }

    // ---- time charts --------------------------------------------------

    private fun drawWave(canvas: Canvas, series: List<Int>, palette: ChartPalette, density: Float) {
        if (series.size < 2) return
        val width = canvas.width
        val height = canvas.height
        val strokeWidth = 3f * density
        val inset = strokeWidth
        val chartHeight = height - inset * 2
        val maxValue = series.max().coerceAtLeast(1).toFloat()
        val points = series.mapIndexed { i, clicks ->
            (width.toFloat() * i / (series.size - 1)) to
                (inset + chartHeight * (1f - clicks / maxValue))
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
                    ColorUtils.setAlphaComponent(palette.accent, 61), // 24%
                    ColorUtils.setAlphaComponent(palette.accent, 0),
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
                color = palette.accent
            },
        )
    }

    private fun drawBars(canvas: Canvas, series: List<Int>, palette: ChartPalette, density: Float) {
        if (series.isEmpty()) return
        val width = canvas.width
        val height = canvas.height
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
        val maxValue = buckets.max().coerceAtLeast(1).toFloat()
        val slot = width.toFloat() / buckets.size
        val barWidth = slot * 0.62f
        val radius = min(barWidth / 2f, 3f * density)
        val minBar = 2f * density // zero-ish days still leave a tick, not a gap
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ColorUtils.setAlphaComponent(palette.accent, 217) // 85%
        }
        buckets.forEachIndexed { i, value ->
            val barHeight = (height * value / maxValue).coerceAtLeast(minBar)
            val left = i * slot + (slot - barWidth) / 2f
            canvas.drawRoundRect(
                RectF(left, height - barHeight, left + barWidth, height.toFloat() + radius),
                radius, radius, paint,
            )
        }
    }

    // ---- breakdown charts ---------------------------------------------

    private fun drawTreemap(
        context: Context,
        canvas: Canvas,
        slices: List<LinkStats.Slice>,
        dim: StatsDim,
        palette: ChartPalette,
        density: Float,
    ) {
        val top = slices.sortedByDescending { it.count }.take(8).filter { it.count > 0 }
        if (top.isEmpty()) return
        val gap = 2f * density
        val radius = 3f * density
        val rects = squarify(
            top.map { it.count.toFloat() },
            RectF(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat()),
        )
        val labelP = labelPaint(palette, density)
        val countP = countPaint(palette, density)
        val icon = 16f * density

        rects.forEachIndexed { i, rect ->
            val inner = RectF(
                rect.left + gap / 2, rect.top + gap / 2,
                rect.right - gap / 2, rect.bottom - gap / 2,
            )
            // Rank-ramped accent, brightest cell first, like the dashboard.
            val alpha = (222 - i * 24).coerceAtLeast(56)
            canvas.drawRoundRect(
                inner, radius, radius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = ColorUtils.setAlphaComponent(palette.accent, alpha)
                },
            )
            val pad = 9f * density
            when {
                // Wide: icon + name inline, count beneath.
                inner.width() > 110f * density && inner.height() > 48f * density -> {
                    drawIcon(canvas, context, dim, top[i].label, inner.left + pad, inner.top + pad, icon, palette)
                    val textLeft = inner.left + pad + icon + 6f * density
                    val name = display(dim, top[i].label)
                    val clipped = clipText(name, labelP, inner.right - pad - textLeft)
                    canvas.drawText(clipped, textLeft, inner.top + pad + 12.5f * density, labelP)
                    canvas.drawText(
                        numbers.format(top[i].count),
                        inner.left + pad, inner.top + pad + icon + 14f * density, countP,
                    )
                }
                // Narrow: identity + count only, like the dashboard's tail cells.
                inner.width() > 40f * density && inner.height() > 48f * density -> {
                    drawIcon(canvas, context, dim, top[i].label, inner.left + pad, inner.top + pad, icon, palette)
                    canvas.drawText(
                        numbers.format(top[i].count),
                        inner.left + pad, inner.top + pad + icon + 14f * density, countP,
                    )
                }
                // Squat but wide enough: icon + count on one line.
                inner.width() > 64f * density && inner.height() > 26f * density -> {
                    val small = 12f * density
                    drawIcon(canvas, context, dim, top[i].label, inner.left + pad, inner.top + pad, small, palette)
                    canvas.drawText(
                        numbers.format(top[i].count),
                        inner.left + pad + small + 5f * density,
                        inner.top + pad + small - 1.5f * density, countP,
                    )
                }
            }
        }
    }

    /** Squarified treemap (Bruls et al.): rows of near-square cells. */
    private fun squarify(values: List<Float>, bounds: RectF): List<RectF> {
        val total = values.sum()
        val area = bounds.width() * bounds.height()
        val scaled = values.map { it / total * area }
        val out = mutableListOf<RectF>()
        var free = RectF(bounds)
        var row = mutableListOf<Float>()
        var i = 0

        fun worst(row: List<Float>, side: Float): Float {
            val sum = row.sum()
            val maxV = row.max()
            val minV = row.min()
            val s2 = sum * sum
            return max(side * side * maxV / s2, s2 / (side * side * minV))
        }

        fun layoutRow(row: List<Float>, last: Boolean) {
            val sum = row.sum()
            val horizontal = free.width() >= free.height() // row fills the short side
            if (horizontal) {
                val rowWidth = if (last) free.width() else sum / free.height()
                var y = free.top
                row.forEach { v ->
                    val h = v / rowWidth
                    out += RectF(free.left, y, free.left + rowWidth, y + h)
                    y += h
                }
                free = RectF(free.left + rowWidth, free.top, free.right, free.bottom)
            } else {
                val rowHeight = if (last) free.height() else sum / free.width()
                var x = free.left
                row.forEach { v ->
                    val w = v / rowHeight
                    out += RectF(x, free.top, x + w, free.top + rowHeight)
                    x += w
                }
                free = RectF(free.left, free.top + rowHeight, free.right, free.bottom)
            }
        }

        while (i < scaled.size) {
            val side = min(free.width(), free.height()).coerceAtLeast(1f)
            val v = scaled[i]
            if (row.isEmpty() || worst(row + v, side) <= worst(row, side)) {
                row += v
                i++
            } else {
                layoutRow(row, last = false)
                row = mutableListOf()
            }
        }
        if (row.isNotEmpty()) layoutRow(row, last = true)
        return out
    }

    private fun drawBubbles(
        context: Context,
        canvas: Canvas,
        slices: List<LinkStats.Slice>,
        dim: StatsDim,
        palette: ChartPalette,
        density: Float,
    ) {
        val top = slices.sortedByDescending { it.count }.take(9).filter { it.count > 0 }
        if (top.isEmpty()) return
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        val maxValue = top.first().count.toFloat()
        val maxR = min(w, h) * 0.34f
        val margin = 2f * density

        data class Bubble(val cx: Float, val cy: Float, val r: Float, val slice: LinkStats.Slice)

        val placed = mutableListOf<Bubble>()
        top.forEachIndexed { index, slice ->
            val r = (maxR * sqrt(slice.count / maxValue)).coerceAtLeast(7f * density)
            if (placed.isEmpty()) {
                placed += Bubble(w / 2, h / 2, r, slice)
                return@forEachIndexed
            }
            // Greedy spiral out from center to the first non-overlapping spot.
            var t = 0f
            while (t < 40f) {
                val angle = index * 2.4f + t
                val dist = t / 40f * (max(w, h) / 2f)
                val cx = (w / 2 + cos(angle) * dist).coerceIn(r, w - r)
                val cy = (h / 2 + sin(angle) * dist * (h / w)).coerceIn(r, h - r)
                val clear = placed.all { hypot(it.cx - cx, it.cy - cy) >= it.r + r + margin }
                if (clear) {
                    placed += Bubble(cx, cy, r, slice)
                    return@forEachIndexed
                }
                t += 0.15f
            }
            // No room left: drop the tail bubble rather than overlap.
        }

        val labelP = labelPaint(palette, density).apply { textAlign = Paint.Align.CENTER }
        val countP = countPaint(palette, density).apply { textAlign = Paint.Align.CENTER }
        placed.forEachIndexed { i, b ->
            val alpha = (222 - i * 22).coerceAtLeast(56)
            canvas.drawCircle(
                b.cx, b.cy, b.r,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = ColorUtils.setAlphaComponent(palette.accent, alpha)
                },
            )
            when {
                // Big: icon over name over count, all centered.
                b.r > 30f * density -> {
                    val icon = 18f * density
                    drawIcon(canvas, context, dim, b.slice.label, b.cx - icon / 2, b.cy - icon - 10f * density, icon, palette)
                    val name = clipText(display(dim, b.slice.label), labelP, b.r * 1.7f)
                    canvas.drawText(name, b.cx, b.cy + 5f * density, labelP)
                    canvas.drawText(numbers.format(b.slice.count), b.cx, b.cy + 18f * density, countP)
                }
                // Medium: the identity mark alone, like the dashboard.
                b.r > 12f * density -> {
                    val icon = minOf(16f * density, b.r * 0.95f)
                    drawIcon(canvas, context, dim, b.slice.label, b.cx - icon / 2, b.cy - icon / 2, icon, palette)
                }
            }
        }
    }

    private fun drawMap(
        context: Context,
        canvas: Canvas,
        slices: List<LinkStats.Slice>,
        palette: ChartPalette,
    ) {
        val world = runCatching { WorldMapCache.load(context) }.getOrNull() ?: return
        val counts = slices.associate { it.label.lowercase() to it.count }
        val maxValue = counts.values.maxOrNull()?.coerceAtLeast(1) ?: 1

        val scale = min(canvas.width / world.width, canvas.height / world.height)
        canvas.save()
        canvas.translate(
            (canvas.width - world.width * scale) / 2f,
            (canvas.height - world.height * scale) / 2f,
        )
        canvas.scale(scale, scale)
        canvas.translate(-world.minX, -world.minY)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        world.countries.forEach { country ->
            val clicks = counts[country.iso] ?: 0
            paint.color = if (clicks == 0) {
                palette.surfaceVariant
            } else {
                ColorUtils.blendARGB(palette.accentContainer, palette.accent, clicks / maxValue.toFloat())
            }
            canvas.drawPath(country.path.asAndroidPath(), paint)
        }
        canvas.restore()
    }

    private fun clipText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val count = paint.breakText(text, true, (maxWidth - paint.measureText("…")).coerceAtLeast(0f), null)
        return text.take(count).trimEnd() + "…"
    }
}
