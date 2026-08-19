package me.spoo.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Receives the device-auth redirect (spoo://oauth/callback?code=&state=),
 * hands it to the AuthManager, and trampolines back to the main task so the
 * Custom Tab disappears from the back stack.
 */
class OAuthCallbackActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.data?.let { SpooApp.graph.authManager.handleCallback(it) }
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
        )
        finish()
    }
}
