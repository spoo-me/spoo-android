package me.spoo.android.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { System, Light, Dark }

/** What a horizontal swipe on a link card does; user-mapped per side. */
enum class SwipeAction(
    val label: String,
) {
    None("Nothing"),
    Copy("Copy link"),
    Share("Share"),
    Edit("Edit"),
    Qr("QR code"),
    Delete("Delete"),
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    /** Wallpaper-derived dynamic color (API 31+) vs the seed below. */
    val useDeviceColors: Boolean = true,
    val seedColor: Long = DEFAULT_SEED,
    val showShareInMenu: Boolean = true,
    val swipeRight: SwipeAction = SwipeAction.Edit,
    val swipeLeft: SwipeAction = SwipeAction.Delete,
    val swipeRightEnabled: Boolean = true,
    val swipeLeftEnabled: Boolean = true,
    /** Debug-only: fixture links + stats for design work, no backend. */
    val mockData: Boolean = false,
) {
    companion object {
        /** spoo violet. */
        const val DEFAULT_SEED = 0xFF8B5CF6

        val SEED_CHOICES =
            listOf(
                0xFF8B5CF6, // spoo violet
                0xFF0EA5E9, // sky
                0xFF10B981, // emerald
                0xFFF59E0B, // amber
                0xFFEF4444, // red
                0xFFEC4899, // pink
            )
    }
}

class SettingsRepository(
    private val context: Context,
) {
    val settings: Flow<AppSettings> =
        context.settingsDataStore.data.map { p ->
            AppSettings(
                themeMode =
                    p[THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                        ?: ThemeMode.System,
                useDeviceColors = p[USE_DEVICE_COLORS] ?: true,
                seedColor = p[SEED_COLOR] ?: AppSettings.DEFAULT_SEED,
                showShareInMenu = p[SHOW_SHARE] ?: true,
                swipeRight =
                    p[SWIPE_RIGHT]?.let { runCatching { SwipeAction.valueOf(it) }.getOrNull() }
                        ?: SwipeAction.Edit,
                swipeLeft =
                    p[SWIPE_LEFT]?.let { runCatching { SwipeAction.valueOf(it) }.getOrNull() }
                        ?: SwipeAction.Delete,
                swipeRightEnabled = p[SWIPE_RIGHT_ON] ?: true,
                swipeLeftEnabled = p[SWIPE_LEFT_ON] ?: true,
                mockData = p[MOCK_DATA] ?: false,
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) = context.settingsDataStore.edit { it[THEME_MODE] = mode.name }

    suspend fun setUseDeviceColors(value: Boolean) = context.settingsDataStore.edit { it[USE_DEVICE_COLORS] = value }

    suspend fun setSeedColor(argb: Long) = context.settingsDataStore.edit { it[SEED_COLOR] = argb }

    suspend fun setShowShareInMenu(value: Boolean) = context.settingsDataStore.edit { it[SHOW_SHARE] = value }

    suspend fun setSwipeRight(action: SwipeAction) = context.settingsDataStore.edit { it[SWIPE_RIGHT] = action.name }

    suspend fun setSwipeLeft(action: SwipeAction) = context.settingsDataStore.edit { it[SWIPE_LEFT] = action.name }

    suspend fun setSwipeRightEnabled(value: Boolean) = context.settingsDataStore.edit { it[SWIPE_RIGHT_ON] = value }

    suspend fun setSwipeLeftEnabled(value: Boolean) = context.settingsDataStore.edit { it[SWIPE_LEFT_ON] = value }

    suspend fun setMockData(value: Boolean) = context.settingsDataStore.edit { it[MOCK_DATA] = value }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DEVICE_COLORS = booleanPreferencesKey("use_device_colors")
        val SEED_COLOR = longPreferencesKey("seed_color")
        val SHOW_SHARE = booleanPreferencesKey("show_share_in_menu")
        val SWIPE_RIGHT = stringPreferencesKey("swipe_right")
        val SWIPE_LEFT = stringPreferencesKey("swipe_left")
        val SWIPE_RIGHT_ON = booleanPreferencesKey("swipe_right_enabled")
        val SWIPE_LEFT_ON = booleanPreferencesKey("swipe_left_enabled")
        val MOCK_DATA = booleanPreferencesKey("mock_data")
    }
}
