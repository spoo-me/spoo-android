package me.spoo.android.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import me.spoo.android.R

/** Tabular figures for anything that counts — metrics never jitter. */
val TextStyle.tabular: TextStyle
    get() = copy(fontFeatureSettings = "tnum")

/**
 * The one loud type moment per screen: hero metrics set in Roboto Flex,
 * heavy and slightly extended — the M3 Expressive variable-font move.
 */
@OptIn(ExperimentalTextApi::class)
private fun heroFamily(weight: Int) = FontFamily(
    Font(
        R.font.roboto_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight),
            FontVariation.width(108f),
        ),
    ),
)

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
