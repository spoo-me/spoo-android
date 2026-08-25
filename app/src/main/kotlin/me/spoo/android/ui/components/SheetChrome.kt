package me.spoo.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Trailing gap for bottom-sheet content. The fully expanded
 * ModalBottomSheet already pads its content by the navigation-bar inset
 * (don't add it again — that reads as a dead band above the gesture
 * pill); this is only the aesthetic breathing room after the last
 * element. Sheets must open with skipPartiallyExpanded when their
 * content can exceed the half height, or the tail gets clipped anyway.
 */
@Composable
fun sheetBottomPadding(): Dp = 8.dp
