package me.spoo.android

import android.content.ClipboardManager
import android.content.Intent
import androidx.activity.ComponentActivity

/**
 * QS-tile hop: the clipboard is only readable while a window of ours has
 * focus, so this translucent activity waits for focus, grabs the URL, and
 * forwards into the create sheet. The Android 13+ paste toast is expected.
 */
class ClipboardTrampolineActivity : ComponentActivity() {

    private var handled = false

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || handled) return
        handled = true

        val clipText = getSystemService(ClipboardManager::class.java)
            ?.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)?.coerceToText(this)?.toString()
        val url = clipText?.let { Regex("""https?://\S+""").find(it)?.value }

        startActivity(
            Intent(this, MainActivity::class.java).apply {
                action = AppIntents.ACTION_SHORTEN
                url?.let { putExtra(AppIntents.EXTRA_PREFILL_URL, it) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
        )
        finish()
    }
}
