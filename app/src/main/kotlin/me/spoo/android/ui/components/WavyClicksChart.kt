package me.spoo.android.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * The hero clicks-over-time chart: hand-rolled Canvas per the M3E doctrine
 * (no chart library). Catmull-Rom-smoothed cubic Beziers through the daily
 * points, a PathMeasure draw-on animation on the motion scheme's slow
 * spatial spring, and a gradient fill that fades in behind the stroke.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WavyClicksChart(
    dailyClicks: List<Int>,
    modifier: Modifier = Modifier,
) {
    val stroke = MaterialTheme.colorScheme.primary
    val fillTop = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
    val baseline = MaterialTheme.colorScheme.outlineVariant

    val progress = remember { Animatable(0f) }
    val spring = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(dailyClicks) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = spring)
    }

    Canvas(modifier) {
        if (dailyClicks.size < 2) return@Canvas
        val strokeWidth = 3.dp.toPx()
        val inset = strokeWidth // keep round caps and peaks inside the bounds
        val chartHeight = size.height - inset * 2
        val max = dailyClicks.max().coerceAtLeast(1).toFloat()

        val points = dailyClicks.mapIndexed { i, clicks ->
            Offset(
                x = size.width * i / (dailyClicks.size - 1),
                y = inset + chartHeight * (1f - clicks / max),
            )
        }

        // Catmull-Rom -> cubic Bezier control points for a smooth wave.
        // Control Ys are clamped so flat-then-spike series don't overshoot
        // the baseline or the top edge.
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.lastIndex) {
                val p0 = points.getOrElse(i - 1) { points[i] }
                val p1 = points[i]
                val p2 = points[i + 1]
                val p3 = points.getOrElse(i + 2) { p2 }
                cubicTo(
                    p1.x + (p2.x - p0.x) / 6f,
                    (p1.y + (p2.y - p0.y) / 6f).coerceIn(inset, size.height),
                    p2.x - (p3.x - p1.x) / 6f,
                    (p2.y - (p3.y - p1.y) / 6f).coerceIn(inset, size.height),
                    p2.x, p2.y,
                )
            }
        }

        drawLine(
            color = baseline,
            start = Offset(0f, size.height - 0.5f),
            end = Offset(size.width, size.height - 0.5f),
            strokeWidth = 1.dp.toPx(),
        )

        val fill = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = fill,
            brush = Brush.verticalGradient(listOf(fillTop, fillTop.copy(alpha = 0f))),
            alpha = progress.value,
        )

        val measure = PathMeasure().apply { setPath(path, forceClosed = false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * progress.value, partial, startWithMoveTo = true)
        drawPath(
            path = partial,
            color = stroke,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}
