package me.spoo.android.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import me.spoo.android.data.CreateLinkRequest
import me.spoo.android.data.EmojiCatalog
import me.spoo.android.data.ErrorField
import me.spoo.android.data.FriendlyError
import me.spoo.android.ui.screens.links.CreateState
import me.spoo.android.ui.theme.hero

/**
 * Two-phase create sheet: form -> result. Also the landing surface for the
 * share-target flow, which arrives with [initialUrl] prefilled.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CreateLinkSheet(
    initialUrl: String?,
    state: CreateState,
    emojiCatalog: EmojiCatalog?,
    onEmojiMode: () -> Unit,
    onSubmit: (CreateLinkRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    // The form is taller than the sheet's partial height — half-open, the
    // submit button sits clipped below the fold. Always open in full.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {
            // The hero moment gets the expressive budget: form -> result on
            // spatial-spring scale + effects fade, exits clearing early on
            // the fast token. No SizeTransform — it fights the sheet's own
            // content-height measurement and wedges it half-open.
            val scaleSpring = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
            val enterFade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
            val exitFade = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
            AnimatedContent(
                targetState = state is CreateState.Done,
                transitionSpec = {
                    (fadeIn(enterFade) + scaleIn(scaleSpring, initialScale = 0.92f))
                        .togetherWith(fadeOut(exitFade))
                },
                label = "create-phases",
            ) { done ->
                if (done) {
                    val link = (state as? CreateState.Done)?.link
                    ResultPhase(
                        shortUrl = link?.shortUrl ?: "",
                        destination = link?.originalUrl,
                        onDone = onDismiss,
                    )
                } else {
                    FormPhase(
                        initialUrl = initialUrl,
                        submitting = state is CreateState.Submitting,
                        error = (state as? CreateState.Failed)?.error,
                        emojiCatalog = emojiCatalog,
                        onEmojiMode = {
                            onEmojiMode()
                            // The picker deserves the room: pop the sheet open.
                            scope.launch { sheetState.expand() }
                        },
                        onSubmit = onSubmit,
                    )
                }
            }
            Spacer(Modifier.height(sheetBottomPadding()))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FormPhase(
    initialUrl: String?,
    submitting: Boolean,
    error: FriendlyError?,
    emojiCatalog: EmojiCatalog?,
    onEmojiMode: () -> Unit,
    onSubmit: (CreateLinkRequest) -> Unit,
) {
    var url by rememberSaveable { mutableStateOf(initialUrl.orEmpty()) }
    var alias by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var maxClicks by rememberSaveable { mutableStateOf("") }
    var expiresAt by rememberSaveable { mutableStateOf<Long?>(null) }
    var privateStats by rememberSaveable { mutableStateOf(true) }
    var blockBots by rememberSaveable { mutableStateOf(false) }
    var emojiAlias by rememberSaveable { mutableStateOf(false) }
    // Canonical picks only — the picker is the sole input surface, so the
    // composed alias is valid by construction.
    var emojiPicks by rememberSaveable { mutableStateOf("") }
    val emojiCount = emojiPicks.codePointCount(0, emojiPicks.length)
    val emojiMax = emojiCatalog?.maxGraphemes ?: Int.MAX_VALUE

    // Prevention first, server as backstop: local checks gate the button;
    // whatever still comes back lands on the field the server names.
    val urlOk = isLikelyUrl(url)
    val passwordOk = password.isBlank() || isAcceptablePassword(password)

    // AnimatedContent hosts phases in a Box: without a column root
    // every child would stack at the top-left.
    Column(Modifier.fillMaxWidth()) {
        Text("Shorten a link", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Long URL") },
            placeholder = { Text("https://…") },
            // Live identity check: the destination's favicon appears as
            // soon as the typed host resolves (globe until then).
            leadingIcon = {
                Favicon(
                    host = if (urlOk) faviconHost(normalizeUrl(url)) else null,
                    size = 20.dp,
                )
            },
            isError = (url.isNotBlank() && !urlOk) || error?.field == ErrorField.Url,
            supportingText = when {
                error?.field == ErrorField.Url -> ({ Text(error.message) })
                url.isNotBlank() && !urlOk -> ({ Text("Enter a valid web address") })
                else -> null
            },
            singleLine = true,
            enabled = !submitting,
        )
        Spacer(Modifier.height(12.dp))
        // One alias field, two modes. The emoji face inside the field swaps the
        // IME for the in-app picker (the messaging-app pattern); the keyboard
        // icon swaps back. In emoji mode the field is a read-only composed
        // display, so an invalid alias can't be typed.
        OutlinedTextField(
            value = if (emojiAlias) emojiPicks.emojiPresentationAll() else alias,
            // The API's alias charset, enforced at the keyboard: no error to show.
            onValueChange = { if (!emojiAlias) alias = it.filter(::isAliasChar) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = emojiAlias,
            label = { Text(if (emojiAlias) "Emoji alias" else "Alias") },
            placeholder = { Text("Random if empty", maxLines = 1) },
            prefix = { Text("spoo.me/") },
            isError = error?.field == ErrorField.Alias,
            trailingIcon = {
                Row {
                    if (emojiAlias && emojiPicks.isNotEmpty()) {
                        IconButton(
                            onClick = { emojiPicks = emojiPicks.dropLastCodePoint() },
                            enabled = !submitting,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Backspace,
                                contentDescription = "Remove last emoji",
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            emojiAlias = !emojiAlias
                            if (emojiAlias) onEmojiMode()
                        },
                        enabled = !submitting,
                    ) {
                        Icon(
                            if (emojiAlias) Icons.Outlined.Keyboard else Icons.Outlined.AddReaction,
                            contentDescription = if (emojiAlias) "Type a text alias" else "Compose an emoji alias",
                        )
                    }
                }
            },
            supportingText = when {
                error?.field == ErrorField.Alias -> ({ Text(error.message) })
                emojiAlias && emojiCatalog != null && emojiCount > 0 ->
                    ({ Text("$emojiCount/${emojiCatalog.maxGraphemes}") })
                else -> null
            },
            singleLine = true,
            enabled = !submitting,
        )
        val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
        val enterFade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        val exitFade = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        AnimatedVisibility(
            visible = emojiAlias,
            enter = expandVertically(spatial) + fadeIn(enterFade),
            exit = shrinkVertically(spatial) + fadeOut(exitFade),
        ) {
            EmojiAliasPicker(
                catalog = emojiCatalog,
                enabled = !submitting && emojiCount < emojiMax,
                onPick = { emojiPicks += it.char },
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.weight(1f),
                label = { Text("Password") },
                isError = (password.isNotBlank() && !passwordOk) || error?.field == ErrorField.Password,
                supportingText = when {
                    error?.field == ErrorField.Password -> ({ Text(error.message) })
                    password.isNotBlank() && !passwordOk ->
                        ({ Text("8+ characters with a letter, a number, and @ or .") })
                    else -> null
                },
                singleLine = true,
                enabled = !submitting,
            )
            OutlinedTextField(
                value = maxClicks,
                onValueChange = { maxClicks = it.filter(Char::isDigit) },
                modifier = Modifier.weight(1f),
                label = { Text("Max clicks") },
                isError = error?.field == ErrorField.MaxClicks,
                supportingText = if (error?.field == ErrorField.MaxClicks) {
                    { Text(error.message) }
                } else {
                    null
                },
                trailingIcon = if (maxClicks.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { maxClicks = "" },
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
        }
        Spacer(Modifier.height(12.dp))
        ExpiryField(
            millis = expiresAt,
            onChange = { expiresAt = it },
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

        // Field-anchored errors live on their fields; this line is only for
        // failures with no field to point at (offline, rate limit, ...).
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
                    CreateLinkRequest(
                        url = normalizeUrl(url),
                        alias = (if (emojiAlias) emojiPicks else alias.trim()).ifBlank { null },
                        password = password.ifBlank { null },
                        maxClicks = maxClicks.toIntOrNull(),
                        expireAtMillis = expiresAt,
                        privateStats = privateStats,
                        blockBots = blockBots,
                        emojiAlias = emojiAlias,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !submitting && urlOk && passwordOk,
        ) {
            if (submitting) {
                LoadingIndicator(modifier = Modifier.height(24.dp))
            } else {
                Text("Shorten")
            }
        }
    }
}

/**
 * The payoff surface. One loud element — the short URL in brand type with
 * the animated-weight signature (same as the stats hero) — over a quiet
 * label and a muted destination line. Copy is a SplitButton: the M3E
 * primary-action-plus-variant shape, the variant being copy-as-QR.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ResultPhase(
    shortUrl: String,
    destination: String?,
    onDone: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val fullUrl = "https://$shortUrl"
    var showQr by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Link ready",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Auto-shrink instead of guessing by length: URLs line-break
            // after "/", so on narrow screens a fixed size would wrap and
            // maxLines=1 silently hides the alias line.
            Text(
                shortUrl,
                style = MaterialTheme.typography.displaySmall
                    .hero(key = shortUrl, fontSize = 40.sp, lineHeight = 46.sp),
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 20.sp,
                    maxFontSize = 40.sp,
                    stepSize = 1.sp,
                ),
            )
            destination?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    it.removePrefix("https://").removePrefix("http://"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        onClick = { clipboard.setText(AnnotatedString(fullUrl)) },
                    ) { Text("Copy") }
                },
                trailingButton = {
                    SplitButtonDefaults.TrailingButton(onClick = { showQr = true }) {
                        Icon(
                            Icons.Outlined.QrCode2,
                            contentDescription = "Show QR code",
                            modifier = Modifier.size(SplitButtonDefaults.TrailingIconSize),
                        )
                    }
                },
            )
            FilledTonalButton(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, fullUrl)
                    }
                    context.startActivity(Intent.createChooser(send, null))
                },
            ) { Text("Share") }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
    }

    if (showQr) {
        QrDialog(shortUrl = shortUrl, onDismiss = { showQr = false })
    }
}

/**
 * Toggle row for link options: label, tap-to-show help tooltip (the
 * webapp's pattern), switch. Shared by the create and edit sheets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LinkOptionRow(
    label: String,
    help: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        val tipState = rememberTooltipState()
        val scope = rememberCoroutineScope()
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(help) } },
            state = tipState,
        ) {
            IconButton(onClick = { scope.launch { tipState.show() } }) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = "About $label",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

/** The API's alias charset: a-z A-Z 0-9 _ - (or emoji-only, via the picker). */
internal fun isAliasChar(c: Char): Boolean =
    c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '_' || c == '-'

