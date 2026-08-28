package me.spoo.android.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import me.spoo.android.AppGraph
import me.spoo.android.data.AppSettings
import me.spoo.android.data.LinkStats
import me.spoo.android.data.StatsDim
import me.spoo.android.data.StatsMetric
import me.spoo.android.data.StatsParams
import me.spoo.android.data.ThemeMode

/**
 * Time charts carry the hero count; breakdown charts ARE the widget
 * (labels and counts live inside the cells).
 */
enum class WidgetChart(
    val timeChart: Boolean,
) {
    Wave(true),
    Bars(true),
    Number(true),
    Treemap(false),
    Bubbles(false),
    Map(false),
}

/**
 * Hero typeface for the solo Number widget — Google's expressive Roboto
 * trio. Only Number exposes it; chart overlays stay on the Flex house cut.
 * (A Cardfolio-style outlined-overflow cut was tried and rejected: digits
 * at card height fit ~3 per line, and clipped digits misread the count.)
 */
enum class WidgetFont { Flex, Serif, Mono }

/**
 * One placed widget's identity, chosen in [WidgetConfigActivity] and stored
 * in that instance's Glance state. The manifest presets ("shells" in the
 * widget picker) are just different prefills of this.
 */
data class WidgetConfig(
    val chart: WidgetChart = WidgetChart.Wave,
    val font: WidgetFont = WidgetFont.Flex,
    /** Breakdown dimension for treemap/bubbles; [WidgetChart.Map] is always Country. */
    val dimension: StatsDim = StatsDim.Browser,
    val metric: StatsMetric = StatsMetric.Clicks,
    /** Short code of a single link, or null for all links. */
    val scope: String? = null,
    /** Dimension-value filters, same vocabulary as the Analytics screen. */
    val filters: Map<StatsDim, String> = emptyMap(),
    /** Look-back window in days; null = all time. */
    val rangeDays: Int? = 30,
) {
    val effectiveDimension: StatsDim
        get() = if (chart == WidgetChart.Map) StatsDim.Country else dimension

    val rangeLabel: String
        get() = rangeDays?.let { "${it}D" } ?: "ALL TIME"

    val metricLabel: String
        get() =
            when (metric) {
                StatsMetric.Clicks -> "CLICKS"
                StatsMetric.UniqueClicks -> "UNIQUE"
            }

    /** The mono micro-label: everything the widget claims to show. */
    val label: String
        get() =
            listOfNotNull(
                scope?.let { "/$it" },
                if (chart.timeChart) null else effectiveDimension.name.uppercase(),
                metricLabel,
                rangeLabel,
                if (filters.isNotEmpty()) "FILTERED" else null,
            ).joinToString(" · ")

    fun toParams() =
        StatsParams(
            days = rangeDays,
            // Widget config stays single-select per dimension; the params
            // surface takes sets.
            filters = filters.mapValues { setOf(it.value) },
            metric = metric,
        )

    companion object {
        /** Picker shells: provider receiver class -> prefill. */
        fun presetFor(receiverClassName: String?) =
            when {
                receiverClassName?.endsWith("BarsWidgetReceiver") == true ->
                    WidgetConfig(chart = WidgetChart.Bars, rangeDays = 7)
                receiverClassName?.endsWith("CountWidgetReceiver") == true ->
                    WidgetConfig(chart = WidgetChart.Number, rangeDays = null)
                receiverClassName?.endsWith("TreemapWidgetReceiver") == true ->
                    WidgetConfig(chart = WidgetChart.Treemap, dimension = StatsDim.Browser)
                else -> WidgetConfig()
            }
    }
}

/** What one widget instance renders; cached per instance, stale over spinners. */
data class WidgetData(
    val total: Long,
    val series: List<Int>,
    val slices: List<LinkStats.Slice>,
) {
    fun encodeSlices() = slices.joinToString("\n") { "${it.label}\t${it.count}" }

    companion object {
        fun decodeSlices(raw: String?): List<LinkStats.Slice> =
            raw.orEmpty().split('\n').mapNotNull { line ->
                val tab = line.lastIndexOf('\t')
                if (tab <= 0) return@mapNotNull null
                val count = line.substring(tab + 1).toIntOrNull() ?: return@mapNotNull null
                LinkStats.Slice(line.substring(0, tab), count)
            }
    }
}

fun LinkStats.toWidgetData(config: WidgetConfig) =
    WidgetData(
        total = dailyClicks.sumOf { it.toLong() },
        series = dailyClicks,
        slices =
            when (config.effectiveDimension) {
                StatsDim.Country -> countries
                StatsDim.Browser -> browsers
                StatsDim.Os -> os
                StatsDim.Referrer -> referrers
                StatsDim.Device -> devices
                StatsDim.UtmSource -> utmSources
            },
    )

/** One fetch for whatever the config asks; null when the network says no. */
suspend fun fetchWidgetStats(
    graph: AppGraph,
    config: WidgetConfig,
): LinkStats? =
    runCatching {
        val repo = graph.linksRepository
        if (repo.links.value.isEmpty()) repo.refresh()
        config.scope
            ?.let { repo.stats(it, config.toParams()) }
            ?: repo.accountStats(config.toParams())
    }.getOrNull()

