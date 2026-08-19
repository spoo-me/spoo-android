package me.spoo.android.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import me.spoo.android.SpooApp
import me.spoo.android.data.LinkStats
import me.spoo.android.ui.components.WavyClicksChart

/**
 * Per-link stats: the design centerpiece. Hero wavy clicks-over-time chart,
 * then hand-rolled proportion breakdowns (geo, referrer, browser).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatsScreen(shortCode: String) {
    val stats by produceState<LinkStats?>(initialValue = null, shortCode) {
        value = runCatching { SpooApp.graph.linksRepository.stats(shortCode) }.getOrNull()
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val numbers = NumberFormat.getIntegerInstance()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("/$shortCode") },
                subtitle = {
                    stats?.let {
                        Text(
                            it.link.originalUrl.removePrefix("https://").removePrefix("http://"),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        val loaded = stats
        if (loaded == null) {
            // Progress IS the content here — the one sanctioned wavy-indicator spot.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                LinearWavyProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                item(key = "hero") {
                    Column {
                        Text(
                            numbers.format(loaded.dailyClicks.sum()),
                            style = MaterialTheme.typography.displayMedium,
                        )
                        Text(
                            "clicks · past 30 days",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(20.dp))
                        WavyClicksChart(
                            dailyClicks = loaded.dailyClicks,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                        )
                    }
                }
                item(key = "countries") { Breakdown("Countries", loaded.countries) }
                item(key = "referrers") { Breakdown("Referrers", loaded.referrers) }
                item(key = "browsers") { Breakdown("Browsers", loaded.browsers) }
            }
        }
    }
}

@Composable
private fun Breakdown(title: String, slices: List<LinkStats.Slice>) {
    val numbers = NumberFormat.getIntegerInstance()
    val max = slices.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        slices.forEach { slice ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        slice.label,
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