private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.\\-]*://")

/**
 * Trimmed input, with https:// prepended when no scheme was typed —
 * "example.com/promo" is a valid thing to paste. An explicit scheme is
 * left alone so a typo like htp:// stays an error instead of nesting.
 */
internal fun normalizeUrl(raw: String): String {
    val trimmed = raw.trim()
    return if (trimmed.isEmpty() || SCHEME.containsMatchIn(trimmed)) trimmed else "https://$trimmed"
}

/** Whether [normalizeUrl]'s result is a parseable http(s) URL with a host. */
internal fun isLikelyUrl(raw: String): Boolean {
    val url = normalizeUrl(raw)
    if (!url.startsWith("http://") && !url.startsWith("https://")) return false
    val host = runCatching { java.net.URI(url).host }.getOrNull() ?: return false
    return host.contains('.') || host == "localhost"
}

/** The server's password policy, checked before the round-trip. */
internal fun isAcceptablePassword(p: String): Boolean =
    p.length >= 8 && p.any(Char::isLetter) && p.any(Char::isDigit) &&
        (p.contains('@') || p.contains('.'))

/** Catalogue entries are single codepoints, so backspace is one codepoint. */
private fun String.dropLastCodePoint(): String =
    if (isEmpty()) this else dropLast(Character.charCount(codePointBefore(length)))

/** Display form of the whole composed alias (see [emojiPresentation]). */
private fun String.emojiPresentationAll(): String = buildString {
    var i = 0
    while (i < this@emojiPresentationAll.length) {
        val count = Character.charCount(this@emojiPresentationAll.codePointAt(i))
        append(
            this@emojiPresentationAll.substring(i, i + count).emojiPresentation(),
        )
        i += count
    }
}
