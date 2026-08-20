package me.spoo.android.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import me.spoo.android.auth.AuthState
import me.spoo.android.ui.screens.AccountScreen
import me.spoo.android.ui.screens.LinksScreen
import me.spoo.android.ui.screens.StatsScreen

@Serializable
data object LinksKey : NavKey

@Serializable
data class StatsKey(val shortCode: String) : NavKey

@Serializable
data object AccountKey : NavKey

@Composable
fun SpooNav(
    prefillText: String?,
    startInCreate: Boolean,
    authState: AuthState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    val backStack = rememberNavBackStack(LinksKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<LinksKey> {
                LinksScreen(
                    prefillText = prefillText,
                    startInCreate = startInCreate,
                    onOpenStats = { code -> backStack.add(StatsKey(code)) },
                    onOpenAccount = { backStack.add(AccountKey) },
                )
            }
            entry<StatsKey> { key ->
                StatsScreen(shortCode = key.shortCode)
            }
            entry<AccountKey> {
                AccountScreen(
                    authState = authState,
                    onSignIn = onSignIn,
                    onSignOut = onSignOut,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
