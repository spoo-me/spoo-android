package me.spoo.android.widget

import androidx.datastore.preferences.core.mutablePreferencesOf
import me.spoo.android.data.StatsDim
import me.spoo.android.data.StatsMetric
import kotlin.test.Test
import kotlin.test.assertEquals

class WidgetConfigTest {
    @Test
    fun `config round-trips through glance state`() {
        val config =
            WidgetConfig(
                chart = WidgetChart.Treemap,
                font = WidgetFont.Serif,
                dimension = StatsDim.Referrer,
                metric = StatsMetric.UniqueClicks,
                scope = "mixtape",
                filters = mapOf(StatsDim.Country to "DE", StatsDim.Browser to "Firefox"),
                rangeDays = 90,
            )
        val prefs = mutablePreferencesOf()
        prefs.writeWidgetConfig(config)
        assertEquals(config, prefs.readWidgetConfig())
    }

    @Test
    fun `all-time range survives the zero encoding`() {
        val prefs = mutablePreferencesOf()
        prefs.writeWidgetConfig(WidgetConfig(rangeDays = null))
        assertEquals(null, prefs.readWidgetConfig().rangeDays)
    }

    @Test
    fun `empty state falls back to defaults, not crashes`() {
        assertEquals(WidgetConfig(), mutablePreferencesOf().readWidgetConfig())
    }

    @Test
    fun `unknown enum names fall back instead of crashing`() {
        val prefs = mutablePreferencesOf()
        prefs.writeWidgetConfig(WidgetConfig())
        prefs[WidgetKeys.STYLE] = "Outline"
        prefs[WidgetKeys.FONT] = "ComicSans"
        val read = prefs.readWidgetConfig()
        assertEquals(WidgetChart.Wave, read.chart)
        assertEquals(WidgetFont.Flex, read.font)
    }

    @Test
    fun `saving a config drops the previous scope's cached data`() {
        val prefs = mutablePreferencesOf()
        prefs.writeWidgetData(WidgetData(total = 5, series = listOf(1, 2), slices = emptyList()))
        prefs.writeWidgetConfig(WidgetConfig(scope = "other"))
        assertEquals(0L, prefs.readWidgetData().total)
        assertEquals(emptyList(), prefs.readWidgetData().series)
    }

    @Test
    fun `slices with tabs and newlines in labels round-trip`() {
        val data =
            WidgetData(
                total = 3,
                series = listOf(1, 1, 1),
                slices =
                    listOf(
                        me.spoo.android.data.LinkStats
                            .Slice("Samsung Internet", 2),
                        me.spoo.android.data.LinkStats
                            .Slice("(none)", 1),
                    ),
            )
        val prefs = mutablePreferencesOf()
        prefs.writeWidgetData(data)
        assertEquals(data.slices, prefs.readWidgetData().slices)
    }

    @Test
    fun `label carries scope, dimension, metric, range, and filter flag`() {
        val config =
            WidgetConfig(
                chart = WidgetChart.Bubbles,
                dimension = StatsDim.Browser,
                metric = StatsMetric.Clicks,
                scope = "mixtape",
                filters = mapOf(StatsDim.Country to "DE"),
                rangeDays = 7,
            )
        assertEquals("/mixtape · BROWSER · CLICKS · 7D · FILTERED", config.label)
        assertEquals("CLICKS · 30D", WidgetConfig().label)
    }

    @Test
    fun `picker shells prefill their chart`() {
        assertEquals(WidgetChart.Bars, WidgetConfig.presetFor("x.BarsWidgetReceiver").chart)
        assertEquals(WidgetChart.Number, WidgetConfig.presetFor("x.CountWidgetReceiver").chart)
        assertEquals(null, WidgetConfig.presetFor("x.CountWidgetReceiver").rangeDays)
        assertEquals(WidgetChart.Wave, WidgetConfig.presetFor(null).chart)
    }

    @Test
    fun `map chart always breaks down by country`() {
        val config = WidgetConfig(chart = WidgetChart.Map, dimension = StatsDim.Browser)
        assertEquals(StatsDim.Country, config.effectiveDimension)
    }
}
