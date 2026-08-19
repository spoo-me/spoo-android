package me.spoo.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFloatingActionButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import me.spoo.android.data.SpooLink
import me.spoo.android.ui.components.CreateLinkSheet
import me.spoo.android.ui.screens.links.LinkSort
import me.spoo.android.ui.screens.links.LinksViewModel

/**
 * Home: link list with search + sort, create sheet via FAB, and the
 * share-target hero flow (ACTION_SEND text prefills the create sheet).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LinksScreen(
    sharedText: String?,
    onOpenStats: (String) -> Unit,
    onOpenAccount: () -> Unit,
    viewModel: LinksViewModel = viewModel(),
) {
    val links by viewModel.links.collectAsState()
    val query by viewModel.query.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val createState by viewModel.createState.collectAsState()

    // The share sheet is the hero flow: arriving with ACTION_SEND text goes
    // straight into the create sheet, prefilled.
    var showCreateSheet by rememberSaveable { mutableStateOf(sharedText != null) }
    val sharedUrl = sharedText?.let { Regex("""https?://\S+""").find(it)?.value ?: it }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val totalClicks = links.sumOf { it.totalClicks }
    val numbers = NumberFormat.getIntegerInstance()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("spoo.me") },
                subtitle = {
                    Text("${links.size} links · ${numbers.format(totalClicks)} clicks")
                },
                actions = {
                    IconButton(onClick = onOpenAccount) {
                        Icon(
                            Icons.Outlined.AccountCircle,
                            contentDescription = "Account",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            MediumFloatingActionButton(onClick = { showCreateSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Shorten a link")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.query.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    placeholder = { Text("Search links") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                )
            }
            item(key = "sort") {
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    ToggleButton(
                        checked = sort == LinkSort.Recent,
                        onCheckedChange = { viewModel.sort.value = LinkSort.Recent },
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                    ) { Text("Recent") }
                    ToggleButton(
                        checked = sort == LinkSort.Clicks,
                        onCheckedChange = { viewModel.sort.value = LinkSort.Clicks },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    ) { Text("Top clicks") }
                }
            }
            items(links, key = { it.shortCode }) { link ->
                LinkRow(link = link, onClick = { onOpenStats(link.shortCode) })
            }
            if (links.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = if (query.isBlank()) "No links yet" else "No links match \"$query\"",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateLinkSheet(
            initialUrl = sharedUrl.takeIf { sharedText != null },
            state = createState,
            onSubmit = viewModel::create,
            onDismiss = {
                showCreateSheet = false
                viewModel.resetCreate()
            },
        )
    }
}

@Composable
private fun LinkRow(
    link: SpooLink,
    onClick: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val numbers = NumberFormat.getIntegerInstance()

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = link.shortUrl,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (link.hasPassword) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = "Password protected",
                            modifier = Modifier.height(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = link.originalUrl.removePrefix("https://").removePrefix("http://"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${numbers.format(link.totalClicks)} clicks · ${link.createdLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = {
                clipboard.setText(AnnotatedString("https://${link.shortUrl}"))
            }) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = "Copy https://${link.shortUrl}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
