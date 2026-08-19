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

        val sharedText = if (intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else {
            null
        }

        val authManager = SpooApp.graph.authManager

        setContent {
            SpooTheme {
                val authState by authManager.state.collectAsState()
                SpooNav(
                    sharedText = sharedText,
                    authState = authState,
                    onSignIn = { authManager.startSignIn(this) },
                    onSignOut = authManager::signOut,
                )
            }
        }
    }
}
