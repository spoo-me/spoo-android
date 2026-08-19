package me.spoo.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

        setContent {
            SpooTheme {
                SpooNav(sharedText = sharedText)
            }
        }
    }
}
