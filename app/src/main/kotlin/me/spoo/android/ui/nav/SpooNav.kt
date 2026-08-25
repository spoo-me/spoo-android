package me.spoo.android.ui.nav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    Column(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .then(
                    // The bar owns the bottom inset while it is visible.
                    if (atRoot) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier,
                ),
        ) {
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
            // Content dissolves into the bar instead of hitting an edge.
            if (atRoot) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, MaterialTheme.colorScheme.surface),
                            ),
                        ),
                )
            }
        }
        // The M3 Expressive navigation bar (the shorter redesign), sitting
        // on the ground color so the fade above lands seamlessly.
        if (atRoot) {
            ShortNavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                windowInsets = ShortNavigationBarDefaults.windowInsets
                    .add(WindowInsets(top = 10.dp)),
            ) {
                tabs.forEach { tab ->
                    ShortNavigationBarItem(
                        selected = currentRoot == tab.key,
                        onClick = { switchTab(tab.key) },
                        icon = {
                            Icon(
                                when (tab.key) {
                                    LinksKey -> Icons.Outlined.Link
                                    AnalyticsKey -> Icons.Outlined.BarChart
                                    else -> Icons.Outlined.Settings
                                },
                                contentDescription = null,
                            )
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
    }
}
