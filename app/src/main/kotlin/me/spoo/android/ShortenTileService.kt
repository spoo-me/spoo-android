package me.spoo.android

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService

/** "Shorten clipboard URL" quick-settings tile. */
class ShortenTileService : TileService() {
    // The Intent overload is the only one below API 34.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        val intent =
            Intent(this, ClipboardTrampolineActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
