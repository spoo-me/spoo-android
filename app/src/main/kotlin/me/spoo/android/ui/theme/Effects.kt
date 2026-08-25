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
            elevation = 6.dp,
            shape = shape,
            spotColor = Color.Black.copy(alpha = 0.12f),
            ambientColor = Color.Black.copy(alpha = 0.07f),
        )
    } else {
        this
    }

/** White card in light, elevated tone in dark. */
@Composable
fun cardContainerColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color.White
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

/** Shadow + hairline: the full premium card chrome, both themes. */
@Composable
fun Modifier.cardChrome(shape: Shape): Modifier {
    val light = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    return softCardShadow(shape).then(
        Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
                .copy(alpha = if (light) 0.55f else 0.4f),
            shape = shape,
        ),
    )
}
