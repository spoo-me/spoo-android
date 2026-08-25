package me.spoo.android.ui.theme

import androidx.compose.foundation.border
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/** Tinted card on the clean ground, both themes. */
@Composable
fun cardContainerColor(): Color = MaterialTheme.colorScheme.surfaceContainerLow

/**
 * Loader disc: a pale tint of primary over the ground, dark shape on top
 * (the M3E reference look). Derived, not a container role — medium
 * contrast darkens containers and inverts the disc.
 */
@Composable
fun loaderContainerColor(): Color =
    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        .compositeOver(MaterialTheme.colorScheme.surface)

/**
 * Card chrome is tonal only: color instead of shadows communicates
 * elevation (the M3 rule zingzy adopted). Kept as the single hook so a
 * future interaction-lift lands in one place.
 */
@Composable
fun Modifier.cardChrome(shape: Shape): Modifier = this

/**
 * Icon buttons on the pill rails: bare at rest (containers everywhere
 * read as chrome noise), secondaryContainer only while the control has
 * an active filter — state carried by the affordance, never a dot badge.
 */
@Composable
fun railIconColors(active: Boolean): IconButtonColors =
    if (active) {
        IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    } else {
        IconButtonDefaults.iconButtonColors()
    }
