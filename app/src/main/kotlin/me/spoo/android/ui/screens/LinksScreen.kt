package me.spoo.android.ui.screens

import android.content.Intent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.AdsClick
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.toShape
import androidx.compose.material3.MediumFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import me.spoo.android.R
import me.spoo.android.data.LinkUiStatus
import me.spoo.android.data.LinksFilter
import me.spoo.android.data.SpooLink
import me.spoo.android.data.SwipeAction
import me.spoo.android.ui.components.BottomFade
import me.spoo.android.ui.theme.loaderContainerColor
import me.spoo.android.ui.theme.railIconColors
import me.spoo.android.ui.components.CreateLinkSheet
import me.spoo.android.ui.components.EmojiText
import me.spoo.android.ui.components.FullScreenDateRangePicker
import me.spoo.android.ui.components.EditLinkSheet
import me.spoo.android.ui.components.Favicon
import me.spoo.android.ui.components.QrDialog
import me.spoo.android.ui.components.faviconHost
import me.spoo.android.ui.components.sheetBottomPadding
import me.spoo.android.data.LinkSort
import me.spoo.android.ui.screens.links.LinksViewModel
import me.spoo.android.ui.theme.cardChrome
import me.spoo.android.ui.theme.cardContainerColor
import me.spoo.android.ui.theme.hero
import me.spoo.android.ui.theme.tabular

/**
 * Home: link list with search + sort, favicons, create sheet via FAB,
 * long-press multi-select with bulk delete, and the share-target hero flow
 * (ACTION_SEND, QS tile, and the shortcut land here via [prefillText]/
 * [startInCreate]).
 */
