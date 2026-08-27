package me.spoo.android.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import me.spoo.android.data.AppSettings
import me.spoo.android.data.ThemeMode

/**
 * The scheme the whole product uses — app AND widgets, so home-screen
 * colors follow the same choice as in-app ones. Device dynamic color
 * when the user wants it (and the API allows); otherwise a MaterialKolor
 * scheme derived from their seed, with the clean-ground surface pass.
 */
fun spooColorScheme(
    context: Context,
    settings: AppSettings,
    darkTheme: Boolean,
    /** Widgets skip this: on a wallpaper, the soft tonal surface beats
     *  the app's stark clean ground. */
    cleanGround: Boolean = true,
): ColorScheme {
    val useDevice = settings.useDeviceColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = if (useDevice) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // TonalSpot, not Expressive: a user-picked accent must keep its hue
        // (Expressive's rotations turn "emerald" into peach).
        dynamicColorScheme(
            seedColor = Color(settings.seedColor),
            isDark = darkTheme,
            style = PaletteStyle.TonalSpot,
            // Medium contrast: deeper accents and text per zingzy's call.
            contrastLevel = 0.5,
        )
    }

    if (!cleanGround) return colorScheme

    // Dynamic schemes tint every surface with the seed hue; the premium
    // read needs a neutral ground so true-white cards can sit on it
    // (near-black ground + elevated cards in dark). Containers keep
    // their soft tint — tiers stay visible, cast goes.
    return if (darkTheme) {
        colorScheme.copy(
            surface = Color(0xFF0B0B0D),
            background = Color(0xFF0B0B0D),
        )
    } else {
        // Containers should read as neutral surfaces with a whisper of the
        // seed (M3 derives them from the low-chroma neutral palette); the
        // generated ramp runs hotter, and against a true-white ground every
        // card reads as an accent-colored component. Halve the chroma,
        // keep the hue.
        fun soften(color: Color) = lerp(Color.White, color, 0.5f)
        colorScheme.copy(
            surface = Color.White,
            background = Color.White,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = soften(colorScheme.surfaceContainerLow),
            surfaceContainer = soften(colorScheme.surfaceContainer),
            surfaceContainerHigh = soften(colorScheme.surfaceContainerHigh),
            surfaceContainerHighest = soften(colorScheme.surfaceContainerHighest),
            surfaceVariant = soften(colorScheme.surfaceVariant),
        )
    }
}

/** Whether [settings] resolve to dark, given the system state. */
fun resolvesDark(settings: AppSettings, systemDark: Boolean): Boolean =
    when (settings.themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

@Composable
fun SpooTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit,
) {
    val darkTheme = resolvesDark(settings, isSystemInDarkTheme())
    val context = LocalContext.current
    val colorScheme = remember(settings, darkTheme) {
        spooColorScheme(context, settings, darkTheme)
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = remember { spooTypography() },
        content = content,
    )
}
