package me.spoo.android.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * First-load stand-in for [StatsContent], shaped like the real layout
 * (hero, chart card, breakdowns) so structure appears immediately and
 * nothing jumps when data lands. A slow alpha pulse says "working"
 * without a spinner. Revisits skip this entirely via the stats cache.
 */
@Composable
fun StatsSkeleton(contentPadding: PaddingValues) {
    // Ambient pulse, not a transition: the infinite tween is deliberate
    // (springs don't loop), slow enough to read as breathing.
    val pulse by rememberInfiniteTransition(label = "skeleton")
        .animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "skeleton-alpha",
        )
    val tone = MaterialTheme.colorScheme.surfaceContainerHigh

    @Composable
    fun block(
        width: Dp?,
        height: Dp,
        radius: Dp = 16.dp,
    ) {
        Box(
            modifier =
                Modifier
                    .let { if (width != null) it.width(width) else it.fillMaxWidth() }
                    .height(height)
                    .alpha(pulse)
                    .background(tone, RoundedCornerShape(radius)),
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        block(width = 220.dp, height = 64.dp)
        block(width = 150.dp, height = 16.dp, radius = 8.dp)
        block(width = null, height = 244.dp, radius = 20.dp)
        block(width = null, height = 180.dp, radius = 20.dp)
        block(width = null, height = 180.dp, radius = 20.dp)
    }
}
