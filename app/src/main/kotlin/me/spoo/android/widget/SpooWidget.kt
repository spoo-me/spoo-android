package me.spoo.android.widget

import android.content.Context
import android.content.Intent
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import me.spoo.android.AppIntents
import me.spoo.android.MainActivity
import me.spoo.android.SpooApp

/**
 * Home-screen widget: quick shorten + the three most recent links.
 * Renders the last known repository state — stale data over spinners.
 */
class SpooWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent(context)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetContent(context: Context) {
        val links = SpooApp.graph.linksRepository.links.value.take(3)
        val numbers = NumberFormat.getIntegerInstance()

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.surface)
                .padding(14.dp),
        ) {
            Text(
                "spoo.me",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.height(8.dp))
            Button(
                text = "Shorten a link",
                onClick = actionStartActivity(
                    Intent(context, MainActivity::class.java)
                        .setAction(AppIntents.ACTION_SHORTEN)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                ),
                modifier = GlanceModifier.fillMaxWidth(),
            )
            Spacer(GlanceModifier.height(10.dp))
            if (links.isEmpty()) {
                val signedIn = SpooApp.graph.authManager.state.value is me.spoo.android.auth.AuthState.SignedIn
                Text(
                    if (signedIn) "No links yet" else "Open the app to sign in",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                )
            } else {
                links.forEach { link ->
                    Text(
                        "/${link.shortCode} · ${numbers.format(link.totalClicks)} clicks",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 13.sp,
                        ),
                        modifier = GlanceModifier.padding(vertical = 2.dp),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

class SpooWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpooWidget()
}
