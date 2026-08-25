package me.spoo.android.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
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
import me.spoo.android.ui.components.StatsContent

/** Per-link stats: hero chart, choropleth, filterable breakdowns. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatsScreen(shortCode: String) {
    var params by remember { mutableStateOf(StatsParams()) }
    var stats by remember { mutableStateOf<LinkStats?>(null) }

    // Keep showing the previous result while a param change refetches.
    LaunchedEffect(shortCode, params) {
        stats = runCatching {
            SpooApp.graph.linksRepository.stats(shortCode, params)
        }.getOrNull() ?: stats
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text("/$shortCode") },
                subtitle = {
                    stats?.link?.let {
                        Text(
                            it.originalUrl.removePrefix("https://").removePrefix("http://"),
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
                filterable = false,
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 32.dp,
                ),
            )
        }
    }
}
