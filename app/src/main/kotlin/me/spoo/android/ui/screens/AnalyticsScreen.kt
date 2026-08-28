package me.spoo.android.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.spoo.android.SpooApp
import me.spoo.android.data.LinkStats
import me.spoo.android.data.StatsDim
import me.spoo.android.data.StatsParams
import me.spoo.android.ui.components.BottomFade
import me.spoo.android.ui.components.BrandIcon
import me.spoo.android.ui.components.CountryFlag
import me.spoo.android.ui.components.DeviceIcon
import me.spoo.android.ui.components.Favicon
import me.spoo.android.ui.components.Monogram
import me.spoo.android.ui.components.StatsContent
import me.spoo.android.ui.components.StatsLoadFailure
import me.spoo.android.ui.components.StatsSkeleton
import me.spoo.android.ui.components.countryDisplayName
import me.spoo.android.ui.components.sheetBottomPadding
import me.spoo.android.ui.components.toggling
import me.spoo.android.ui.screens.links.MIN_REFRESH_MS
import me.spoo.android.ui.theme.loaderContainerColor

/** Account-wide analytics tab — same vocabulary as the web dashboard. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnalyticsScreen() {
    var params by remember { mutableStateOf(StatsParams()) }
    // Seed from the cache so tab revisits paint instantly (see StatsScreen).
    var stats by remember {
        mutableStateOf(SpooApp.graph.linksRepository.cachedStats(null, StatsParams()))
    }
    var loadFailed by remember { mutableStateOf(false) }
    var attempt by remember { mutableStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    // The filter menu's vocabulary: same window, NO filters — otherwise
    // picking one value collapses the menu to just that value.
    var vocab by remember { mutableStateOf<LinkStats?>(null) }

    LaunchedEffect(params, attempt) {
        loadFailed = false
        SpooApp.graph.linksRepository
            .cachedStats(null, params)
            ?.let { stats = it }
        runCatching { SpooApp.graph.linksRepository.accountStats(params) }
            .onSuccess { stats = it }
            .onFailure {
                if (stats == null) {
                    loadFailed = true
                } else {
                    snackbar.showSnackbar("Couldn't update stats")
                }
            }
    }
    LaunchedEffect(params.days, params.customRange, attempt) {
        // Menu vocabulary only: the sheet falls back to the filtered data.
        vocab = runCatching {
            SpooApp.graph.linksRepository.accountStats(params.copy(filters = emptyMap()))
        }.getOrNull() ?: vocab
    }

    var showFilters by remember { mutableStateOf(false) }

    // Pull-to-refresh over the whole screen, same grammar as Links.
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pullState = rememberPullToRefreshState()
    val pullThreshold =
        with(LocalDensity.current) {
            PullToRefreshDefaults.PositionalThreshold.toPx()
        }
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                val started = System.currentTimeMillis()
                runCatching { SpooApp.graph.linksRepository.accountStats(params) }
                    .onSuccess { stats = it }
                    .onFailure { snackbar.showSnackbar("Couldn't refresh") }
                // Same floor as Links: a sub-frame refresh reads as a snap.
                delay((MIN_REFRESH_MS - (System.currentTimeMillis() - started)).coerceAtLeast(0))
                refreshing = false
            }
        },
        state = pullState,
        // Surface-painted: the strip revealed by the pull must match the
        // content, not the window background (accidental duotone).
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        indicator = {
            ContainedLoadingIndicator(
                containerColor = loaderContainerColor(),
                indicatorColor = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 24.dp)
                        .size(56.dp)
                        .graphicsLayer {
                            val progress = pullState.distanceFraction.coerceIn(0f, 1f)
                            scaleX = progress
                            scaleY = progress
                            alpha = progress
                        },
            )
        },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = pullState.distanceFraction * pullThreshold },
        ) {
            // No page header: the hero count leads, content starts at the top.
            Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
                Box(Modifier.fillMaxSize()) {
                    val loaded = stats
                    if (loaded == null) {
                        if (loadFailed) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .padding(padding),
                                contentAlignment = Alignment.Center,
                            ) {
                                StatsLoadFailure(onRetry = { attempt++ })
                            }
                        } else {
                            StatsSkeleton(
                                contentPadding =
                                    PaddingValues(
                                        start = 20.dp,
                                        end = 20.dp,
                                        top = padding.calculateTopPadding() + 28.dp,
                                        bottom = padding.calculateBottomPadding(),
                                    ),
                            )
                        }
                    } else {
                        StatsContent(
                            stats = loaded,
                            params = params,
                            onParamsChange = { params = it },
                            onOpenFilters = { showFilters = true },
                            contentPadding =
                                PaddingValues(
                                    start = 20.dp,
                                    end = 20.dp,
                                    top = padding.calculateTopPadding() + 28.dp,
                                    bottom = padding.calculateBottomPadding() + 32.dp,
                                ),
                        )
                    }
                    BottomFade()
                }
            }
        } // pull-translation box
    } // PullToRefreshBox

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
            modifier =
                Modifier
                    .padding(horizontal = 24.dp)
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
                "Country",
                stats?.countries,
                StatsDim.Country,
                params,
                onParamsChange,
                labelFor = ::countryDisplayName,
                icon = { CountryFlag(it, size = 18.dp) },
            )
            FilterGroup(
                "Browser",
                stats?.browsers,
                StatsDim.Browser,
                params,
                onParamsChange,
                labelFor = { it },
                icon = { BrandIcon(it, size = 18.dp) },
            )
            FilterGroup(
                "Operating system",
                stats?.os,
                StatsDim.Os,
                params,
                onParamsChange,
                labelFor = { it },
                icon = { BrandIcon(it, size = 18.dp) },
            )
            FilterGroup(
                "Referrer",
                stats?.referrers,
                StatsDim.Referrer,
                params,
                onParamsChange,
                labelFor = { it },
                icon = { value ->
                    if (value.contains('.')) Favicon(value, size = 18.dp) else Monogram(value, size = 18.dp)
                },
            )
            FilterGroup(
                "Device",
                stats?.devices,
                StatsDim.Device,
                params,
                onParamsChange,
                labelFor = { it.replaceFirstChar(Char::uppercase) },
                icon = { DeviceIcon(it, size = 18.dp) },
            )
            FilterGroup(
                "UTM source",
                stats?.utmSources,
                StatsDim.UtmSource,
                params,
                onParamsChange,
                labelFor = { it },
                icon = { Monogram(it, size = 18.dp) },
            )
            Spacer(Modifier.height(sheetBottomPadding()))
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
