package me.spoo.android.ui.screens

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.spoo.android.SpooApp
import me.spoo.android.data.LinkStats
import me.spoo.android.data.StatsParams
import me.spoo.android.ui.components.EmojiText
import me.spoo.android.ui.components.Favicon
import me.spoo.android.ui.components.StatsContent
import me.spoo.android.ui.components.StatsLoadFailure
import me.spoo.android.ui.components.StatsSkeleton
import me.spoo.android.ui.components.faviconHost

/** Per-link stats: hero chart, choropleth, filterable breakdowns. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun StatsScreen(
    shortCode: String,
    onBack: () -> Unit = {},
) {
    var params by remember { mutableStateOf(StatsParams()) }
    // Seed from the cache: revisits paint the last data instantly and
    // refetch silently underneath (stale-while-revalidate).
    var stats by remember {
        mutableStateOf(SpooApp.graph.linksRepository.cachedStats(shortCode, StatsParams()))
    }
    var loadFailed by remember { mutableStateOf(false) }
    var attempt by remember { mutableStateOf(0) }
    val snackbar = remember { SnackbarHostState() }

    // Keep showing the previous result while a param change refetches; a
    // failure with nothing on screen becomes a retry state, a failure over
    // stale data announces itself instead of silently lying.
    LaunchedEffect(shortCode, params, attempt) {
        loadFailed = false
        SpooApp.graph.linksRepository
            .cachedStats(shortCode, params)
            ?.let { stats = it }
        runCatching { SpooApp.graph.linksRepository.stats(shortCode, params) }
            .onSuccess { stats = it }
            .onFailure {
                if (stats == null) {
                    loadFailed = true
                } else {
                    snackbar.showSnackbar("Couldn't update stats")
                }
            }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            MediumFlexibleTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        stats?.link?.let {
                            Favicon(host = faviconHost(it.originalUrl), size = 24.dp)
                            Spacer(Modifier.width(10.dp))
                        }
                        EmojiText("/$shortCode")
                    }
                },
                subtitle = {
                    stats?.link?.let {
                        Text(
                            it.originalUrl.removePrefix("https://").removePrefix("http://"),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
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
                // Shaped like the destination, so nothing jumps.
                StatsSkeleton(
                    contentPadding =
                        PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = padding.calculateTopPadding() + 8.dp,
                            bottom = padding.calculateBottomPadding(),
                        ),
                )
            }
        } else {
            StatsContent(
                stats = loaded,
                params = params,
                onParamsChange = { params = it },
                filterable = false,
                contentPadding =
                    PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = padding.calculateBottomPadding() + 32.dp,
                    ),
            )
        }
    }
}
