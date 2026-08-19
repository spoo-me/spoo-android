package me.spoo.android.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import me.spoo.android.ui.screens.LinksScreen
import me.spoo.android.ui.screens.StatsScreen

@Serializable
data object LinksKey : NavKey

@Serializable
data class StatsKey(val shortCode: String) : NavKey

@Composable
fun SpooNav(sharedText: String?) {
    val backStack = rememberNavBackStack(LinksKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<LinksKey> {
                LinksScreen(
                    sharedText = sharedText,
                    onOpenStats = { code -> backStack.add(StatsKey(code)) },
                )
            }
            entry<StatsKey> { key ->
                StatsScreen(shortCode = key.shortCode)
            }
        },
    )
}
