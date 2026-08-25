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
import androidx.compose.material3.IconButton
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import me.spoo.android.data.LinkStats
import me.spoo.android.data.StatsDim
import me.spoo.android.data.StatsParams
import me.spoo.android.ui.theme.cardChrome
import me.spoo.android.ui.theme.cardContainerColor
import me.spoo.android.ui.theme.tabular

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

    val rangeLabel = params.customRange?.let { (from, to) ->
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
                Text(
                    numbers.format(stats.dailyClicks.sum()),
                    style = MaterialTheme.typography.displayMedium.tabular,
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
                            shapes = when (i) {
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
                        IconButton(onClick = onOpenFilters) {
                            BadgedBox(
                                badge = {
                                    if (params.filters.isNotEmpty()) {
                                        Badge { Text("${params.filters.size}") }
                                    }
                                },
                            ) {
                                Icon(Icons.Outlined.FilterList, contentDescription = "Filters")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                WavyClicksChart(
                    dailyClicks = stats.dailyClicks,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                )
            }
        }

        // A contribution grid needs a run of weeks to read as one;
        // short ranges skip it rather than render a lonely strip.
        if (stats.dailyClicks.size >= 56) {
            item(key = "activity") {
                StatsCard(title = "Daily activity") {
                    ClickHeatmap(
                        dailyClicks = stats.dailyClicks,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(92.dp),
                    )
                }
            }
        }

        item(key = "countries") {
            Breakdown(
                title = "Countries",
                slices = stats.countries,
                activeValue = params.filters[StatsDim.Country],
                labelFor = ::countryDisplayName,
                icon = { CountryFlag(it) },
                onToggle = if (filterable) {
                    { onParamsChange(params.toggling(StatsDim.Country, it)) }
                } else {
                    null
                },
                header = if (stats.countries.isNotEmpty()) {
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
        item(key = "browsers") {
            Breakdown(
                title = "Browsers",
                slices = stats.browsers,
                activeValue = params.filters[StatsDim.Browser],
                labelFor = { it },
                icon = { BrandIcon(it) },
                onToggle = if (filterable) {
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
                activeValue = params.filters[StatsDim.Os],
                labelFor = { it },
                icon = { BrandIcon(it) },
                onToggle = if (filterable) {
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
                activeValue = params.filters[StatsDim.Referrer],
                labelFor = { it },
                icon = { value ->
                    if (value.contains('.')) Favicon(value) else Monogram(value)
                },
                onToggle = if (filterable) {
                    { onParamsChange(params.toggling(StatsDim.Referrer, it)) }
                } else {
                    null
                },
            )
        }
    }

    if (showRangePicker) {
        val pickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val from = pickerState.selectedStartDateMillis
                        val to = pickerState.selectedEndDateMillis
                        if (from != null && to != null) {
                            onParamsChange(params.copy(customRange = from to to))
                        }
                        showRangePicker = false
                    },
                    enabled = pickerState.selectedEndDateMillis != null,
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showRangePicker = false }) { Text("Cancel") }
            },
        ) {
            DateRangePicker(state = pickerState, showModeToggle = false)
        }
    }
}

fun StatsParams.toggling(dim: StatsDim, value: String): StatsParams =
    copy(
        filters = if (filters[dim] == value) filters - dim else filters + (dim to value),
    )

/** One framed section: quiet card, muted section label, content below. */
@Composable
private fun StatsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.cardChrome(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = cardContainerColor(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun Breakdown(
    title: String,
    slices: List<LinkStats.Slice>,
    activeValue: String?,
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
                Text(
                    "No data in this range",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            top.forEach { slice ->
                val active = slice.label == activeValue
                Surface(
                    onClick = { onToggle?.invoke(slice.label) },
                    enabled = onToggle != null,
                    shape = MaterialTheme.shapes.medium,
                    color = if (active) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        Color.Transparent
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
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
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(slice.count / max.toFloat())
                                        .height(5.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
