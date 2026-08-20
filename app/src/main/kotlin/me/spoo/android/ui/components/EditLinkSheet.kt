package me.spoo.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.spoo.android.data.LinkEdit
import me.spoo.android.data.SpooLink
import me.spoo.android.ui.screens.links.EditState

/**
 * Edit sheet with tri-state semantics: untouched fields are kept, an
 * emptied password/max-clicks field clears the value on the server.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditLinkSheet(
    link: SpooLink,
    state: EditState,
    onSubmit: (LinkEdit) -> Unit,
    onDismiss: () -> Unit,
) {
    var longUrl by rememberSaveable { mutableStateOf(link.originalUrl) }
    var alias by rememberSaveable { mutableStateOf(link.shortCode) }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordTouched by rememberSaveable { mutableStateOf(false) }
    var maxClicks by rememberSaveable { mutableStateOf("") }
    var maxClicksTouched by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is EditState.Done) onDismiss()
    }

    val submitting = state is EditState.Submitting

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Text("Edit ${link.shortUrl}", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = longUrl,
                onValueChange = { longUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Destination") },
                singleLine = true,
                enabled = !submitting,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = alias,
                onValueChange = { alias = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Alias") },
                prefix = { Text("spoo.me/") },
                singleLine = true,
                enabled = !submitting,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; passwordTouched = true },
                    modifier = Modifier.weight(1f),
                    label = { Text("Password") },
                    placeholder = { Text(if (link.hasPassword) "set · empty to remove" else "not set") },
                    singleLine = true,
                    enabled = !submitting,
                )
                OutlinedTextField(
                    value = maxClicks,
                    onValueChange = { maxClicks = it.filter(Char::isDigit); maxClicksTouched = true },
                    modifier = Modifier.weight(1f),
                    label = { Text("Max clicks") },
                    placeholder = { Text("empty to remove") },
                    singleLine = true,
                    enabled = !submitting,
                )
            }

            if (state is EditState.Failed) {
                Spacer(Modifier.height(8.dp))
                Text(
                    state.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    onSubmit(
                        LinkEdit(
                            longUrl = longUrl.trim().takeIf { it != link.originalUrl && it.startsWith("http") },
                            alias = alias.trim().takeIf { it.isNotBlank() && it != link.shortCode },
                            password = password.takeIf { passwordTouched && it.isNotBlank() },
                            clearPassword = passwordTouched && password.isBlank() && link.hasPassword,
                            maxClicks = maxClicks.toLongOrNull().takeIf { maxClicksTouched },
                            clearMaxClicks = maxClicksTouched && maxClicks.isBlank(),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !submitting,
            ) {
                if (submitting) LoadingIndicator(modifier = Modifier.height(24.dp)) else Text("Save")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
