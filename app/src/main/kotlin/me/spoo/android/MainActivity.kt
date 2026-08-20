package me.spoo.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import me.spoo.android.ui.nav.SpooNav
import me.spoo.android.ui.theme.SpooTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefillText = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> intent?.getStringExtra(AppIntents.EXTRA_PREFILL_URL)
        }
        val startInCreate = prefillText != null || intent?.action == AppIntents.ACTION_SHORTEN

        val authManager = SpooApp.graph.authManager

        setContent {
            SpooTheme {
                val authState by authManager.state.collectAsState()
                SpooNav(
                    prefillText = prefillText,
                    startInCreate = startInCreate,
                    authState = authState,
                    onSignIn = { authManager.startSignIn(this) },
                    onSignOut = authManager::signOut,
                )
            }
        }
    }
}
