package me.spoo.android.ui.nav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import me.spoo.android.SpooApp
import me.spoo.android.auth.AuthState
import me.spoo.android.data.AppSettings
import me.spoo.android.ui.screens.AnalyticsScreen
import me.spoo.android.ui.screens.LinksScreen
import me.spoo.android.ui.screens.SettingsScreen
import me.spoo.android.ui.screens.SignInGate
import me.spoo.android.ui.screens.StatsScreen

@Serializable
data object LinksKey : NavKey

@Serializable
data class StatsKey(val shortCode: String) : NavKey

@Serializable
data object AnalyticsKey : NavKey

@Serializable
data object SettingsKey : NavKey

private data class Tab(val key: NavKey, val label: String)

@Composable
fun SpooNav(
    prefillText: String?,
    startInCreate: Boolean,
    authState: AuthState,
    settings: AppSettings,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    // Authed-only — except mock mode, which exists to explore the design.
    if (authState !is AuthState.SignedIn && !settings.mockData) {
        SignInGate(authState = authState, onSignIn = onSignIn)
        return
    }

    val backStack = rememberNavBackStack(LinksKey)
    val scope = rememberCoroutineScope()
    val settingsRepo = SpooApp.graph.settingsRepository

    val tabs = listOf(
        Tab(LinksKey, "Links"),
        Tab(AnalyticsKey, "Analytics"),
        Tab(SettingsKey, "Settings"),
    )
    val currentRoot = backStack.firstOrNull()
    val atRoot = backStack.size == 1

    fun switchTab(key: NavKey) {
        if (currentRoot == key && atRoot) return
        backStack.clear()
        backStack.add(key)
    }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
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
                        )
                    }
                    entry<StatsKey> { key ->
                        StatsScreen(shortCode = key.shortCode)
                    }
                    entry<AnalyticsKey> {
                        AnalyticsScreen()
                    }
                    entry<SettingsKey> {
                        SettingsScreen(
                            username = (authState as? AuthState.SignedIn)?.username ?: "mock mode",
                            settings = settings,
                            onSetThemeMode = { scope.launch { settingsRepo.setThemeMode(it) } },
                            onSetUseDeviceColors = { scope.launch { settingsRepo.setUseDeviceColors(it) } },
                            onSetSeedColor = { scope.launch { settingsRepo.setSeedColor(it) } },
                            onSetShowShare = { scope.launch { settingsRepo.setShowShareInMenu(it) } },
                            onSetMockData = { scope.launch { settingsRepo.setMockData(it) } },
                            onSignOut = onSignOut,
                        )
                    }
                },
            )
        }
        // Floating icon-only tab bar: content scrolls behind it.
        if (atRoot) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 6.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tabs.forEach { tab ->
                        val selected = currentRoot == tab.key
                        val icon = when (tab.key) {
                            LinksKey -> Icons.Outlined.Link
                            AnalyticsKey -> Icons.Outlined.Insights
                            else -> Icons.Outlined.Settings
                        }
                        if (selected) {
                            FilledIconButton(onClick = { switchTab(tab.key) }) {
                                Icon(icon, contentDescription = tab.label)
                            }
                        } else {
                            IconButton(onClick = { switchTab(tab.key) }) {
                                Icon(icon, contentDescription = tab.label)
                            }
                        }
                    }
                }
            }
        }
    }
}
