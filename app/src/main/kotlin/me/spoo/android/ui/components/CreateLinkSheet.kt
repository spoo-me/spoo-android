package me.spoo.android.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import me.spoo.android.data.CreateLinkRequest
import me.spoo.android.ui.screens.links.CreateState

/**
 * Two-phase create sheet: form -> result. Also the landing surface for the
 * share-target flow, which arrives with [initialUrl] prefilled.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CreateLinkSheet(
    initialUrl: String?,
    state: CreateState,
    onSubmit: (CreateLinkRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            when (state) {
                is CreateState.Done -> ResultPhase(
                    shortUrl = "https://${state.link.shortUrl}",
                    onDone = onDismiss,
                )
                else -> FormPhase(
                    initialUrl = initialUrl,
                    submitting = state is CreateState.Submitting,
                    error = (state as? CreateState.Failed)?.reason,
                    onSubmit = onSubmit,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FormPhase(
    initialUrl: String?,
    submitting: Boolean,
    error: String?,
    onSubmit: (CreateLinkRequest) -> Unit,
) {
    var url by rememberSaveable { mutableStateOf(initialUrl.orEmpty()) }
    var alias by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var maxClicks by rememberSaveable { mutableStateOf("") }
    var emojiAlias by rememberSaveable { mutableStateOf(false) }

    Text("Shorten a link", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(16.dp))

    OutlinedTextField(
        value = url,
        onValueChange = { url = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Long URL") },
        placeholder = { Text("https://…") },
        singleLine = true,
        enabled = !submitting,
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = alias,
        onValueChange = { alias = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(if (emojiAlias) "Emoji alias" else "Alias") },
        placeholder = { Text(if (emojiAlias) "🚀🔗 (random if empty)" else "summer-sale (random if empty)") },
        prefix = { Text("spoo.me/") },
        singleLine = true,
        enabled = !submitting,
    )
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.weight(1f),
            label = { Text("Password") },
            singleLine = true,
            enabled = !submitting,
        )
        OutlinedTextField(
            value = maxClicks,
            onValueChange = { maxClicks = it.filter(Char::isDigit) },
            modifier = Modifier.weight(1f),
            label = { Text("Max clicks") },
            singleLine = true,
            enabled = !submitting,
        )
    }
    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Emoji alias",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(checked = emojiAlias, onCheckedChange = { emojiAlias = it }, enabled = !submitting)
    }

    if (error != null) {
        Spacer(Modifier.height(8.dp))
        Text(
            error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    Spacer(Modifier.height(20.dp))
    Button(
        onClick = {
            onSubmit(
                CreateLinkRequest(
                    url = url.trim(),
                    alias = alias.trim().ifBlank { null },
                    password = password.ifBlank { null },
                    maxClicks = maxClicks.toIntOrNull(),
                    emojiAlias = emojiAlias,
                ),
            )
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !submitting && url.startsWith("http"),
    ) {
        if (submitting) {
            LoadingIndicator(modifier = Modifier.height(24.dp))
        } else {
            Text("Shorten")
        }
    }
}

@Composable
private fun ResultPhase(
    shortUrl: String,
    onDone: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Text("Link ready", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(16.dp))
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            shortUrl.removePrefix("https://"),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = { clipboard.setText(AnnotatedString(shortUrl)) },
            modifier = Modifier.weight(1f),
        ) { Text("Copy") }
        FilledTonalButton(
            onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shortUrl)
                }
                context.startActivity(Intent.createChooser(send, null))
            },
            modifier = Modifier.weight(1f),
        ) { Text("Share") }
    }
    Spacer(Modifier.height(4.dp))
    TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
        Text("Done")
    }
}
