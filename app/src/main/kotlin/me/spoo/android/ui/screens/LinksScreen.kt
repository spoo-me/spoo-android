package me.spoo.android.ui.screens

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import me.spoo.android.data.LinkUiStatus
import me.spoo.android.data.LinksFilter
import me.spoo.android.data.SpooLink
import me.spoo.android.ui.components.CreateLinkSheet
import me.spoo.android.ui.components.EditLinkSheet
import me.spoo.android.ui.components.Favicon
import me.spoo.android.ui.components.QrDialog
import me.spoo.android.ui.components.faviconHost
import me.spoo.android.ui.screens.links.LinkSort
import me.spoo.android.ui.screens.links.LinksViewModel
import me.spoo.android.ui.theme.cardChrome
import me.spoo.android.ui.theme.cardContainerColor
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
    onOpenStats: (String) -> Unit,
    viewModel: LinksViewModel = viewModel(),
) {
    val links by viewModel.links.collectAsState()
    val query by viewModel.query.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val createState by viewModel.createState.collectAsState()
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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val totalClicks = links.sumOf { it.totalClicks }
    val numbers = NumberFormat.getIntegerInstance()
    val selecting = selection.isNotEmpty()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar) },
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
            } else {
                MediumFlexibleTopAppBar(
                    title = { Text("spoo.me") },
                    subtitle = {
                        Text(
                            "${links.size} links · ${numbers.format(totalClicks)} clicks",
                            style = androidx.compose.material3.LocalTextStyle.current.tabular,
                        )
                    },
                    actions = {
                        IconButton(onClick = { showLinkFilters = true }) {
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
                        IconButton(onClick = { showCreatedPicker = true }) {
                            BadgedBox(
                                badge = { if (filter.createdRange != null) Badge() },
                            ) {
                                Icon(
                                    Icons.Outlined.DateRange,
                                    contentDescription = "Created date range",
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "search") {
                // Filled, not outlined: quiet chrome over hairline chrome.
                TextField(
                    value = query,
                    onValueChange = { viewModel.query.value = it },
                    modifier = Modifier.fillMaxWidth(),
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
                }
            }
            items(links, key = { it.id }) { link ->
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
        }

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
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete selected",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        }
    }

    if (showCreateSheet) {
        CreateLinkSheet(
            initialUrl = sharedUrl,
            state = createState,
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
        val pickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showCreatedPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val from = pickerState.selectedStartDateMillis
                        val to = pickerState.selectedEndDateMillis
                        if (from != null && to != null) {
                            viewModel.filter.value = filter.copy(createdRange = from to to)
                        }
                        showCreatedPicker = false
                    },
                    enabled = pickerState.selectedEndDateMillis != null,
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.filter.value = filter.copy(createdRange = null)
                    showCreatedPicker = false
                }) { Text("All time") }
            },
        ) {
            DateRangePicker(state = pickerState, showModeToggle = false)
        }
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
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
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
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(3.dp))
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
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun LinkUiStatus.dotColor() = when (this) {
    LinkUiStatus.Active -> Color(0xFF4ADE80)
    LinkUiStatus.Inactive -> Color(0xFF9CA3AF)
    LinkUiStatus.Expired -> Color(0xFFF59E0B)
    LinkUiStatus.Blocked -> Color(0xFFEF4444)
}

@OptIn(ExperimentalFoundationApi::class)
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
            // Identity shell sized to the icon, centered in the card.
            Box(
                modifier = Modifier
                    .width(46.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Favicon(host = faviconHost(link.originalUrl), size = 26.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = link.shortUrl,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (link.active) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
                    if (!selecting) {
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
                Spacer(Modifier.height(1.dp))
                Text(
                    text = link.originalUrl.removePrefix("https://").removePrefix("http://"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.padding(end = 6.dp)) {
                    Text(
                        text = numbers.format(link.totalClicks),
                        style = MaterialTheme.typography.titleMedium.tabular,
                        color = if (link.active) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.alignByBaseline(),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "clicks",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = buildString {
                            if (!link.active) append("${link.status.name.lowercase()} · ")
                            append(link.createdLabel)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        DropdownMenu(expanded = menuOpen, onDismissRequest = { onMenuOpenChange(false) }) {
            DropdownMenuItem(
                text = { Text("Copy") },
                onClick = {
                    onMenuOpenChange(false)
                    clipboard.setText(AnnotatedString("https://${link.shortUrl}"))
                },
            )
            if (showShare) {
                DropdownMenuItem(
                    text = { Text("Share") },
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
                onClick = { onMenuOpenChange(false); onQr() },
            )
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = { onMenuOpenChange(false); onEdit() },
            )
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = { onMenuOpenChange(false); onDelete() },
            )
        }
    }
}
