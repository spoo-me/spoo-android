package me.spoo.android.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import me.spoo.oauth.TokenPair

private val Context.authDataStore by preferencesDataStore(name = "auth")

/**
 * Token persistence. App-private DataStore under FBE; if we ever store more
 * than bearer tokens, revisit with a Keystore-wrapped layer.
 */
class TokenStore(private val context: Context) {

    data class Persisted(val tokens: TokenPair, val username: String)

    suspend fun read(): Persisted? {
        val prefs = context.authDataStore.data.first()
        val access = prefs[ACCESS] ?: return null
        val refresh = prefs[REFRESH] ?: return null
        val username = prefs[USERNAME] ?: return null
        return Persisted(TokenPair(access, refresh), username)
    }

    suspend fun write(tokens: TokenPair, username: String) {
        context.authDataStore.edit {
            it[ACCESS] = tokens.accessToken
            it[REFRESH] = tokens.refreshToken
            it[USERNAME] = username
        }
    }

    suspend fun updateTokens(tokens: TokenPair) {
        context.authDataStore.edit {
            it[ACCESS] = tokens.accessToken
            it[REFRESH] = tokens.refreshToken
        }
    }

    suspend fun clear() {
        context.authDataStore.edit { it.clear() }
    }

    // The in-flight PKCE handshake survives process death during the
    // browser round-trip ("don't keep activities" is a real setting).
    data class Pending(val state: String, val verifier: String)

    suspend fun writePending(state: String, verifier: String) {
        context.authDataStore.edit {
            it[PENDING_STATE] = state
            it[PENDING_VERIFIER] = verifier
        }
    }

    suspend fun readPending(): Pending? {
        val prefs = context.authDataStore.data.first()
        val state = prefs[PENDING_STATE] ?: return null
        val verifier = prefs[PENDING_VERIFIER] ?: return null
        return Pending(state, verifier)
    }

    suspend fun clearPending() {
        context.authDataStore.edit {
            it.remove(PENDING_STATE)
            it.remove(PENDING_VERIFIER)
        }
    }

    private companion object {
        val ACCESS = stringPreferencesKey("access_token")
        val REFRESH = stringPreferencesKey("refresh_token")
        val USERNAME = stringPreferencesKey("username")
        val PENDING_STATE = stringPreferencesKey("pending_state")
        val PENDING_VERIFIER = stringPreferencesKey("pending_verifier")
    }
}
