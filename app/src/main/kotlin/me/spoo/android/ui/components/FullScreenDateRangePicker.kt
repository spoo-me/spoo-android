package me.spoo.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * DateRangePicker the way M3 intends: full-screen, close/apply in a top
 * bar — never squeezed into the small DatePickerDialog frame.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenDateRangePicker(
    onDismiss: () -> Unit,
    onApply: (Long, Long) -> Unit,
    neutralLabel: String? = null,
    onNeutral: (() -> Unit)? = null,
) {
    val state = rememberDateRangePickerState()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false, // edge to edge, no dark strip
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            // Match the picker body so the top bar doesn't read two-tone.
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Cancel")
                    }
                    Spacer(Modifier.weight(1f))
                    if (neutralLabel != null && onNeutral != null) {
                        TextButton(onClick = { onNeutral(); onDismiss() }) {
                            Text(neutralLabel)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    TextButton(
                        enabled = state.selectedEndDateMillis != null,
                        onClick = {
                            val from = state.selectedStartDateMillis
                            val to = state.selectedEndDateMillis
                            if (from != null && to != null) onApply(from, to)
                            onDismiss()
                        },
                    ) { Text("Apply") }
                }
                DateRangePicker(
                    state = state,
                    showModeToggle = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
