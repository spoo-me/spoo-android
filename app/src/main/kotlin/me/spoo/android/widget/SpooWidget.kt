package me.spoo.android.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import java.text.NumberFormat
import kotlinx.coroutines.flow.first
import me.spoo.android.MainActivity
import me.spoo.android.SpooApp

/**
 * Home-screen widget, scoped per instance by [WidgetConfig] (chosen on
 * placement, editable via long-press). Everything the composition shows is
 * read from the instance's Glance state INSIDE the composition — a warm
 * Glance session recomposes without re-running provideGlance, so values
 * captured outside provideContent go stale after a reconfigure.
 */
class SpooWidget : GlanceAppWidget() {

    // Exact size so chart bitmaps are rendered 1:1 for the slot.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val graph = SpooApp.graph
        // TokenStore, not AuthManager state: a cold widget process may render
        // before restore() lands, and the token file is the durable truth.
        val signedIn = graph.tokenStore.read() != null ||
            graph.settingsRepository.settings.first().mockData

        val config = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
            .readWidgetConfig()
        val fresh = if (signedIn) fetchWidgetData(graph, config) else null
        updateAppWidgetState(context, id) {
            it[WidgetKeys.SIGNED_IN] = signedIn
            fresh?.let(it::writeWidgetData) // fetch failed: keep the cache
        }
        if (!config.chart.timeChart) {
            val slices = (fresh ?: getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
                .readWidgetData()).slices
            WidgetIconCache.prefetch(
                context, config.effectiveDimension,
                slices.sortedByDescending { s -> s.count }.take(9).map { s -> s.label },
            )
        }

        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                if (prefs[WidgetKeys.SIGNED_IN] != true) {
                    SignedOutContent(context)
                } else {
                    WidgetContent(context, prefs.readWidgetConfig(), prefs.readWidgetData())
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetContent(context: Context, config: WidgetConfig, data: WidgetData) {
        val size = LocalSize.current
        val density = context.resources.displayMetrics.density
        val palette = ChartPalette(
            accent = GlanceTheme.colors.primary.getColor(context).toArgb(),
            onSurface = GlanceTheme.colors.onSurface.getColor(context).toArgb(),
            onSurfaceVariant = GlanceTheme.colors.onSurfaceVariant.getColor(context).toArgb(),
            surfaceVariant = GlanceTheme.colors.surfaceVariant.getColor(context).toArgb(),
            accentContainer = GlanceTheme.colors.primaryContainer.getColor(context).toArgb(),
        )

        val openApp = actionStartActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )

        // Time charts underlay the bottom two-thirds beneath the label and
        // count; breakdown charts ARE the widget, full-bleed and unlabeled.
        val chartHeight = if (config.chart.timeChart) size.height * 0.68f else size.height
        val hasChart = config.chart != WidgetChart.Number &&
            (if (config.chart.timeChart) data.series.size >= 2 else data.slices.isNotEmpty())

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.surface)
                .clickable(openApp),
        ) {
            if (hasChart) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    Image(
                        provider = ImageProvider(
                            WidgetChartRenderer.render(
                                context = context,
                                config = config,
                                data = data,
                                width = (size.width.value * density).toInt(),
                                height = (chartHeight.value * density).toInt(),
                                density = density,
                                palette = palette,
                            ),
                        ),
                        contentDescription = null,
                        modifier = GlanceModifier.fillMaxWidth().height(chartHeight),
                        contentScale = ContentScale.FillBounds,
                    )
                }
            }
            if (config.chart.timeChart) {
                Column(
                    modifier = GlanceModifier
                        .padding(horizontal = 18.dp)
                        .padding(vertical = if (config.chart == WidgetChart.Number) 12.dp else 10.dp)
                        .let { if (config.chart == WidgetChart.Number) it.fillMaxSize() else it },
                    verticalAlignment = if (config.chart == WidgetChart.Number) {
                        Alignment.CenterVertically
                    } else {
                        Alignment.Top
                    },
                ) {
                    Text(
                        config.label,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                        maxLines = 1,
                    )
                    val label = NumberFormat.getIntegerInstance().format(data.total)
                    val compact = size.width.value < 220f || size.height.value < 100f
                    Text(
                        label,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = when {
                                label.length <= 7 -> if (compact) 34.sp else 44.sp
                                label.length <= 10 -> if (compact) 27.sp else 36.sp
                                else -> if (compact) 21.sp else 28.sp
                            },
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                }
            } else if (!hasChart) {
                Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No data in this range",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp,
                        ),
                    )
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun SignedOutContent(context: Context) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.surface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "spoo.me",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.height(10.dp))
            Button(
                text = "Sign in",
                onClick = actionStartActivity(
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                ),
            )
        }
    }
}

/** The picker shells; all render [SpooWidget], differing in prefill. */
class SpooWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpooWidget()
}

class BarsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpooWidget()
}

class CountWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpooWidget()
}

class TreemapWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpooWidget()
}