@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
)
@Composable
fun LinksScreen(
    prefillText: String?,
    startInCreate: Boolean,
    showShareInMenu: Boolean,
    swipeRight: SwipeAction = SwipeAction.Edit,
    swipeLeft: SwipeAction = SwipeAction.Delete,
    onOpenStats: (String) -> Unit,
    onOpenSettings: () -> Unit = {},
    viewModel: LinksViewModel = viewModel(),
) {
    val links by viewModel.links.collectAsState()
    val query by viewModel.query.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val createState by viewModel.createState.collectAsState()
    val emojiCatalog by viewModel.emojiCatalog.collectAsState()
    val editState by viewModel.editState.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val selection by viewModel.selection.collectAsState()
    val filter by viewModel.filter.collectAsState()
    var showLinkFilters by remember { mutableStateOf(false) }
    var showCreatedPicker by remember { mutableStateOf(false) }

    var showCreateSheet by rememberSaveable { mutableStateOf(startInCreate) }
    val sharedUrl = prefillText?.let { Regex("""https?://\S+""").find(it)?.value ?: it }

    var qrFor by remember { mutableStateOf<SpooLink?>(null) }
    var editFor by remember { mutableStateOf<SpooLink?>(null) }
    var deleteFor by remember { mutableStateOf<SpooLink?>(null) }
    var confirmBulkDelete by remember { mutableStateOf(false) }
    var showExpiryPicker by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.actionMessage.value = null
        }
    }

    val selecting = selection.isNotEmpty()

    // Pull-to-refresh owns the WHOLE screen: the entire UI (app bar
    // included) rides down with the pull and springs back, the loader
    // grows above it all — the M3 behavior, not a list-only shimmy.
    val refreshing by viewModel.refreshing.collectAsState()
    val loadingMore by viewModel.loadingMore.collectAsState()
    val pullState = rememberPullToRefreshState()
    val pullThreshold = with(LocalDensity.current) {
        PullToRefreshDefaults.PositionalThreshold.toPx()
    }
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = viewModel::refresh,
        state = pullState,
        // Surface-painted: the strip revealed by the pull must match the
        // content, not the window background (accidental duotone).
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        indicator = {
            // The doc's look at every phase: the contained disc with the
            // dark morphing shape, growing with the pull. The stock pull
            // indicator draws a hollow ring mid-pull instead.
            ContainedLoadingIndicator(
                containerColor = loaderContainerColor(),
                indicatorColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
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
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        // No app bar announcing spoo.me — the hero metric IS the header.
        // Selection mode keeps its contextual bar.
        topBar = {
            if (selecting) {
                TopAppBar(
                    title = { Text("${selection.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear selection")
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            if (!selecting) {
                MediumFloatingActionButton(onClick = { showCreateSheet = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Shorten a link")
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
        val listState = rememberLazyListState()
        // Auto-pagination: ask for the next page as the end scrolls near.
        LaunchedEffect(listState) {
            snapshotFlow {
                val info = listState.layoutInfo
                val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                last >= info.totalItemsCount - 6
            }
                .distinctUntilChanged()
                .collect { nearEnd -> if (nearEnd) viewModel.loadMore() }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 20.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "search") {
                // The M3E scaffold header: search pill + circular identity
                // chip (brand ghost -> Settings, where the account lives).
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Filled, not outlined: quiet chrome over hairline chrome.
                    TextField(
                        value = query,
                        onValueChange = { viewModel.query.value = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search links") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        shape = CircleShape,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                    )
                    Spacer(Modifier.width(12.dp))
                    val darkGlyph = MaterialTheme.colorScheme.surface.luminance() > 0.5f
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable(onClick = onOpenSettings),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(
                                if (darkGlyph) R.drawable.logo_black else R.drawable.logo_white,
                            ),
                            contentDescription = "Account",
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            item(key = "sort") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
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
                    Spacer(Modifier.weight(1f))
                    // Bare at rest; a secondaryContainer tint appears only
                    // while a filter is active (state via affordance, no
                    // naked dot badges).
                    IconButton(
                        onClick = { showLinkFilters = true },
                        colors = railIconColors(active = filter.count > 0),
                    ) {
                        BadgedBox(
                            badge = {
                                if (filter.count > 0) Badge { Text("${filter.count}") }
                            },
                        ) {
                            Icon(
                                Icons.Outlined.FilterList,
                                contentDescription = "Filters",
                                // Mirrored: reads as refinement, not a menu.
                                modifier = Modifier.scale(scaleX = -1f, scaleY = 1f),
                            )
                        }
                    }
                    IconButton(
                        onClick = { showCreatedPicker = true },
                        colors = railIconColors(active = filter.createdRange != null),
                    ) {
                        Icon(
                            Icons.Outlined.DateRange,
                            contentDescription = "Created date range",
                        )
                    }
                }
            }
            items(links, key = { it.id }) { link ->
                val clipboard = LocalClipboardManager.current
                val context = LocalContext.current
                val runSwipeAction: (SwipeAction) -> Unit = { action ->
                    when (action) {
                        SwipeAction.Copy -> {
                            clipboard.setText(AnnotatedString("https://${link.shortUrl}"))
                            viewModel.actionMessage.value = "Copied ${link.shortUrl}"
                        }
                        SwipeAction.Share -> {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "https://${link.shortUrl}")
                            }
                            context.startActivity(Intent.createChooser(send, null))
                        }
                        SwipeAction.Edit -> editFor = link
                        SwipeAction.Qr -> qrFor = link
                        SwipeAction.Delete -> deleteFor = link
                        SwipeAction.None -> Unit
                    }
                }
                SwipeableLinkCard(
                    swipeRight = swipeRight,
                    swipeLeft = swipeLeft,
                    enabled = !selecting,
                    onAction = runSwipeAction,
                ) {
                    LinkRow(
                        link = link,
                        selecting = selecting,
                        selected = link.id in selection,
                        showShare = showShareInMenu,
                        onClick = {
                            if (selecting) viewModel.toggleSelected(link.id) else onOpenStats(link.shortCode)
                        },
                        onLongClick = { viewModel.toggleSelected(link.id) },
                        onQr = { qrFor = link },
                        onEdit = { editFor = link },
                        onDelete = { deleteFor = link },
                    )
                }
            }
            if (links.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = if (query.isBlank()) "No links yet" else "No links match \"$query\"",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (loadingMore) {
                item(key = "loading-more") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Bare morphing shape: inline with content, the
                        // contained disc reads too heavy.
                        LoadingIndicator(
                            modifier = Modifier.size(40.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        BottomFade()

        // The webapp's floating bulk-action bar, in its M3E form.
        if (selecting) {
            HorizontalFloatingToolbar(
                expanded = true,
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = padding.calculateBottomPadding() + 20.dp),
            ) {
                IconButton(onClick = { viewModel.setSelectedStatus(true) }) {
                    Icon(Icons.Outlined.Link, contentDescription = "Enable selected")
                }
                IconButton(onClick = { viewModel.setSelectedStatus(false) }) {
                    Icon(Icons.Outlined.LinkOff, contentDescription = "Disable selected")
                }
                IconButton(onClick = { showExpiryPicker = true }) {
                    Icon(Icons.Outlined.Timer, contentDescription = "Set expiry for selected")
                }
                IconButton(onClick = { confirmBulkDelete = true }) {
                    // Inherits the toolbar pair — error red clashes on the
                    // vibrant container, and the confirm dialog carries the
                    // destructive weight anyway.
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete selected")
                }
            }
        }
        }
    }
    } // pull-translation box
    } // PullToRefreshBox

    if (showCreateSheet) {
        CreateLinkSheet(
            initialUrl = sharedUrl,
            state = createState,
            emojiCatalog = emojiCatalog,
            onEmojiMode = viewModel::ensureEmojiCatalog,
            onSubmit = viewModel::create,
            onDismiss = {
                showCreateSheet = false
                viewModel.resetCreate()
            },
        )
    }

    qrFor?.let { link ->
        QrDialog(shortUrl = link.shortUrl, onDismiss = { qrFor = null })
    }

    editFor?.let { link ->
        EditLinkSheet(
            link = link,
            state = editState,
            onSubmit = { edit -> viewModel.updateLink(link.id, edit) },
            onDismiss = {
                editFor = null
                viewModel.resetEdit()
            },
        )
    }

    deleteFor?.let { link ->
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            title = { Text("Delete ${link.shortUrl}?") },
            text = { Text("The short link stops working immediately. Its stats are deleted with it.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteLink(link.id)
                    deleteFor = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteFor = null }) { Text("Cancel") }
            },
        )
    }

    if (showLinkFilters) {
        LinksFilterSheet(
            filter = filter,
            onFilterChange = { viewModel.filter.value = it },
            onDismiss = { showLinkFilters = false },
        )
    }

    if (showCreatedPicker) {
        FullScreenDateRangePicker(
            onDismiss = { showCreatedPicker = false },
            onApply = { from, to ->
                viewModel.filter.value = filter.copy(createdRange = from to to)
            },
            neutralLabel = "All time",
            onNeutral = { viewModel.filter.value = filter.copy(createdRange = null) },
        )
    }

    if (showExpiryPicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showExpiryPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setSelectedExpiry(pickerState.selectedDateMillis)
                        showExpiryPicker = false
                    },
                    enabled = pickerState.selectedDateMillis != null,
                ) { Text("Set expiry") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.setSelectedExpiry(null)
                    showExpiryPicker = false
                }) { Text("Clear expiry") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (confirmBulkDelete) {
        AlertDialog(
            onDismissRequest = { confirmBulkDelete = false },
            title = { Text("Delete ${selection.size} links?") },
            text = { Text("The short links stop working immediately. Their stats are deleted with them.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSelected()
                    confirmBulkDelete = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmBulkDelete = false }) { Text("Cancel") }
            },
        )
    }
}

/** Status + protections + clear, mirroring the webapp's links filters. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LinksFilterSheet(
    filter: LinksFilter,
    onFilterChange: (LinksFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Filters",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (filter.count > 0) {
                    TextButton(onClick = { onFilterChange(LinksFilter()) }) {
                        Text("Clear all")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Status",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LinkUiStatus.entries.forEach { status ->
                    FilterChip(
                        selected = filter.status == status,
                        onClick = {
                            onFilterChange(
                                filter.copy(status = if (filter.status == status) null else status),
                            )
                        },
                        leadingIcon = {
                            // Sized like the icon chips so the slot's 8dp
                            // start inset reads balanced, not hollow.
                            Box(
                                Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(4.5.dp))
                                    .background(status.dotColor()),
                            )
                        },
                        label = { Text(status.name) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Protections",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter.passwordProtected,
                    onClick = {
                        onFilterChange(filter.copy(passwordProtected = !filter.passwordProtected))
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    label = { Text("Password protected") },
                )
                FilterChip(
                    selected = filter.clickLimited,
                    onClick = { onFilterChange(filter.copy(clickLimited = !filter.clickLimited)) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    label = { Text("Click-limited") },
                )
            }
            Spacer(Modifier.height(8.dp + sheetBottomPadding()))
        }
    }
}

/**
 * Horizontal swipe on a card runs a user-mapped action (Settings ->
 * Behavior). The card always settles back — sheets and dialogs carry
 * the action itself, so nothing is destructive from the gesture alone.
 */
@Composable
private fun SwipeableLinkCard(
    swipeRight: SwipeAction,
    swipeLeft: SwipeAction,
    enabled: Boolean,
    onAction: (SwipeAction) -> Unit,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> onAction(swipeRight)
                SwipeToDismissBoxValue.EndToStart -> onAction(swipeLeft)
                else -> Unit
            }
            false // settle back, never dismiss
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = enabled && swipeRight != SwipeAction.None,
        enableDismissFromEndToStart = enabled && swipeLeft != SwipeAction.None,
        backgroundContent = {
            val action = when (state.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> swipeRight
                SwipeToDismissBoxValue.EndToStart -> swipeLeft
                else -> SwipeAction.None
            }
            if (action != SwipeAction.None) {
                val (container, onContainer, glyph) = swipeVisual(action)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.large)
                        .background(container)
                        .padding(horizontal = 24.dp),
                    contentAlignment = if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                        Alignment.CenterStart
                    } else {
                        Alignment.CenterEnd
                    },
                ) {
                    Icon(glyph, contentDescription = action.label, tint = onContainer)
                }
            }
        },
        content = {
            // The M3 interaction lift: elevation appears only while the
            // card is being swiped — the one sanctioned shadow.
            val swiping = state.dismissDirection != SwipeToDismissBoxValue.Settled
            val lift by animateDpAsState(
                targetValue = if (swiping) 10.dp else 0.dp,
                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                label = "swipeLift",
            )
            Box(Modifier.shadow(lift, MaterialTheme.shapes.large)) { content() }
        },
    )
}

