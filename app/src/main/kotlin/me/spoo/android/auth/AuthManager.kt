package me.spoo.android.auth

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.spoo.SpooClient
import me.spoo.android.BuildConfig
import me.spoo.oauth.Session
import me.spoo.oauth.TokenPair
import me.spoo.oauth.generatePkcePair
import me.spoo.oauth.generateState

/**
 * Sign in with spoo: device-auth PKCE in a Custom Tab. The SDK owns the
 * protocol (URL building, exchange, single-flight rotating refresh); this
 * class owns the browser hop, state verification, and persistence.
 * Exchange/refresh go through [anonClient] — never a session-bearing one.
 */
class AuthManager(
    private val store: TokenStore,
    private val anonClient: SpooClient,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    suspend fun restore() {
        val saved = store.read() ?: return
        _session.value = sessionOf(saved.tokens)
        _state.value = AuthState.SignedIn(saved.username)
    }

    fun startSignIn(context: Context) {
        val pkce = generatePkcePair()
        val csrf = generateState()
        val url = anonClient.oauth.authorizationUrl(
            appId = BuildConfig.SPOO_APP_ID,
            state = csrf,
            codeChallenge = pkce.challenge,
            redirectUri = BuildConfig.SPOO_REDIRECT_URI,
        )
        scope.launch {
            store.writePending(csrf, pkce.verifier)
            withContext(Dispatchers.Main) {
                CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
            }
        }
    }

    /** Called with the redirect URI's `?code=&state=` from the callback activity. */
    fun handleCallback(uri: Uri) {
        val code = uri.getQueryParameter("code")
        val echoedState = uri.getQueryParameter("state")
        scope.launch {
            val pending = store.readPending()
            store.clearPending()
            if (code.isNullOrBlank() || pending == null || echoedState != pending.state) {
                // CSRF mismatch or malformed callback: drop the flow, but
                // tell the gate the attempt died instead of failing mute.
                _state.value = AuthState.SignInFailed
                return@launch
            }
            _state.value = AuthState.Authorizing
            try {
                val granted = anonClient.oauth.exchangeCode(code, pending.verifier)
                val username = granted.user.userName ?: granted.user.email ?: "spoo account"
                store.write(granted.tokens(), username)
                _session.value = sessionOf(granted.tokens())
                _state.value = AuthState.SignedIn(username)
            } catch (_: Exception) {
                _state.value = AuthState.SignInFailed
            }
        }
    }

    fun signOut() {
        scope.launch {
            runCatching { _session.value?.invalidate() }
            store.clear()
            _session.value = null
            _state.value = AuthState.SignedOut
        }
    }

    private fun sessionOf(tokens: TokenPair) = Session(
        tokens = tokens,
        onRefresh = { rotated -> scope.launch { store.updateTokens(rotated) } },
    )
}
