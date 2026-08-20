package me.spoo.android.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import me.spoo.android.SpooApp
import me.spoo.android.data.AppSettings
import me.spoo.android.data.StatsMetric
import me.spoo.android.ui.components.Favicon
import me.spoo.android.ui.components.faviconHost
import me.spoo.android.ui.theme.SpooTheme

/**
 * The widget builder: launched by the launcher when a shell is placed
 * (android:configure) and again on long-press edit (reconfigurable).
 * Saves into the instance's Glance state and triggers a render.
 */
class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Backing out must cancel the placement, per the widget contract.
        setResult(
            RESULT_CANCELED,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val preset = WidgetConfig.presetFor(
            AppWidgetManager.getInstance(this)
                .getAppWidgetInfo(appWidgetId)?.provider?.className,
        )

        setContent {
            val settings by SpooApp.graph.settingsRepository.settings
                .collectAsState(initial = AppSettings())
            SpooTheme(settings = settings) {
                ConfigScreen(
                    appWidgetId = appWidgetId,
                    preset = preset,
                    onSave = { config -> save(appWidgetId, config) },
                )
            }
        }
    }

    private fun save(appWidgetId: Int, config: WidgetConfig) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity)
                .getGlanceIdBy(appWidgetId)
            updateAppWidgetState(this@WidgetConfigActivity, glanceId) {
                it.writeWidgetConfig(config)
            }
            SpooWidget().update(this@WidgetConfigActivity, glanceId)
            setResult(
                RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
            )
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConfigScreen(
    appWidgetId: Int,
    preset: WidgetConfig,
    onSave: (WidgetConfig) -> Unit,
) {
    var config by remember { mutableStateOf(preset) }

    // Reconfigure: prefill from the instance's stored choices, if any.
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(appWidgetId) {
        runCatching {
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
            if (WidgetKeys.STYLE in prefs) config = prefs.readWidgetConfig()
        }
        runCatching {
            val repo = SpooApp.graph.linksRepository
            if (repo.links.value.isEmpty()) repo.refresh()
        }
    }
    val links by SpooApp.graph.linksRepository.links.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Widget") },
                subtitle = { Text("Choose what this widget shows") },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Button(onClick = { onSave(config) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Save widget")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
        ) {
            item { SectionLabel("Style") }
            item {
                ToggleRow(
                    options = listOf(
                        Triple(WidgetStyle.Wave, "Wave", Icons.Outlined.ShowChart),
                        Triple(WidgetStyle.Bars, "Bars", Icons.Outlined.BarChart),
                        Triple(WidgetStyle.Number, "Number", Icons.Outlined.Numbers),
                    ),
                    selected = config.style,
                    onSelect = { config = config.copy(style = it) },
                )
            }
            item { SectionLabel("Metric") }
            item {
                ToggleRow(
                    options = listOf(
                        Triple(StatsMetric.Clicks, "Clicks", null),
                        Triple(StatsMetric.UniqueClicks, "Unique clicks", null),
                    ),
                    selected = config.metric,
                    onSelect = { config = config.copy(metric = it) },
                )
            }
            item { SectionLabel("Time range") }
            item {
                ToggleRow(
                    options = listOf(
                        Triple<Int?, String, ImageVector?>(7, "7d", null),
                        Triple<Int?, String, ImageVector?>(30, "30d", null),
                        Triple<Int?, String, ImageVector?>(90, "90d", null),
                        Triple<Int?, String, ImageVector?>(null, "All", null),
                    ),
                    selected = config.rangeDays,
                    onSelect = { config = config.copy(rangeDays = it) },
                )
            }
            item { SectionLabel("Scope") }
            item {
                ScopeRow(
                    selected = config.scope == null,
                    onClick = { config = config.copy(scope = null) },
                ) {
                    Text("All links", style = MaterialTheme.typography.bodyLarge)
                }
            }
            items(links.size) { i ->
                val link = links[i]
                ScopeRow(
                    selected = config.scope == link.shortCode,
                    onClick = { config = config.copy(scope = link.shortCode) },
                ) {
                    Favicon(host = faviconHost(link.originalUrl), size = 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "/${link.shortCode}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${link.totalClicks}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun <T> ToggleRow(
    options: List<Triple<T, String, ImageVector?>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, (value, label, icon) ->
            ToggleButton(
                checked = selected == value,
                onCheckedChange = { onSelect(value) },
                modifier = Modifier.weight(1f),
                shapes = when (i) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                }
                Text(label, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ScopeRow(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(14.dp))
        content()
    }
}
