package me.spoo.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.spoo.android.data.EmojiCatalog
import me.spoo.android.data.EmojiChoice
import me.spoo.android.ui.theme.loaderContainerColor

/**
 * In-app picker for emoji aliases: the accepted catalogue is the only
 * input surface, so an invalid alias can't be composed. Search over
 * names/keywords, category jump row, sectioned grid.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EmojiAliasPicker(
    catalog: EmojiCatalog?,
    enabled: Boolean,
    onPick: (EmojiChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    // One contained tray, not loose form elements: two tonal tiers above
    // the sheet so the boundary actually reads (the sheet itself sits on
    // surfaceContainerLow).
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box(
            Modifier
                .height(320.dp)
                .padding(horizontal = 12.dp)
                .padding(top = 12.dp),
        ) {
            if (catalog == null) {
                ContainedLoadingIndicator(
                    modifier = Modifier.size(56.dp).align(Alignment.Center),
                    containerColor = loaderContainerColor(),
                    indicatorColor = MaterialTheme.colorScheme.primary,
                )
            } else {
                PickerBody(catalog, enabled, onPick)
            }
        }
    }
}

private sealed interface PickerItem {
    data class Header(
        val group: String,
    ) : PickerItem

    data class Cell(
        val choice: EmojiChoice,
    ) : PickerItem
}

@Composable
private fun PickerBody(
    catalog: EmojiCatalog,
    enabled: Boolean,
    onPick: (EmojiChoice) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val gridState = rememberLazyGridState()

    // Drop entries the device font can't draw — tofu never reaches the grid.
    val renderable =
        remember(catalog) {
            val paint = android.graphics.Paint()
            catalog.entries.filter { paint.hasGlyph(it.char.emojiPresentation()) }
        }

    val sections =
        remember(renderable) {
            buildList {
                var last: String? = null
                renderable.forEach {
                    if (it.group != last) {
                        add(PickerItem.Header(it.group))
                        last = it.group
                    }
                    add(PickerItem.Cell(it))
                }
            }
        }

    val filtered =
        remember(renderable, query) {
            val q = query.trim().lowercase()
            if (q.isEmpty()) {
                emptyList()
            } else {
                renderable.filter {
                    it.name.contains(q) || it.keywords.any { k -> k.contains(q) }
                }
            }
        }

    Column {
        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            placeholder = { Text("Search emoji") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            shape = CircleShape,
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 44.dp),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (query.isBlank()) {
                items(
                    count = sections.size,
                    span = { i ->
                        if (sections[i] is PickerItem.Header) {
                            GridItemSpan(maxLineSpan)
                        } else {
                            GridItemSpan(1)
                        }
                    },
                ) { i ->
                    when (val item = sections[i]) {
                        is PickerItem.Header ->
                            Text(
                                item.group,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                // Start inset optically aligns with the emoji
                                // column (glyphs sit centered in 44dp cells).
                                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp, start = 8.dp),
                            )
                        is PickerItem.Cell -> EmojiCell(item.choice, enabled, onPick)
                    }
                }
            } else if (filtered.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No matching emoji",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(filtered.size) { i -> EmojiCell(filtered[i], enabled, onPick) }
            }
        }
    }
}

@Composable
private fun EmojiCell(
    choice: EmojiChoice,
    enabled: Boolean,
    onPick: (EmojiChoice) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled) { onPick(choice) },
        contentAlignment = Alignment.Center,
    ) {
        FluentEmoji(char = choice.char, size = 30.dp)
    }
}

/**
 * Display form of a canonical (VS16-stripped) catalogue character. BMP
 * symbols (single UTF-16 unit: ❤ ☕ ♻) default to text presentation
 * without the selector; astral emoji never need it.
 */
internal fun String.emojiPresentation(): String = if (length == 1) this + "\uFE0F" else this