suspend fun fetchWidgetData(
    graph: AppGraph,
    config: WidgetConfig,
): WidgetData? = fetchWidgetStats(graph, config)?.toWidgetData(config)

/** Glance-state keys: config + the per-instance data cache. */
internal object WidgetKeys {
    val STYLE = stringPreferencesKey("style")
    val FONT = stringPreferencesKey("font")
    val DIMENSION = stringPreferencesKey("dimension")
    val METRIC = stringPreferencesKey("metric")
    val SCOPE = stringPreferencesKey("scope")
    val FILTERS = stringPreferencesKey("filters")
    val RANGE_DAYS = intPreferencesKey("range_days") // 0 = all time
    val SIGNED_IN = booleanPreferencesKey("signed_in")
    val CACHED_TOTAL = longPreferencesKey("cached_total")
    val CACHED_SERIES = stringPreferencesKey("cached_series")
    val CACHED_SLICES = stringPreferencesKey("cached_slices")

    // The app's theme choice, mirrored into every widget's state so a
    // warm session (which never re-runs provideGlance) can still recolor.
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val THEME_USE_DEVICE = booleanPreferencesKey("theme_use_device")
    val THEME_SEED = longPreferencesKey("theme_seed")
}

/** The theme trio as [AppSettings], for [spooColorScheme]-style builders. */
internal fun Preferences.readWidgetTheme(): AppSettings =
    AppSettings(
        themeMode =
            this[WidgetKeys.THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.System,
        useDeviceColors = this[WidgetKeys.THEME_USE_DEVICE] ?: true,
        seedColor = this[WidgetKeys.THEME_SEED] ?: AppSettings.DEFAULT_SEED,
    )

internal fun MutablePreferences.writeWidgetTheme(settings: AppSettings) {
    this[WidgetKeys.THEME_MODE] = settings.themeMode.name
    this[WidgetKeys.THEME_USE_DEVICE] = settings.useDeviceColors
    this[WidgetKeys.THEME_SEED] = settings.seedColor
}

internal fun Preferences.readWidgetConfig(): WidgetConfig =
    WidgetConfig(
        chart =
            this[WidgetKeys.STYLE]
                ?.let { runCatching { WidgetChart.valueOf(it) }.getOrNull() } ?: WidgetChart.Wave,
        font =
            this[WidgetKeys.FONT]
                ?.let { runCatching { WidgetFont.valueOf(it) }.getOrNull() } ?: WidgetFont.Flex,
        dimension =
            this[WidgetKeys.DIMENSION]
                ?.let { runCatching { StatsDim.valueOf(it) }.getOrNull() } ?: StatsDim.Browser,
        metric =
            this[WidgetKeys.METRIC]
                ?.let { runCatching { StatsMetric.valueOf(it) }.getOrNull() } ?: StatsMetric.Clicks,
        scope = this[WidgetKeys.SCOPE],
        filters =
            this[WidgetKeys.FILTERS]
                .orEmpty()
                .split('\n')
                .mapNotNull { line ->
                    val eq = line.indexOf('=')
                    if (eq <= 0) return@mapNotNull null
                    val dim =
                        runCatching { StatsDim.valueOf(line.substring(0, eq)) }.getOrNull()
                            ?: return@mapNotNull null
                    dim to line.substring(eq + 1)
                }.toMap(),
        rangeDays =
            this[WidgetKeys.RANGE_DAYS]?.takeIf { it > 0 }
                ?: if (WidgetKeys.RANGE_DAYS in this) null else 30,
    )

internal fun MutablePreferences.writeWidgetConfig(config: WidgetConfig) {
    this[WidgetKeys.STYLE] = config.chart.name
    this[WidgetKeys.FONT] = config.font.name
    this[WidgetKeys.DIMENSION] = config.dimension.name
    this[WidgetKeys.METRIC] = config.metric.name
    config.scope?.let { this[WidgetKeys.SCOPE] = it } ?: remove(WidgetKeys.SCOPE)
    if (config.filters.isEmpty()) {
        remove(WidgetKeys.FILTERS)
    } else {
        this[WidgetKeys.FILTERS] =
            config.filters.entries.joinToString("\n") { "${it.key.name}=${it.value}" }
    }
    this[WidgetKeys.RANGE_DAYS] = config.rangeDays ?: 0
    // The cache belongs to the old scope; drop it so we never render
    // another query's numbers under this config's label.
    remove(WidgetKeys.CACHED_TOTAL)
    remove(WidgetKeys.CACHED_SERIES)
    remove(WidgetKeys.CACHED_SLICES)
}

internal fun MutablePreferences.writeWidgetData(data: WidgetData) {
    this[WidgetKeys.CACHED_TOTAL] = data.total
    this[WidgetKeys.CACHED_SERIES] = data.series.joinToString(",")
    this[WidgetKeys.CACHED_SLICES] = data.encodeSlices()
}

internal fun Preferences.readWidgetData() =
    WidgetData(
        total = this[WidgetKeys.CACHED_TOTAL] ?: 0L,
        series =
            this[WidgetKeys.CACHED_SERIES]
                ?.split(',')
                ?.mapNotNull(String::toIntOrNull)
                .orEmpty(),
        slices = WidgetData.decodeSlices(this[WidgetKeys.CACHED_SLICES]),
    )
