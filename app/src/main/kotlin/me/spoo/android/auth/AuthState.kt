package me.spoo.android.auth

sealed interface AuthState {
    data object SignedOut : AuthState

    data object Authorizing : AuthState

    data class SignedIn(
        val username: String,
    ) : AuthState

    /** Signed out because the OAuth flow died mid-way; the gate says so. */
    data object SignInFailed : AuthState
}
