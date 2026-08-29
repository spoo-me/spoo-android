package me.spoo.android.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The "Expires" form input: a read-only field that opens a two-step
 * date -> time picker on tap. Empty means never; a set expiry clears with
 * the trailing X. The time step defaults to end of day and reads in the
 * user's local timezone; the server validates the moment is in the future.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpiryField(
    millis: Long?,
    onChange: (Long?) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    errorText: String? = null,
) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    // The date step's result (UTC midnight), waiting for the time step.
    var pendingDateUtc by remember { mutableStateOf<Long?>(null) }

    val source = remember { MutableInteractionSource() }
    // Read-only fields still focus on tap: the release opens the picker.
    LaunchedEffect(source) {
        source.interactions.collect {
            if (it is PressInteraction.Release) showDate = true
        }
    }
    OutlinedTextField(
        value =
            millis?.let {
                SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.US).format(Date(it))
            } ?: "",
        onValueChange = {},
        modifier = modifier,
        readOnly = true,
        label = { Text("Expires") },
        placeholder = { Text("Never") },
        interactionSource = source,
        trailingIcon = {
            if (millis != null) {
                IconButton(onClick = { onChange(null) }, enabled = enabled) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear expiry")
                }
            } else {
                Icon(Icons.Outlined.Event, contentDescription = null)
            }
        },
        isError = errorText != null,
        supportingText = errorText?.let { { Text(it) } },
        singleLine = true,
        enabled = enabled,
    )

    if (showDate) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = millis)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDateUtc = dateState.selectedDateMillis
                        showDate = false
                        if (pendingDateUtc != null) showTime = true
                    },
                ) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTime) {
        val existing = millis?.let { Calendar.getInstance().apply { timeInMillis = it } }
        val timeState =
            rememberTimePickerState(
                initialHour = existing?.get(Calendar.HOUR_OF_DAY) ?: 23,
                initialMinute = existing?.get(Calendar.MINUTE) ?: 59,
            )
        TimePickerDialog(
            onDismissRequest = { showTime = false },
            title = { Text("Expiry time") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDateUtc?.let { dateUtc ->
                            onChange(combineLocal(dateUtc, timeState.hour, timeState.minute))
                        }
                        showTime = false
                    },
                ) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showTime = false }) { Text("Cancel") }
            },
        ) {
            TimePicker(state = timeState)
        }
    }
}

/**
 * The date picker hands back UTC midnight of the chosen day; the picked
 * wall-clock time means that day in the USER'S timezone, so recombine the
 * calendar fields locally.
 */
private fun combineLocal(
    dateUtcMillis: Long,
    hour: Int,
    minute: Int,
): Long {
    val utc =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = dateUtcMillis
        }
    return Calendar
        .getInstance()
        .apply {
            clear()
            set(
                utc.get(Calendar.YEAR),
                utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH),
                hour,
                minute,
            )
        }.timeInMillis
}
