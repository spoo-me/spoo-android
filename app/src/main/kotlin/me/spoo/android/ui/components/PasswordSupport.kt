package me.spoo.android.ui.components

import android.app.Activity
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import java.security.SecureRandom

// Same recipe and word list as the web composer's Suggest button. The
// "." separators satisfy the backend's URL-password rule: a letter, a
// digit, and an "@" or "." with no two consecutive specials.
private val WORDS =
    (
        "amber anchor basil beacon birch bishop bramble bronze cedar cinder " +
            "cobalt comet copper coral cypress delta dune ember fable falcon " +
            "fennel fjord garnet gossamer granite harbor hazel heron indigo " +
            "ivory juniper kelp koala lantern larch lichen lumen maple marble " +
            "meadow nectar nimbus onyx opal orchid otter pebble pewter pixel " +
            "quartz quill raven reef rowan sable saffron slate sorrel spruce " +
            "thistle topaz tundra umber velvet willow zephyr zinc"
    ).split(" ")

private val rng = SecureRandom()

/**
 * Three words and three digits from [WORDS] (64 entries): about 28 bits,
 * where two words and two digits was ~15.6 — small enough to walk online
 * against a rate-limited endpoint in under two hours.
 */
internal fun suggestPassword(): String {
    fun word() = WORDS[rng.nextInt(WORDS.size)]
    return "${word()}.${word()}.${word()}.${100 + rng.nextInt(900)}"
}

/**
 * Marks the window secure while [active], so a revealed password stays
 * out of the Recents thumbnail, screenshots, and screen recordings. Scoped
 * to the reveal rather than the whole app: stats screens are worth
 * screenshotting.
 */
@Composable
internal fun SecureWhileVisible(active: Boolean) {
    val view = LocalView.current
    DisposableEffect(active, view) {
        // A bottom sheet renders in its own dialog window, so flagging the
        // activity alone leaves the field itself capturable; the activity
        // still needs it for the Recents thumbnail.
        val windows =
            listOfNotNull(
                (view.parent as? DialogWindowProvider)?.window,
                generateSequence(view.context) { (it as? ContextWrapper)?.baseContext }
                    .filterIsInstance<Activity>()
                    .firstOrNull()
                    ?.window,
            )
        if (active) windows.forEach { it.addFlags(WindowManager.LayoutParams.FLAG_SECURE) }
        onDispose {
            if (active) windows.forEach { it.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
        }
    }
}
