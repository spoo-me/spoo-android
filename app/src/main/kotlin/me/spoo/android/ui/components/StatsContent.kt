package me.spoo.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.spoo.android.data.LinkStats
import me.spoo.android.data.StatsDim
import me.spoo.android.data.StatsParams
import me.spoo.android.ui.theme.cardChrome
import me.spoo.android.ui.theme.cardContainerColor
import me.spoo.android.ui.theme.hero
import me.spoo.android.ui.theme.railIconColors
import me.spoo.android.ui.theme.tabular
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val RANGES = listOf(7 to "7d", 30 to "30d", 90 to "90d", null to "All")
private const val TOP_N = 6

/**
 * The stats body shared by per-link and account-wide screens: hero count,
 * date-range switch (presets + custom range), wavy chart, activity grid,
 * and carded breakdowns (countries carry the choropleth). With
 * [filterable], rows toggle themselves as filters.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StatsContent(
    stats: LinkStats,
    params: StatsParams,
    onParamsChange: (StatsParams) -> Unit,
    contentPadding: PaddingValues,
    filterable: Boolean = true,
    onOpenFilters: (() -> Unit)? = null,
) {
    val numbers = NumberFormat.getIntegerInstance()
    var showRangePicker by remember { mutableStateOf(false) }

    val rangeLabel =
        params.customRange?.let { (from, to) ->
            val fmt = SimpleDateFormat("MMM d", Locale.US)
            "${fmt.format(Date(from))} – ${fmt.format(Date(to))}"
        } ?: when (params.days) {
            null -> "all time"
            else -> "past ${params.days} days"
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "hero") {
            Column {
                val total = stats.dailyClicks.sum()
                Text(
                    numbers.format(total),
                    style = MaterialTheme.typography.displayLarge.hero(key = total),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        buildString {
                            append("clicks · ")
                            append(rangeLabel)
                            if (params.filters.isNotEmpty()) append(" · filtered")
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (params.filters.isNotEmpty()) {
                        TextButton(onClick = { onParamsChange(params.copy(filters = emptyMap())) }) {
                            Text("Clear")
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RANGES.forEachIndexed { i, (days, label) ->
                        ToggleButton(
                            checked = params.customRange == null && params.days == days,
                            onCheckedChange = {
                                onParamsChange(params.copy(days = days, customRange = null))
                            },
                            shapes =
                                when (i) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                        ) { Text(label) }
                    }
                    ToggleButton(
                        checked = params.customRange != null,
                        onCheckedChange = { showRangePicker = true },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    ) {
                        Icon(
                            Icons.Outlined.DateRange,
                            contentDescription = "Custom date range",
                        )
                    }
                    if (onOpenFilters != null) {
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = onOpenFilters,
                            colors = railIconColors(active = params.filters.isNotEmpty()),
                        ) {
                            BadgedBox(
                                badge = {
                                    if (params.filters.isNotEmpty()) {
                                        Badge { Text("${params.filters.values.sumOf { it.size }}") }
                                    }
                                },
                            ) {
                                Icon(Icons.Outlined.FilterList, contentDescription = "Filters")
                            }
                        }
                    }
                }
            }
        }

        // The hero chart in the same card language as every other section,
        // tall enough to breathe — no orphan baselines.
        item(key = "chart") {
            StatsCard(title = "Clicks over time", fullBleed = true) {
                // All-zero data would draw a lone baseline: honest empty
                // text in the same body instead, height stable.
                if (stats.dailyClicks.sum() == 0) {
                    EmptyBody(height = 210.dp)
                } else {
                    WavyClicksChart(
                        dailyClicks = stats.dailyClicks,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(210.dp),
                    )
                }
            }
        }

        item(key = "browsers") {
            Breakdown(
                title = "Browsers",
                slices = stats.browsers,
                activeValues = params.filters[StatsDim.Browser].orEmpty(),
                labelFor = { it },
                icon = { BrandIcon(it) },
                onToggle =
                    if (filterable) {
                        { onParamsChange(params.toggling(StatsDim.Browser, it)) }
                    } else {
                        null
                    },
            )
        }
        item(key = "os") {
            Breakdown(
                title = "Operating systems",
                slices = stats.os,
                activeValues = params.filters[StatsDim.Os].orEmpty(),
                labelFor = { it },
                icon = { BrandIcon(it) },
                onToggle =
                    if (filterable) {
                        { onParamsChange(params.toggling(StatsDim.Os, it)) }
                    } else {
                        null
                    },
            )
        }
        item(key = "referrers") {
            Breakdown(
                title = "Referrers",
                slices = stats.referrers,
                activeValues = params.filters[StatsDim.Referrer].orEmpty(),
                labelFor = { it },
                icon = { value ->
                    if (value.contains('.')) Favicon(value) else Monogram(value)
                },
                onToggle =
                    if (filterable) {
                        { onParamsChange(params.toggling(StatsDim.Referrer, it)) }
                    } else {
                        null
                    },
            )
        }
        item(key = "devices") {
            Breakdown(
                title = "Devices",
                slices = stats.devices,
                activeValues = params.filters[StatsDim.Device].orEmpty(),
                labelFor = { it.replaceFirstChar(Char::uppercase) },
                icon = { DeviceIcon(it) },
                onToggle =
                    if (filterable) {
                        { onParamsChange(params.toggling(StatsDim.Device, it)) }
                    } else {
                        null
                    },
            )
        }
        item(key = "utm-sources") {
            Breakdown(
                title = "UTM sources",
                slices = stats.utmSources,
                activeValues = params.filters[StatsDim.UtmSource].orEmpty(),
                labelFor = { it },
                icon = { Monogram(it) },
                onToggle =
                    if (filterable) {
                        { onParamsChange(params.toggling(StatsDim.UtmSource, it)) }
                    } else {
                        null
                    },
            )
        }
        item(key = "countries") {
            Breakdown(
                title = "Countries",
                slices = stats.countries,
                activeValues = params.filters[StatsDim.Country].orEmpty(),
                labelFor = ::countryDisplayName,
                icon = { CountryFlag(it) },
                onToggle =
                    if (filterable) {
                        { onParamsChange(params.toggling(StatsDim.Country, it)) }
                    } else {
                        null
                    },
                header =
                    if (stats.countries.isNotEmpty()) {
                        {
                            WorldChoropleth(
                                countries = stats.countries.associate { it.label.lowercase() to it.count },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    } else {
                        null
                    },
            )
        }
    }

    if (showRangePicker) {
        FullScreenDateRangePicker(
            onDismiss = { showRangePicker = false },
            onApply = { from, to -> onParamsChange(params.copy(customRange = from to to)) },
        )
    }
}

fun StatsParams.toggling(
    dim: StatsDim,
    value: String,
): StatsParams {
    val current = filters[dim].orEmpty()
    val next = if (value in current) current - value else current + value
    return copy(filters = if (next.isEmpty()) filters - dim else filters + (dim to next))
}

/**
 * One framed section: quiet card, muted section label, content below.
 * [fullBleed] keeps the title inset but lets content consume the shell.
 */
