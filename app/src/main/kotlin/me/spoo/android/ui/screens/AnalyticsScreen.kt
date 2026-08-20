package me.spoo.android.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
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
import androidx.compose.ui.unit.dp
import me.spoo.android.SpooApp
import me.spoo.android.data.LinkStats
import me.spoo.android.data.StatsParams
import me.spoo.android.ui.components.StatsContent

/** Account-wide analytics tab — same vocabulary as the web dashboard. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnalyticsScreen() {
    var params by remember { mutableStateOf(StatsParams()) }
    var stats by remember { mutableStateOf<LinkStats?>(null) }

    LaunchedEffect(params) {
        stats = runCatching {
            SpooApp.graph.linksRepository.accountStats(params)
        }.getOrNull() ?: stats
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Analytics") },
                subtitle = { Text("All your links") },
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
                LinearWavyProgressIndicator()
            }
        } else {
            StatsContent(
                stats = loaded,
                params = params,
                onParamsChange = { params = it },
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
