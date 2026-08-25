package me.spoo.android.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import me.spoo.android.R

/** Tabular figures for anything that counts — metrics never jitter. */
val TextStyle.tabular: TextStyle
    get() = copy(fontFeatureSettings = "tnum")

/**
 * Roboto Flex at a pinned axis position. Font weight and style weight
 * are kept equal so no fake-bold synthesis creeps in.
 */
@OptIn(ExperimentalTextApi::class)
private fun flex(weight: Int, width: Float) = FontFamily(
    Font(
        R.font.roboto_flex,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight),
            FontVariation.width(width),
        ),
    ),
)

private fun TextStyle.brand(weight: Int, width: Float = 104f) =
    copy(fontFamily = flex(weight, width), fontWeight = FontWeight(weight))

/**
 * The M3 brand/plain typeface split: Roboto Flex carries the expressive
 * tiers (display, headline, title), body and label stay plain Roboto.
 * Per the type-styles doctrine, baseline and emphasized are customized
 * as pairs — emphasized is consistently heavier AND wider than baseline.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun spooTypography(base: Typography = Typography()) = base.copy(
    displayLarge = base.displayLarge.brand(550, 104f),
    displayMedium = base.displayMedium.brand(550, 104f),
    displaySmall = base.displaySmall.brand(540, 104f),
    headlineLarge = base.headlineLarge.brand(550, 104f),
    headlineMedium = base.headlineMedium.brand(550, 104f),
    headlineSmall = base.headlineSmall.brand(540, 104f),
    titleLarge = base.titleLarge.brand(550, 103f),
    titleMedium = base.titleMedium.brand(560, 103f),
    displayLargeEmphasized = base.displayLargeEmphasized.brand(760, 108f),
    displayMediumEmphasized = base.displayMediumEmphasized.brand(760, 108f),
    displaySmallEmphasized = base.displaySmallEmphasized.brand(740, 108f),
    headlineLargeEmphasized = base.headlineLargeEmphasized.brand(700, 106f),
    headlineMediumEmphasized = base.headlineMediumEmphasized.brand(700, 106f),
    headlineSmallEmphasized = base.headlineSmallEmphasized.brand(680, 106f),
    titleLargeEmphasized = base.titleLargeEmphasized.brand(660, 105f),
    titleMediumEmphasized = base.titleMediumEmphasized.brand(650, 105f),
)

private fun heroFamily(weight: Int) = flex(weight, 108f)

/**
 * Hero style with the M3E animated-axis signature: the number lands at a
 * lighter weight and springs to full black whenever [key] changes, in
 * step with the chart's draw-on. One shot, spatial spring, no loop.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TextStyle.hero(key: Any?): TextStyle {
    val weight = remember { Animatable(450f) }
    val spring = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(key) {
        weight.snapTo(450f)
        weight.animateTo(800f, animationSpec = spring)
    }
    return copy(fontFamily = heroFamily(weight.value.toInt())).tabular
}