@Composable
private fun StatsCard(
    title: String,
    fullBleed: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .cardChrome(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = cardContainerColor(),
    ) {
        Column(
            Modifier.padding(
                if (fullBleed) PaddingValues(top = 16.dp) else PaddingValues(16.dp),
            ),
        ) {
            Text(
                title,
                modifier = Modifier.padding(horizontal = if (fullBleed) 16.dp else 0.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * One empty-state grammar for every chart body: quiet centered muted
 * text at a stable height, so empty and loaded cards line up uniformly.
 */
@Composable
private fun EmptyBody(height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(height),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "No data in this range",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Breakdown(
    title: String,
    slices: List<LinkStats.Slice>,
    activeValues: Set<String>,
    labelFor: (String) -> String,
    icon: @Composable (String) -> Unit,
    onToggle: ((String) -> Unit)?,
    header: (@Composable () -> Unit)? = null,
) {
    val numbers = NumberFormat.getIntegerInstance()
    val top = slices.take(TOP_N)
    val max = top.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    StatsCard(title = title) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            header?.invoke()
            if (top.isEmpty()) {
                EmptyBody(height = 120.dp)
            }
            top.forEach { slice ->
                val active = slice.label in activeValues
                // Surface pairs onSecondaryContainer automatically; every
                // child must speak that pair, bar included (pairing law).
                Surface(
                    onClick = { onToggle?.invoke(slice.label) },
                    enabled = onToggle != null,
                    shape = MaterialTheme.shapes.medium,
                    color =
                        if (active) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            Color.Transparent
                        },
                ) {
                    val content = LocalContentColor.current
                    Row(
                        modifier =
                            Modifier.padding(
                                horizontal = if (active) 12.dp else 4.dp,
                                vertical = 8.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        icon(slice.label)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    labelFor(slice.label),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    numbers.format(slice.count),
                                    style = MaterialTheme.typography.labelLarge.tabular,
                                )
                            }
                            // Flat share bars, deliberately: the wave is the
                            // hero chart's signature — 24 squiggles per page
                            // would spend it (tried, rejected).
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (active) {
                                                content.copy(alpha = 0.15f) // state-layer alpha
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHigh
                                            },
                                        ),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth(slice.count / max.toFloat())
                                            .height(5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (active) content else MaterialTheme.colorScheme.primary,
                                            ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
