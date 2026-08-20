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
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import me.spoo.android.data.LinkStats
import me.spoo.android.data.StatsDim
import me.spoo.android.data.StatsParams

private val RANGES = listOf(7 to "7d", 30 to "30d", 90 to "90d", null to "All")

/**
 * The stats body shared by per-link and account-wide screens: hero count,
 * date-range switch, wavy chart, choropleth, and breakdowns whose rows
 * toggle themselves as filters (mirroring the webapp's filter bar).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatsContent(
    stats: LinkStats,
    params: StatsParams,
    onParamsChange: (StatsParams) -> Unit,
    contentPadding: PaddingValues,
) {
    val numbers = NumberFormat.getIntegerInstance()
    val rangeLabel = when (params.days) {
        null -> "all time"
        else -> "past ${params.days} days"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item(key = "hero") {
            Column {
                Text(
                    numbers.format(stats.dailyClicks.sum()),
                    style = MaterialTheme.typography.displayMedium,
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
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    RANGES.forEachIndexed { i, (days, label) ->
                        ToggleButton(
                            checked = params.days == days,
                            onCheckedChange = { onParamsChange(params.copy(days = days)) },
                            shapes = when (i) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                RANGES.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                        ) { Text(label) }
                    }
                }
                Spacer(Modifier.height(20.dp))
                WavyClicksChart(
                    dailyClicks = stats.dailyClicks,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                )
            }
        }

        if (stats.countries.isNotEmpty()) {
            item(key = "map") {
                WorldChoropleth(
                    countries = stats.countries.associate { it.label.lowercase() to it.count },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item(key = "countries") {
            Breakdown(
                title = "Countries",
                slices = stats.countries,
                activeValue = params.filters[StatsDim.Country],
                labelFor = ::countryDisplayName,
                icon = { CountryFlag(it) },
                onToggle = { onParamsChange(params.toggling(StatsDim.Country, it)) },
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
                onToggle = { onParamsChange(params.toggling(StatsDim.Referrer, it)) },
            )
        }
        item(key = "browsers") {
            Breakdown(
                title = "Browsers",
                slices = stats.browsers,
                activeValue = params.filters[StatsDim.Browser],
                labelFor = { it },
                icon = { Monogram(it) },
                onToggle = { onParamsChange(params.toggling(StatsDim.Browser, it)) },
            )
        }
    }
}

private fun StatsParams.toggling(dim: StatsDim, value: String): StatsParams =
    copy(
        filters = if (filters[dim] == value) filters - dim else filters + (dim to value),
    )

@Composable
private fun Breakdown(
    title: String,
    slices: List<LinkStats.Slice>,
    activeValue: String?,
    labelFor: (String) -> String,
    icon: @Composable (String) -> Unit,
    onToggle: (String) -> Unit,
) {
    val numbers = NumberFormat.getIntegerInstance()
    val max = slices.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (slices.isEmpty()) {
            Text(
                "No data in this range",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        slices.forEach { slice ->
            val active = slice.label == activeValue
            Surface(
                onClick = { onToggle(slice.label) },
                shape = MaterialTheme.shapes.medium,
                color = if (active) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        icon(slice.label)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            labelFor(slice.label),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            numbers.format(slice.count),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(slice.count / max.toFloat())
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                        )
                    }
                }
            }
        }
    }
}
