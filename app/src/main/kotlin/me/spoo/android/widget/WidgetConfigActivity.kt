package me.spoo.android.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.spoo.android.SpooApp
import me.spoo.android.data.AppSettings
import me.spoo.android.data.LinkStats
import me.spoo.android.data.StatsDim
import me.spoo.android.data.StatsMetric
import me.spoo.android.ui.components.BrandIcon
import me.spoo.android.ui.components.CountryFlag
import me.spoo.android.ui.components.Favicon
import me.spoo.android.ui.components.Monogram
import me.spoo.android.ui.components.countryDisplayName
import me.spoo.android.ui.components.faviconHost
import me.spoo.android.ui.theme.SpooTheme
import me.spoo.android.ui.theme.spooColorScheme
import java.text.NumberFormat

/**
 * The widget builder: launched by the launcher when a shell is placed
 * (android:configure) and again on long-press edit (reconfigurable).
 * Saves into the instance's Glance state and triggers a render.
 */
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appWidgetId =
            intent?.getIntExtra(
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

        // Exported for the launcher, so any app can send an id; only
        // configure widgets that are actually ours.
        val provider =
            AppWidgetManager
                .getInstance(this)
                .getAppWidgetInfo(appWidgetId)
                ?.provider
        if (provider?.packageName != packageName) {
            finish()
            return
        }

        val preset = WidgetConfig.presetFor(provider.className)

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

    private fun save(
        appWidgetId: Int,
        config: WidgetConfig,
    ) {
        lifecycleScope.launch {
            val graph = SpooApp.graph
            val glanceId =
                GlanceAppWidgetManager(this@WidgetConfigActivity)
                    .getGlanceIdBy(appWidgetId)
            // Fetch here so the widget lands populated, not blank-then-fill.
            val data = fetchWidgetData(graph, config)
            if (data != null && !config.chart.timeChart) {
                WidgetIconCache.prefetch(
                    this@WidgetConfigActivity,
                    config.effectiveDimension,
                    data.slices
                        .sortedByDescending { it.count }
                        .take(9)
                        .map { it.label },
                )
            }
            val signedIn =
                graph.tokenStore.read() != null ||
                    graph.settingsRepository.settings
                        .first()
                        .mockData
            updateAppWidgetState(this@WidgetConfigActivity, glanceId) {
                it.writeWidgetConfig(config)
                it[WidgetKeys.SIGNED_IN] = signedIn
                data?.let(it::writeWidgetData)
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
    var stats by remember { mutableStateOf<LinkStats?>(null) }
    var saving by remember { mutableStateOf(false) }

    // Reconfigure: prefill from the instance's stored choices, if any.
    val context = LocalContext.current
    LaunchedEffect(appWidgetId) {
        runCatching {
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
            if (WidgetKeys.STYLE in prefs) config = prefs.readWidgetConfig()
        }
    }

    // Live data for the preview and the filter vocabularies; stale data
    // stays up while the next query is in flight. Icons for every dimension
    // are prefetched up front so switching chart/dimension never renders
    // monogram placeholders that a fetched favicon would replace.
    LaunchedEffect(config.metric, config.rangeDays, config.scope, config.filters) {
        val fetched = fetchWidgetStats(SpooApp.graph, config) ?: return@LaunchedEffect
        listOf(
            StatsDim.Browser to fetched.browsers,
            StatsDim.Os to fetched.os,
            StatsDim.Referrer to fetched.referrers,
        ).forEach { (dim, slices) ->
            WidgetIconCache.prefetch(
                context,
                dim,
                slices.sortedByDescending { it.count }.take(9).map { it.label },
            )
        }
        stats = fetched
    }
    val links by SpooApp.graph.linksRepository.links
        .collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text("Widget") },
                subtitle = { Text("Choose what this widget shows") },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Button(
                    onClick = {
                        saving = true
                        onSave(config)
                    },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (saving) {
                        LoadingIndicator(Modifier.height(24.dp))
                    } else {
                        Text("Save widget")
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
        ) {
            item { WidgetPreview(config, stats?.toWidgetData(config)) }
            item { SectionLabel("Chart") }
            item {
                ToggleRow(
                    options =
                        listOf(
                            Triple(WidgetChart.Wave, "Wave", Icons.Outlined.ShowChart),
                            Triple(WidgetChart.Bars, "Bars", Icons.Outlined.BarChart),
                            Triple(WidgetChart.Number, "Number", Icons.Outlined.Numbers),
                        ),
                    selected = config.chart,
                    onSelect = { config = config.copy(chart = it) },
                )
            }
            item { Spacer(Modifier.height(6.dp)) }
            item {
                ToggleRow(
                    options =
                        listOf(
                            Triple(WidgetChart.Treemap, "Treemap", Icons.Outlined.GridView),
                            Triple(WidgetChart.Bubbles, "Bubbles", Icons.Outlined.BubbleChart),
                            Triple(WidgetChart.Map, "Map", Icons.Outlined.Public),
                        ),
                    selected = config.chart,
                    onSelect = { config = config.copy(chart = it) },
                )
            }
            if (config.chart == WidgetChart.Number) {
                item { SectionLabel("Type") }
                item {
                    ToggleRow(
                        options =
                            listOf(
                                Triple<WidgetFont, String, ImageVector?>(WidgetFont.Flex, "Flex", null),
                                Triple<WidgetFont, String, ImageVector?>(WidgetFont.Serif, "Serif", null),
                                Triple<WidgetFont, String, ImageVector?>(WidgetFont.Mono, "Mono", null),
                            ),
                        selected = config.font,
                        onSelect = { config = config.copy(font = it) },
                    )
                }
            }
            if (config.chart == WidgetChart.Treemap || config.chart == WidgetChart.Bubbles) {
                item { SectionLabel("Dimension") }
                item {
                    ToggleRow(
                        options =
                            listOf(
                                Triple<StatsDim, String, ImageVector?>(StatsDim.Browser, "Browser", null),
                                Triple<StatsDim, String, ImageVector?>(StatsDim.Os, "OS", null),
                                Triple<StatsDim, String, ImageVector?>(StatsDim.Referrer, "Referrer", null),
                                Triple<StatsDim, String, ImageVector?>(StatsDim.Country, "Country", null),
                            ),
                        selected = config.dimension,
                        onSelect = { config = config.copy(dimension = it) },
                    )
                }
            }
            item { SectionLabel("Metric") }
            item {
                ToggleRow(
                    options =
                        listOf(
                            Triple<StatsMetric, String, ImageVector?>(StatsMetric.Clicks, "Clicks", null),
                            Triple<StatsMetric, String, ImageVector?>(StatsMetric.UniqueClicks, "Unique clicks", null),
                        ),
                    selected = config.metric,
                    onSelect = { config = config.copy(metric = it) },
                )
            }
            item { SectionLabel("Time range") }
            item {
                ToggleRow(
                    options =
                        listOf(
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
            item { SectionLabel("Filters") }
            item {
                FilterGroup(
                    "Country",
                    stats?.countries,
                    StatsDim.Country,
                    config,
                    onConfigChange = { config = it },
                    labelFor = ::countryDisplayName,
                    icon = { CountryFlag(it, size = 18.dp) },
                )
            }
            item {
                FilterGroup(
                    "Browser",
                    stats?.browsers,
                    StatsDim.Browser,
                    config,
                    onConfigChange = { config = it },
                    labelFor = { it },
                    icon = { BrandIcon(it, size = 18.dp) },
                )
            }
            item {
                FilterGroup(
                    "Operating system",
                    stats?.os,
                    StatsDim.Os,
                    config,
                    onConfigChange = { config = it },
                    labelFor = { it },
                    icon = { BrandIcon(it, size = 18.dp) },
                )
            }
            item {
                FilterGroup(
                    "Referrer",
                    stats?.referrers,
                    StatsDim.Referrer,
                    config,
                    onConfigChange = { config = it },
                    labelFor = { it },
                    icon = { value ->
                        if (value.contains('.')) Favicon(value, size = 18.dp) else Monogram(value, size = 18.dp)
                    },
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

/** The widget, verbatim: same renderer, same layout grammar, real data. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WidgetPreview(
    config: WidgetConfig,
    data: WidgetData?,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    // The widget's OWN scheme, not the app theme's: same palette pass the
    // launcher renders (soft tonal ground, system day/night), so the
    // preview background matches the homescreen exactly.
    val settings by SpooApp.graph.settingsRepository.settings
        .collectAsState(initial = AppSettings())
    val systemDark = isSystemInDarkTheme()
    val widgetScheme =
        remember(settings, systemDark) {
            spooColorScheme(context, settings, darkTheme = systemDark, cleanGround = false)
        }
    val palette =
        ChartPalette(
            accent = widgetScheme.primary.toArgb(),
            onSurface = widgetScheme.onSurface.toArgb(),
            onSurfaceVariant = widgetScheme.onSurfaceVariant.toArgb(),
            surface = widgetScheme.surface.toArgb(),
            surfaceVariant = widgetScheme.surfaceVariant.toArgb(),
            accentContainer = widgetScheme.primaryContainer.toArgb(),
        )

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .aspectRatio(1.85f) // a real 4x2 slot is taller than it looks
                .clip(RoundedCornerShape(24.dp))
                .background(widgetScheme.surface)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(24.dp),
                ),
    ) {
        val widthDp = maxWidth
        val heightDp = maxHeight
        val chartHeight = if (config.chart.timeChart) heightDp * 0.68f else heightDp
        val hasChart =
            data != null && config.chart != WidgetChart.Number &&
                (if (config.chart.timeChart) data.series.size >= 2 else data.slices.isNotEmpty())

        if (data == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
            return@BoxWithConstraints
        }
        if (hasChart) {
            val bitmap =
                remember(config, data, widthDp, chartHeight) {
                    WidgetChartRenderer.render(
                        context = context,
                        config = config,
                        data = data,
                        width = (widthDp.value * density).toInt(),
                        height = (chartHeight.value * density).toInt(),
                        density = density,
                        palette = palette,
                    )
                }
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                        .align(Alignment.BottomStart),
                contentScale = ContentScale.FillBounds,
            )
        }
        if (config.chart.timeChart) {
            Column(
                modifier =
                    Modifier
                        .padding(horizontal = 18.dp)
                        .padding(vertical = if (config.chart == WidgetChart.Number) 12.dp else 10.dp)
                        .let { if (config.chart == WidgetChart.Number) it.fillMaxSize() else it },
                verticalArrangement =
                    if (config.chart == WidgetChart.Number) {
                        Arrangement.Center
                    } else {
                        Arrangement.Top
                    },
            ) {
                Text(
                    config.label,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = widgetScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                // Same hero mask the real widget rasterizes, so the
                // preview is honest about the type.
                val label = NumberFormat.getIntegerInstance().format(data.total)
                val solo = config.chart == WidgetChart.Number
                val heroSp =
                    if (solo) {
                        72f
                    } else {
                        when {
                            label.length <= 7 -> 44f
                            label.length <= 10 -> 36f
                            else -> 28f
                        }
                    }
                val heroBitmap =
                    remember(label, heroSp, solo, widthDp, config.font) {
                        WidgetChartRenderer.renderHeroText(
                            context,
                            label,
                            heroSp * density,
                            emphatic = solo,
                            maxWidthPx = ((widthDp.value - 36f) * density).toInt(),
                            font = if (solo) config.font else WidgetFont.Flex,
                        )
                    }
                androidx.compose.foundation.Image(
                    bitmap = heroBitmap.asImageBitmap(),
                    contentDescription = label,
                    colorFilter =
                        androidx.compose.ui.graphics.ColorFilter.tint(
                            widgetScheme.onSurface,
                        ),
                )
            }
        } else if (!hasChart) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No data in this range",
                    style = MaterialTheme.typography.bodyMedium,
                    color = widgetScheme.onSurfaceVariant,
                )
            }
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
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, (value, label, icon) ->
            ToggleButton(
                checked = selected == value,
                onCheckedChange = { onSelect(value) },
                modifier = Modifier.weight(1f),
                shapes =
                    when (i) {
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
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
            Modifier
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(
    title: String,
    slices: List<LinkStats.Slice>?,
    dim: StatsDim,
    config: WidgetConfig,
    onConfigChange: (WidgetConfig) -> Unit,
    labelFor: (String) -> String,
    icon: @Composable (String) -> Unit,
) {
    val values = slices.orEmpty().take(6)
    if (values.isEmpty()) return
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { slice ->
                val selectedValue = config.filters[dim] == slice.label
                FilterChip(
                    selected = selectedValue,
                    onClick = {
                        val filters =
                            if (selectedValue) {
                                config.filters - dim
                            } else {
                                config.filters + (dim to slice.label)
                            }
                        onConfigChange(config.copy(filters = filters))
                    },
                    leadingIcon = { icon(slice.label) },
                    label = { Text(labelFor(slice.label)) },
                )
            }
        }
    }
}
