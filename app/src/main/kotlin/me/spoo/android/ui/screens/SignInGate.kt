package me.spoo.android.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.spoo.android.R
import me.spoo.android.auth.AuthState

/** The app is authed-only: this full-screen gate is the signed-out state. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SignInGate(
    authState: AuthState,
    onSignIn: () -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The real brand glyph, unboxed: white on dark, black on light.
            val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            Image(
                painter = painterResource(if (dark) R.drawable.logo_white else R.drawable.logo_black),
                contentDescription = null,
                modifier = Modifier.size(88.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text("spoo.me", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Shorten links, watch them travel.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth(),
                enabled = authState !is AuthState.Authorizing,
            ) {
                if (authState is AuthState.Authorizing) {
                    LoadingIndicator(modifier = Modifier.height(24.dp))
                } else {
                    Text("Sign in with spoo.me")
                }
            }
            if (authState is AuthState.SignInFailed) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Sign-in didn't complete. Try again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
