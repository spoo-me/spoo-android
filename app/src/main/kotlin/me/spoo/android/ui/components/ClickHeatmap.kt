package me.spoo.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.min

/**
 * Contribution-style activity grid over daily clicks: one cell per day,
 * columns are weeks, the last cell is today. Intensity ramps on the
 * accent; empty days keep a quiet cell so the grid reads as a calendar,
 * not scattered dots.
 */
@Composable
fun ClickHeatmap(
    dailyClicks: List<Int>,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceContainerHigh

    Canvas(modifier) {
        if (dailyClicks.isEmpty()) return@Canvas
        val weeks = ceil(dailyClicks.size / 7f).toInt()
        val gap = 3.dp.toPx()
        val cell = min(
            (size.width - gap * (weeks - 1)) / weeks,
            (size.height - gap * 6) / 7f,
        )
        if (cell <= 0f) return@Canvas
        val radius = CornerRadius(cell * 0.28f)
        val max = dailyClicks.max().coerceAtLeast(1).toFloat()

        // Column-major, padded at the start so the final cell is the
        // latest day, exactly like a contribution calendar.
        val padStart = weeks * 7 - dailyClicks.size
        val left = 0f

        dailyClicks.forEachIndexed { i, clicks ->
            val slot = padStart + i
            val col = slot / 7
            val row = slot % 7
            drawRoundRect(
                color = if (clicks == 0) {
                    empty
                } else {
                    accent.copy(alpha = 0.25f + 0.75f * (clicks / max))
                },
                topLeft = Offset(left + col * (cell + gap), row * (cell + gap)),
                size = Size(cell, cell),
                cornerRadius = radius,
            )
        }
    }
}
