package me.spoo.android.ui.nav

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
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
import me.spoo.android.data.SwipeAction
import me.spoo.android.ui.screens.AnalyticsScreen
import me.spoo.android.ui.screens.LinksScreen
import me.spoo.android.ui.screens.SettingsScreen
import me.spoo.android.ui.screens.SignInGate
import me.spoo.android.ui.screens.StatsScreen

@Serializable
data object LinksKey : NavKey

@Serializable
data class StatsKey(
    val shortCode: String,
) : NavKey

@Serializable
data object AnalyticsKey : NavKey

@Serializable
data object SettingsKey : NavKey

private data class Tab(
    val key: NavKey,
    val label: String,
)

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

    val tabs =
        listOf(
            Tab(LinksKey, "Links"),
            Tab(AnalyticsKey, "Analytics"),
            Tab(SettingsKey, "Settings"),
        )
    // Android back doctrine: Links is the start destination and stays at
    // the bottom of the stack — back from any other tab returns to Links,
    // back from Links exits. Stats pushes on top as a detail.
    val currentKey = backStack.lastOrNull()
    val atRoot = currentKey !is StatsKey

    fun switchTab(key: NavKey) {
        if (currentKey == key) return
        backStack.clear()
        backStack.add(LinksKey)
        if (key != LinksKey) backStack.add(key)
    }

    Column(Modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    // The top-level fade dips through this ground between the
                    // outgoing and incoming screens: it must be surface, or
                    // the window background flashes through (white, in dark).
                    .background(MaterialTheme.colorScheme.surface)
                    .then(
                        // The bar owns the bottom inset while it is visible.
                        if (atRoot) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier,
                    ),
        ) {
            // Two named M3 patterns on the MOTION PHYSICS SYSTEM — spring
            // tokens only, no easing/duration tweens (that system is
            // legacy). Full-screen transitions take the SLOW tokens per the
            // speed table; exits ride the fast effects spring so they clear
            // early, keeping the top-level fade sequential in feel.
            // - Top level (tabs): quick fade-out, then fade-in + soft scale.
            // - Forward/backward (Stats detail): expressive slow spatial
            //   slides that overshoot into place, mirrored on pop.
            val slowSpatial = MaterialTheme.motionScheme.slowSpatialSpec<IntOffset>()
            val slowSpatialScale = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
            val slowEffects = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
            val fastEffects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
            val topLevel =
                (fadeIn(slowEffects) + scaleIn(slowSpatialScale, initialScale = 0.92f))
                    .togetherWith(fadeOut(fastEffects))
            val forward =
                (slideInHorizontally(slowSpatial) { it / 3 } + fadeIn(slowEffects))
                    .togetherWith(slideOutHorizontally(slowSpatial) { -it / 3 } + fadeOut(fastEffects))
            val backward =
                (slideInHorizontally(slowSpatial) { -it / 3 } + fadeIn(slowEffects))
                    .togetherWith(slideOutHorizontally(slowSpatial) { it / 3 } + fadeOut(fastEffects))
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                // Tabs: the top-level pattern in every direction. The Stats
                // entry overrides with forward/backward via its metadata.
                transitionSpec = { topLevel },
                popTransitionSpec = { topLevel },
                predictivePopTransitionSpec = { topLevel },
                entryProvider =
                    entryProvider {
                        entry<LinksKey> {
                            LinksScreen(
                                prefillText = prefillText,
                                startInCreate = startInCreate,
                                showShareInMenu = settings.showShareInMenu,
                                swipeRight = if (settings.swipeRightEnabled) settings.swipeRight else SwipeAction.None,
                                swipeLeft = if (settings.swipeLeftEnabled) settings.swipeLeft else SwipeAction.None,
                                onOpenStats = { code -> backStack.add(StatsKey(code)) },
                                onOpenSettings = { switchTab(SettingsKey) },
                            )
                        }
                        entry<StatsKey>(
                            // Forward/backward owns this destination: in from
                            // the trailing edge, back out the same way.
                            metadata =
                                NavDisplay.transitionSpec { forward } +
                                    NavDisplay.popTransitionSpec { backward } +
                                    NavDisplay.predictivePopTransitionSpec { backward },
                        ) { key ->
                            StatsScreen(
                                shortCode = key.shortCode,
                                onBack = { backStack.removeLastOrNull() },
                            )
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
                                onSetSwipeRight = { scope.launch { settingsRepo.setSwipeRight(it) } },
                                onSetSwipeLeft = { scope.launch { settingsRepo.setSwipeLeft(it) } },
                                onSetSwipeRightEnabled = { scope.launch { settingsRepo.setSwipeRightEnabled(it) } },
                                onSetSwipeLeftEnabled = { scope.launch { settingsRepo.setSwipeLeftEnabled(it) } },
                                onSetMockData = { scope.launch { settingsRepo.setMockData(it) } },
                                onSignOut = onSignOut,
                            )
                        }
                    },
            )
        }
        // The M3 Expressive navigation bar (the shorter redesign), sitting
        // on the ground color so the fade above lands seamlessly.
        if (atRoot) {
            ShortNavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                windowInsets =
                    ShortNavigationBarDefaults.windowInsets
                        .add(WindowInsets(top = 10.dp)),
            ) {
                tabs.forEach { tab ->
                    ShortNavigationBarItem(
                        selected = currentKey == tab.key,
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
