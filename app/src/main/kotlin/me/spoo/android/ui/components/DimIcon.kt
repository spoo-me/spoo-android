package me.spoo.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import java.util.Locale

/**
 * Identity icons for dimension values, mirroring the webapp: favicons for
 * hosts/referrers (gstatic 404s honestly, so Coil's error slot draws the
 * globe), flag emoji for countries, neutral monograms elsewhere. Identity
 * color lives in the icon; surrounding chrome stays neutral.
 */

/** Favicon for a host, globe fallback when the host has none. */
@Composable
fun Favicon(host: String?, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    val globe = rememberVectorPainter(Icons.Outlined.Public)
    if (host.isNullOrBlank()) {
        Icon(
            Icons.Outlined.Public,
            contentDescription = null,
            modifier = modifier.size(size),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    AsyncImage(
        model = "https://t2.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON" +
            "&fallback_opts=TYPE,SIZE&url=https://$host&size=64",
        contentDescription = null,
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.extraSmall),
        error = globe,
        fallback = globe,
    )
}

/** Flag emoji for an ISO-3166 alpha-2 code; a globe for unknowns. */
@Composable
fun CountryFlag(code: String, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    val emoji = flagEmoji(code)
    if (emoji == null) {
        Icon(
            Icons.Outlined.Public,
            contentDescription = null,
            modifier = modifier.size(size),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Box(modifier.size(size), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 15.sp)
        }
    }
}

/** Neutral monogram circle for values with no natural artwork (browsers). */
@Composable
fun Monogram(label: String, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "US" -> "United States"; passes unknown values through untouched. */
fun countryDisplayName(code: String): String {
    if (code.length != 2 || !code.all { it.isLetter() }) return code
    val name = Locale("", code.uppercase()).getDisplayCountry(Locale.ENGLISH)
    return if (name.isNullOrBlank() || name == code.uppercase()) code else name
}

private fun flagEmoji(code: String): String? {
    if (code.length != 2 || !code.all { it.isLetter() }) return null
    val upper = code.uppercase()
    if (Locale("", upper).getDisplayCountry(Locale.ENGLISH) == upper) return null
    return buildString {
        upper.forEach { appendCodePoint(it.code - 'A'.code + 0x1F1E6) }
    }
}

/** Host of a URL for favicon lookups, null when unparsable. */
fun faviconHost(url: String): String? = runCatching {
    java.net.URI(url).host?.removePrefix("www.")
}.getOrNull()
