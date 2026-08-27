package me.spoo.android.data

import me.spoo.AuthenticationException
import me.spoo.ContentBlockedException
import me.spoo.NotFoundException
import me.spoo.PermissionException
import me.spoo.RateLimitException
import me.spoo.SessionExpiredException
import me.spoo.SpooApiException
import me.spoo.SpooDecodeException
import me.spoo.SpooException
import me.spoo.SpooIOException
import me.spoo.ValidationException

/** Which form input a failure points at, for field-anchored display. */
enum class ErrorField { Url, Alias, Password, MaxClicks, Expiry }

/** A user-facing failure: plain words plus, when known, the field at fault. */
data class FriendlyError(
    val message: String,
    val field: ErrorField? = null,
)

/**
 * Maps any repository failure to words a user should actually read. The SDK
 * already types every failure; this layer only chooses the sentence.
 */
fun friendlyError(
    e: Throwable,
    fallback: String,
): FriendlyError =
    when (e) {
        is SpooIOException ->
            FriendlyError("Can't reach spoo.me. Check your connection and try again.")
        is SpooDecodeException ->
            FriendlyError("Unexpected response from the server. Try updating the app.")
        is SessionExpiredException ->
            FriendlyError("Your session expired. Sign in again.")
        is RateLimitException -> {
            val wait = e.rateLimit.retryAfter?.inWholeSeconds
            FriendlyError(
                if (wait != null && wait > 0) {
                    "Too many requests. Try again in ${wait}s."
                } else {
                    "Too many requests. Try again in a moment."
                },
            )
        }
        is ContentBlockedException -> FriendlyError("This destination is blocked.")
        is NotFoundException -> FriendlyError("That link no longer exists.")
        is AuthenticationException, is PermissionException ->
            FriendlyError("You don't have access to do that.")
        is ValidationException ->
            FriendlyError(cleanServerMessage(e, fallback), errorField(e.field))
        is SpooApiException -> FriendlyError(cleanServerMessage(e, fallback))
        is SpooException -> FriendlyError(fallback)
        else -> FriendlyError(fallback)
    }

private fun errorField(field: String?): ErrorField? =
    when (field) {
        "long_url", "url" -> ErrorField.Url
        "alias" -> ErrorField.Alias
        "password" -> ErrorField.Password
        "max_clicks" -> ErrorField.MaxClicks
        "expire_after" -> ErrorField.Expiry
        else -> null
    }

/**
 * Envelope messages arrive pydantic-flavored ("alias: Value error, alias
 * must be ..."). Strip the machinery, keep the sentence. Bodies that were
 * never an envelope surface as "HTTP {status}" and get the fallback.
 */
private fun cleanServerMessage(
    e: SpooApiException,
    fallback: String,
): String {
    var text = e.message.orEmpty().ifBlank { fallback }
    if (Regex("^HTTP \\d+$").matches(text)) return fallback
    e.field?.let { f -> text = text.removePrefix("$f: ") }
    text = text.removePrefix("Value error, ")
    return text.replaceFirstChar { it.uppercase() }
}
