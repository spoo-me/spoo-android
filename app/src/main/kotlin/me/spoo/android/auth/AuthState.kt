package me.spoo.android.auth

sealed interface AuthState {
    data object SignedOut : AuthState
    data object Authorizing : AuthState
    data class SignedIn(val username: String) : AuthState
}
