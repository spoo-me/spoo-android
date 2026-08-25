package me.spoo.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import me.spoo.android.data.AppSettings
import me.spoo.android.data.ThemeMode

/**
 * Device dynamic color when the user wants it (and the API allows);
 * otherwise a MaterialKolor expressive scheme derived from their seed.
 */
@Composable
fun SpooTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit,
) {
    val darkTheme = when (settings.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    val useDevice = settings.useDeviceColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = if (useDevice) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // TonalSpot, not Expressive: a user-picked accent must keep its hue
        // (Expressive's rotations turn "emerald" into peach).
        rememberDynamicColorScheme(
            seedColor = Color(settings.seedColor),
            isDark = darkTheme,
            style = PaletteStyle.TonalSpot,
        )
    }

    // Dynamic schemes tint every surface with the seed hue; the premium
    // read needs a neutral ground so true-white cards can sit on it
    // (near-black ground + elevated cards in dark). Containers keep
    // their soft tint — tiers stay visible, cast goes.
    val cleanGround = if (darkTheme) {
        colorScheme.copy(
            surface = Color(0xFF0B0B0D),
            background = Color(0xFF0B0B0D),
        )
    } else {
        colorScheme.copy(
            surface = Color.White,
            background = Color.White,
        )
    }

    MaterialExpressiveTheme(
        colorScheme = cleanGround,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
