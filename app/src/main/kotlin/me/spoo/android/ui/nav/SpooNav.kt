package me.spoo.android.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import me.spoo.android.SpooApp
import me.spoo.android.auth.AuthState
import me.spoo.android.data.AppSettings
import me.spoo.android.ui.screens.AccountStatsScreen
import me.spoo.android.ui.screens.LinksScreen
import me.spoo.android.ui.screens.SettingsScreen
import me.spoo.android.ui.screens.SignInGate
import me.spoo.android.ui.screens.StatsScreen

@Serializable
data object LinksKey : NavKey

@Serializable
data class StatsKey(val shortCode: String) : NavKey

@Serializable
data object AccountStatsKey : NavKey

@Serializable
data object SettingsKey : NavKey

@Composable
fun SpooNav(
    prefillText: String?,
    startInCreate: Boolean,
    authState: AuthState,
    settings: AppSettings,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    // Authed-only: no anonymous surface at all.
    if (authState !is AuthState.SignedIn) {
        SignInGate(authState = authState, onSignIn = onSignIn)
        return
    }

    val backStack = rememberNavBackStack(LinksKey)
    val scope = rememberCoroutineScope()
    val settingsRepo = SpooApp.graph.settingsRepository

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<LinksKey> {
                LinksScreen(
                    prefillText = prefillText,
                    startInCreate = startInCreate,
                    showShareInMenu = settings.showShareInMenu,
                    onOpenStats = { code -> backStack.add(StatsKey(code)) },
                    onOpenInsights = { backStack.add(AccountStatsKey) },
                    onOpenSettings = { backStack.add(SettingsKey) },
                )
            }
            entry<StatsKey> { key ->
                StatsScreen(shortCode = key.shortCode)
            }
            entry<AccountStatsKey> {
                AccountStatsScreen(onBack = { backStack.removeLastOrNull() })
            }
            entry<SettingsKey> {
                SettingsScreen(
                    username = authState.username,
                    settings = settings,
                    onSetThemeMode = { scope.launch { settingsRepo.setThemeMode(it) } },
                    onSetUseDeviceColors = { scope.launch { settingsRepo.setUseDeviceColors(it) } },
                    onSetSeedColor = { scope.launch { settingsRepo.setSeedColor(it) } },
                    onSetShowShare = { scope.launch { settingsRepo.setShowShareInMenu(it) } },
                    onSignOut = onSignOut,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
