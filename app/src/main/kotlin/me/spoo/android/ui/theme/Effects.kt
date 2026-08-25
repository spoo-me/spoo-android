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

/** Tinted card on the clean ground, both themes. */
@Composable
fun cardContainerColor(): Color = MaterialTheme.colorScheme.surfaceContainerLow

/**
 * Card chrome is tonal only: color instead of shadows communicates
 * elevation (the M3 rule zingzy adopted). Kept as the single hook so a
 * future interaction-lift lands in one place.
 */
@Composable
fun Modifier.cardChrome(shape: Shape): Modifier = this
