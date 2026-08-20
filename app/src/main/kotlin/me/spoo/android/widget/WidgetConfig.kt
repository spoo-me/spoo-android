package me.spoo.android.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import me.spoo.android.data.StatsMetric

enum class WidgetStyle { Wave, Bars, Number }

/**
 * One placed widget's identity, chosen in [WidgetConfigActivity] and stored
 * in that instance's Glance state. The manifest presets ("shells" in the
 * widget picker) are just different prefills of this.
 */
data class WidgetConfig(
    val style: WidgetStyle = WidgetStyle.Wave,
    val metric: StatsMetric = StatsMetric.Clicks,
    /** Short code of a single link, or null for all links. */
    val scope: String? = null,
    /** Look-back window in days; null = all time. */
    val rangeDays: Int? = 30,
) {
    val rangeLabel: String
        get() = rangeDays?.let { "${it}D" } ?: "ALL TIME"

    val metricLabel: String
        get() = when (metric) {
            StatsMetric.Clicks -> "CLICKS"
            StatsMetric.UniqueClicks -> "UNIQUE"
        }

    companion object {
        /** Picker shells: provider receiver class -> prefill. */
        fun presetFor(receiverClassName: String?) = when {
            receiverClassName?.endsWith("BarsWidgetReceiver") == true ->
                WidgetConfig(style = WidgetStyle.Bars, rangeDays = 7)
            receiverClassName?.endsWith("CountWidgetReceiver") == true ->
                WidgetConfig(style = WidgetStyle.Number, rangeDays = null)
            else -> WidgetConfig()
        }
    }
}

/** Glance-state keys: config + the per-instance data cache. */
internal object WidgetKeys {
    val STYLE = stringPreferencesKey("style")
    val METRIC = stringPreferencesKey("metric")
    val SCOPE = stringPreferencesKey("scope")
    val RANGE_DAYS = intPreferencesKey("range_days") // 0 = all time
    val CACHED_TOTAL = longPreferencesKey("cached_total")
    val CACHED_SERIES = stringPreferencesKey("cached_series")
}

internal fun Preferences.readWidgetConfig(): WidgetConfig = WidgetConfig(
    style = this[WidgetKeys.STYLE]
        ?.let { runCatching { WidgetStyle.valueOf(it) }.getOrNull() } ?: WidgetStyle.Wave,
    metric = this[WidgetKeys.METRIC]
        ?.let { runCatching { StatsMetric.valueOf(it) }.getOrNull() } ?: StatsMetric.Clicks,
    scope = this[WidgetKeys.SCOPE],
    rangeDays = this[WidgetKeys.RANGE_DAYS]?.takeIf { it > 0 }
        ?: if (WidgetKeys.RANGE_DAYS in this) null else 30,
)

internal fun MutablePreferences.writeWidgetConfig(config: WidgetConfig) {
    this[WidgetKeys.STYLE] = config.style.name
    this[WidgetKeys.METRIC] = config.metric.name
    config.scope?.let { this[WidgetKeys.SCOPE] = it } ?: remove(WidgetKeys.SCOPE)
    this[WidgetKeys.RANGE_DAYS] = config.rangeDays ?: 0
    // The cache belongs to the old scope; drop it so we never render
    // another query's numbers under this config's label.
    remove(WidgetKeys.CACHED_TOTAL)
    remove(WidgetKeys.CACHED_SERIES)
}
