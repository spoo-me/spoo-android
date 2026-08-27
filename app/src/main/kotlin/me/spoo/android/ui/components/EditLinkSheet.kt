package me.spoo.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import me.spoo.android.data.ErrorField
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
    // Plain remember: visibility resets on reopen, the draft does not.
    var passwordVisible by remember { mutableStateOf(false) }
    // The server never echoes the password, so editing is explicit modes:
    // keep (default, untouched), replace, remove — all reversible until
    // save. Mirrors the webapp's "Password is set · Replace / Remove".
    var passwordMode by rememberSaveable {
        mutableStateOf(if (link.hasPassword) PwMode.Keep else PwMode.New)
    }
    // Existing values prefill; the touched flags keep untouched fields out
    // of the patch (tri-state: keep / set / clear).
    var maxClicks by rememberSaveable { mutableStateOf(link.maxClicks?.toString() ?: "") }
    var maxClicksTouched by rememberSaveable { mutableStateOf(false) }
    var expiry by rememberSaveable { mutableStateOf(link.expireAtMillis) }
    var expiryTouched by rememberSaveable { mutableStateOf(false) }
    var privateStats by rememberSaveable { mutableStateOf(link.privateStats) }
    var blockBots by rememberSaveable { mutableStateOf(link.blockBots) }

    LaunchedEffect(state) {
        if (state is EditState.Done) onDismiss()
    }

    val submitting = state is EditState.Submitting
    val error = (state as? EditState.Failed)?.error
    // Emoji aliases are edited as-is (the field keeps whatever the link
    // has); only plain-text aliases get the charset filter.
    val textAlias = link.shortCode.all(::isAliasChar)
    val urlOk = isLikelyUrl(longUrl)
    val passwordOk =
        when (passwordMode) {
            PwMode.Replace, PwMode.New -> password.isBlank() || isAcceptablePassword(password)
            else -> true
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Same clipping hazard as the create sheet: open in full.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 24.dp)
                    .imePadding(),
        ) {
            Text("Edit ${link.shortUrl}", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = longUrl,
                onValueChange = { longUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Destination") },
                // Tracks the typed host live, same as the create sheet.
                leadingIcon = {
                    Favicon(
                        host = if (urlOk) faviconHost(normalizeUrl(longUrl)) else null,
                        size = 20.dp,
                    )
                },
                isError = (longUrl.isNotBlank() && !urlOk) || error?.field == ErrorField.Url,
                supportingText =
                    when {
                        error?.field == ErrorField.Url -> ({ Text(error.message) })
                        longUrl.isNotBlank() && !urlOk -> ({ Text("Enter a valid web address") })
                        else -> null
                    },
                singleLine = true,
                enabled = !submitting,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = alias,
                onValueChange = { alias = if (textAlias) it.filter(::isAliasChar) else it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Alias") },
                prefix = { Text("spoo.me/") },
                isError = error?.field == ErrorField.Alias,
                supportingText =
                    if (error?.field == ErrorField.Alias) {
                        { Text(error.message) }
                    } else {
                        null
                    },
                singleLine = true,
                enabled = !submitting,
            )
            Spacer(Modifier.height(12.dp))
            when (passwordMode) {
                PwMode.Keep ->
                    OutlinedTextField(
                        value = "••••••••",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        isError = error?.field == ErrorField.Password,
                        supportingText =
                            if (error?.field == ErrorField.Password) {
                                { Text(error.message) }
                            } else {
                                null
                            },
                        trailingIcon = {
                            Row {
                                IconButton(
                                    onClick = {
                                        password = ""
                                        passwordMode = PwMode.Replace
                                    },
                                    enabled = !submitting,
                                ) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Replace password")
                                }
                                IconButton(
                                    onClick = {
                                        password = ""
                                        passwordMode = PwMode.Remove
                                    },
                                    enabled = !submitting,
                                ) {
                                    Icon(
                                        Icons.Outlined.DeleteOutline,
                                        contentDescription = "Remove password",
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        enabled = !submitting,
                    )
                PwMode.Remove ->
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        supportingText = { Text("Will be removed on save") },
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordMode = PwMode.Keep },
                                enabled = !submitting,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.Undo,
                                    contentDescription = "Keep password",
                                )
                            }
                        },
                        singleLine = true,
                        enabled = !submitting,
                    )
                else ->
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(if (passwordMode == PwMode.Replace) "New password" else "Password")
                        },
                        isError =
                            (password.isNotBlank() && !passwordOk) ||
                                error?.field == ErrorField.Password,
                        supportingText =
                            when {
                                error?.field == ErrorField.Password -> ({ Text(error.message) })
                                password.isNotBlank() && !passwordOk ->
                                    ({ Text("8+ characters with a letter, a number, and @ or .") })
                                else -> null
                            },
                        // Password treatment keeps IMEs from learning the value.
                        visualTransformation =
                            if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            Row {
                                if (password.isEmpty()) {
                                    IconButton(
                                        onClick = {
                                            password = suggestPassword()
                                            passwordVisible = true
                                        },
                                        enabled = !submitting,
                                    ) {
                                        Icon(Icons.Outlined.Casino, contentDescription = "Suggest a password")
                                    }
                                } else {
                                    IconButton(
                                        onClick = { passwordVisible = !passwordVisible },
                                        enabled = !submitting,
                                    ) {
                                        Icon(
                                            if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        )
                                    }
                                }
                                if (passwordMode == PwMode.Replace) {
                                    IconButton(
                                        onClick = {
                                            password = ""
                                            passwordMode = PwMode.Keep
                                        },
                                        enabled = !submitting,
                                    ) {
                                        Icon(Icons.Outlined.Close, contentDescription = "Keep current password")
                                    }
                                }
                            }
                        },
                        singleLine = true,
                        enabled = !submitting,
                    )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = maxClicks,
                onValueChange = {
                    maxClicks = it.filter(Char::isDigit)
                    maxClicksTouched = true
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Max clicks") },
                placeholder = { Text("empty to remove") },
                isError = error?.field == ErrorField.MaxClicks,
                supportingText =
                    if (error?.field == ErrorField.MaxClicks) {
                        { Text(error.message) }
                    } else {
                        null
                    },
                trailingIcon =
                    if (maxClicks.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = {
                                    maxClicks = ""
                                    maxClicksTouched = true
                                },
                                enabled = !submitting,
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear click limit")
                            }
                        }
                    } else {
                        null
                    },
                singleLine = true,
                enabled = !submitting,
            )
            Spacer(Modifier.height(12.dp))
            ExpiryField(
                millis = expiry,
                onChange = {
                    expiry = it
                    expiryTouched = true
                },
                enabled = !submitting,
                modifier = Modifier.fillMaxWidth(),
                errorText = error?.takeIf { it.field == ErrorField.Expiry }?.message,
            )
            Spacer(Modifier.height(8.dp))
            LinkOptionRow(
                label = "Private stats",
                help = "Only you can open this link's stats page.",
                checked = privateStats,
                onCheckedChange = { privateStats = it },
                enabled = !submitting,
            )
            LinkOptionRow(
                label = "Block bots",
                help = "Keeps known bots and crawlers from following the link.",
                checked = blockBots,
                onCheckedChange = { blockBots = it },
                enabled = !submitting,
            )

            if (error != null && error.field == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    error.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    onSubmit(
                        LinkEdit(
                            longUrl = normalizeUrl(longUrl).takeIf { it != link.originalUrl },
                            alias = alias.trim().takeIf { it.isNotBlank() && it != link.shortCode },
                            password =
                                password.takeIf {
                                    (passwordMode == PwMode.Replace || passwordMode == PwMode.New) &&
                                        it.isNotBlank()
                                },
                            clearPassword = passwordMode == PwMode.Remove,
                            maxClicks = maxClicks.toLongOrNull().takeIf { maxClicksTouched },
                            clearMaxClicks = maxClicksTouched && maxClicks.isBlank(),
                            expireAtMillis = expiry.takeIf { expiryTouched },
                            clearExpiry = expiryTouched && expiry == null && link.expireAtMillis != null,
                            privateStats = privateStats.takeIf { it != link.privateStats },
                            blockBots = blockBots.takeIf { it != link.blockBots },
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !submitting && urlOk && passwordOk,
            ) {
                if (submitting) LoadingIndicator(modifier = Modifier.height(24.dp)) else Text("Save")
            }
            Spacer(Modifier.height(sheetBottomPadding()))
        }
    }
}

/** Password edit modes; New = the link has no password yet. */
private enum class PwMode { Keep, Replace, Remove, New }
