package me.spoo.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import me.spoo.android.SpooApp
import me.spoo.android.data.LinkStats
import me.spoo.android.data.StatsDim
import me.spoo.android.data.StatsParams
import me.spoo.android.ui.components.BottomFade
import me.spoo.android.ui.components.BrandIcon
import me.spoo.android.ui.components.CountryFlag
import me.spoo.android.ui.components.Favicon
import me.spoo.android.ui.components.Monogram
import me.spoo.android.ui.components.StatsContent
import me.spoo.android.ui.components.countryDisplayName
import me.spoo.android.ui.components.toggling

/** Account-wide analytics tab — same vocabulary as the web dashboard. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnalyticsScreen() {
    var params by remember { mutableStateOf(StatsParams()) }
    var stats by remember { mutableStateOf<LinkStats?>(null) }
    // The filter menu's vocabulary: same window, NO filters — otherwise
    // picking one value collapses the menu to just that value.
    var vocab by remember { mutableStateOf<LinkStats?>(null) }

    LaunchedEffect(params) {
        stats = runCatching {
            SpooApp.graph.linksRepository.accountStats(params)
        }.getOrNull() ?: stats
    }
    LaunchedEffect(params.days, params.customRange) {
        vocab = runCatching {
            SpooApp.graph.linksRepository.accountStats(params.copy(filters = emptyMap()))
        }.getOrNull() ?: vocab
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text("Analytics") },
                subtitle = { Text("All your links") },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            val loaded = stats
            if (loaded == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator(modifier = Modifier.size(56.dp))
                }
            } else {
                StatsContent(
                    stats = loaded,
                    params = params,
                    onParamsChange = { params = it },
                    onOpenFilters = { showFilters = true },
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = padding.calculateBottomPadding() + 32.dp,
                    ),
                )
            }
            BottomFade()
        }
    }

    if (showFilters) {
        FilterSheet(
            stats = vocab ?: stats,
            params = params,
            onParamsChange = { params = it },
            onDismiss = { showFilters = false },
        )
    }
}

/** One single-select chip row per dimension, values from the current data. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    stats: LinkStats?,
    params: StatsParams,
    onParamsChange: (StatsParams) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Filters",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (params.filters.isNotEmpty()) {
                    TextButton(onClick = { onParamsChange(params.copy(filters = emptyMap())) }) {
                        Text("Clear all")
                    }
                }
            }
            FilterGroup(
                "Country", stats?.countries, StatsDim.Country, params, onParamsChange,
                labelFor = ::countryDisplayName,
                icon = { CountryFlag(it, size = 18.dp) },
            )
            FilterGroup(
                "Browser", stats?.browsers, StatsDim.Browser, params, onParamsChange,
                labelFor = { it },
                icon = { BrandIcon(it, size = 18.dp) },
            )
            FilterGroup(
                "Operating system", stats?.os, StatsDim.Os, params, onParamsChange,
                labelFor = { it },
                icon = { BrandIcon(it, size = 18.dp) },
            )
            FilterGroup(
                "Referrer", stats?.referrers, StatsDim.Referrer, params, onParamsChange,
                labelFor = { it },
                icon = { value ->
                    if (value.contains('.')) Favicon(value, size = 18.dp) else Monogram(value, size = 18.dp)
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(
    title: String,
    slices: List<LinkStats.Slice>?,
    dim: StatsDim,
    params: StatsParams,
    onParamsChange: (StatsParams) -> Unit,
    labelFor: (String) -> String,
    icon: @Composable (String) -> Unit,
) {
    val values = slices.orEmpty().take(8)
    if (values.isEmpty()) return
    Spacer(Modifier.height(16.dp))
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { slice ->
            val selected = slice.label in params.filters[dim].orEmpty()
            FilterChip(
                selected = selected,
                onClick = { onParamsChange(params.toggling(dim, slice.label)) },
                leadingIcon = { icon(slice.label) },
                label = { Text(labelFor(slice.label)) },
            )
        }
    }
}
