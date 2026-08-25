package me.spoo.android.ui.theme

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * The soft, wide, barely-there card shadow. Translucent shadow colors
 * diffuse what stock elevation renders harsh; dark theme skips it
 * entirely (shadows on near-black read as mud, tiers do the work there).
 */
@Composable
fun Modifier.softCardShadow(shape: Shape): Modifier =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        shadow(
            elevation = 7.dp,
            shape = shape,
            spotColor = Color.Black.copy(alpha = 0.16f),
            ambientColor = Color.Black.copy(alpha = 0.10f),
        )
    } else {
        this
    }

/** Tinted card on the clean ground, both themes. */
@Composable
fun cardContainerColor(): Color = MaterialTheme.colorScheme.surfaceContainerLow

/** The card chrome: soft shadow only — the tint edge is the border. */
@Composable
fun Modifier.cardChrome(shape: Shape): Modifier = softCardShadow(shape)