/** Container/on-container pairs per the color-role doctrine. */
@Composable
private fun swipeVisual(action: SwipeAction): Triple<Color, Color, androidx.compose.ui.graphics.vector.ImageVector> {
    val scheme = MaterialTheme.colorScheme
    return when (action) {
        SwipeAction.Delete -> Triple(scheme.errorContainer, scheme.onErrorContainer, Icons.Outlined.Delete)
        SwipeAction.Edit -> Triple(scheme.secondaryContainer, scheme.onSecondaryContainer, Icons.Outlined.Edit)
        SwipeAction.Qr -> Triple(scheme.secondaryContainer, scheme.onSecondaryContainer, Icons.Outlined.QrCode)
        SwipeAction.Copy -> Triple(scheme.tertiaryContainer, scheme.onTertiaryContainer, Icons.Outlined.ContentCopy)
        SwipeAction.Share -> Triple(scheme.tertiaryContainer, scheme.onTertiaryContainer, Icons.Outlined.Share)
        SwipeAction.None -> Triple(Color.Transparent, Color.Transparent, Icons.Outlined.Close)
    }
}

// Roles, not hexes: fixed colors break dark theme, dynamic color, and
// user-controlled contrast (per the M3 color doctrine).
@Composable
private fun LinkUiStatus.dotColor() = when (this) {
    LinkUiStatus.Active -> MaterialTheme.colorScheme.primary
    LinkUiStatus.Inactive -> MaterialTheme.colorScheme.outline
    LinkUiStatus.Expired -> MaterialTheme.colorScheme.tertiary
    LinkUiStatus.Blocked -> MaterialTheme.colorScheme.error
}

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class,
)
@Composable
private fun LinkRow(
    link: SpooLink,
    selecting: Boolean,
    selected: Boolean,
    showShare: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onQr: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val numbers = NumberFormat.getIntegerInstance()

    // Pairing law: on secondaryContainer everything speaks its on-color.
    val primaryText = when {
        selected -> MaterialTheme.colorScheme.onSecondaryContainer
        link.active -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val mutedText = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    // The reference card anatomy: identity + title block up top with the
    // overflow pinned to the corner, then a bottom rail — the metric on
    // the left, the date on the right. One fact per line, nothing crammed.
    Column(
        modifier = Modifier
            .cardChrome(MaterialTheme.shapes.large)
            .clip(MaterialTheme.shapes.large)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    cardContainerColor()
                },
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 16.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Identity shell: the app's one abstract shape — the spoo ghost
            // (MaterialShapes avatar-masking pattern, per shape doctrine).
            // Tapping it toggles selection, the Gmail avatar gesture.
            Box(
                modifier = Modifier
                    .width(50.dp)
                    .height(54.dp)
                    .clip(MaterialShapes.Ghostish.toShape())
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(onClick = onLongClick),
                contentAlignment = Alignment.Center,
            ) {
                Favicon(host = faviconHost(link.originalUrl), size = 24.dp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EmojiText(
                            text = link.shortUrl,
                            style = MaterialTheme.typography.titleMedium,
                            color = primaryText,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        // Constraint hints in one muted row: lock, click
                        // cap, expiry — affordance presence, no chips.
                        if (link.hasPassword) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = "Password protected",
                                modifier = Modifier.height(14.dp),
                                tint = mutedText,
                            )
                        }
                        if (link.clickLimited) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Outlined.AdsClick,
                                contentDescription = "Click limit set",
                                modifier = Modifier.height(14.dp),
                                tint = mutedText,
                            )
                        }
                        if (link.expireAtMillis != null) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = "Expiry set",
                                modifier = Modifier.height(14.dp),
                                tint = mutedText,
                            )
                        }
                    }
                    if (!selecting) {
                        // Hugs the card corner rather than the title line.
                        Box(Modifier.offset(y = (-5).dp)) {
                            LinkMenu(
                                link = link,
                                showShare = showShare,
                                menuOpen = menuOpen,
                                onMenuOpenChange = { menuOpen = it },
                                onQr = onQr,
                                onEdit = onEdit,
                                onDelete = onDelete,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = link.originalUrl.removePrefix("https://").removePrefix("http://"),
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.padding(end = 6.dp)) {
                    Text(
                        text = numbers.format(link.totalClicks),
                        style = MaterialTheme.typography.titleMedium.tabular,
                        color = primaryText,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "clicks",
                        style = MaterialTheme.typography.labelMedium,
                        color = mutedText,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = buildString {
                            if (!link.active) append("${link.status.name.lowercase()} · ")
                            append(link.createdLabel)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = mutedText,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
        }
    }
}

/** Corner overflow: compact target, the reference card's quiet "...". */
@Composable
private fun LinkMenu(
    link: SpooLink,
    showShare: Boolean,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onQr: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Box {
        IconButton(
            onClick = { onMenuOpenChange(true) },
            modifier = Modifier.size(30.dp),
        ) {
            Icon(
                Icons.Outlined.MoreHoriz,
                contentDescription = "More actions for ${link.shortUrl}",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Expressive menu skin: rounded tonal container, icon per item.
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { onMenuOpenChange(false) },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            DropdownMenuItem(
                text = { Text("Copy") },
                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                onClick = {
                    onMenuOpenChange(false)
                    clipboard.setText(AnnotatedString("https://${link.shortUrl}"))
                },
            )
            if (showShare) {
                DropdownMenuItem(
                    text = { Text("Share") },
                    leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                    onClick = {
                        onMenuOpenChange(false)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "https://${link.shortUrl}")
                        }
                        context.startActivity(Intent.createChooser(send, null))
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("QR code") },
                leadingIcon = { Icon(Icons.Outlined.QrCode, contentDescription = null) },
                onClick = { onMenuOpenChange(false); onQr() },
            )
            DropdownMenuItem(
                text = { Text("Edit") },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                onClick = { onMenuOpenChange(false); onEdit() },
            )
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = { onMenuOpenChange(false); onDelete() },
            )
        }
    }
}
