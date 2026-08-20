package me.spoo.android.ui.nav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
    // Bumped by the pill's FAB; LinksScreen opens the create sheet on change.
    var createRequests by remember { mutableIntStateOf(0) }

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
                            createRequests = createRequests,
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
        // Floating icon-only tab bar; on Links it carries the create FAB.
        if (atRoot) {
            val tabItems: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
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
            val barModifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
            if (currentRoot == LinksKey) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    floatingActionButton = {
                        FloatingToolbarDefaults.VibrantFloatingActionButton(
                            onClick = { createRequests++ },
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = "Shorten a link")
                        }
                    },
                    modifier = barModifier,
                    content = tabItems,
                )
            } else {
                HorizontalFloatingToolbar(
                    expanded = true,
                    modifier = barModifier,
                    content = tabItems,
                )
            }
        }
    }
}
