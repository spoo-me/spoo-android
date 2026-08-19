package me.spoo.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Walking-skeleton links screen. Real list + create sheet land in M1;
 * [sharedText] is the ACTION_SEND payload when launched from a share sheet.
 */
@Composable
fun LinksScreen(
    sharedText: String?,
    onOpenStats: (String) -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("spoo.me", style = MaterialTheme.typography.displaySmall)
            if (sharedText != null) {
                Text(
                    "shared: $sharedText",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(onClick = { onOpenStats("demo") }) {
                Text("Stats skeleton")
            }
        }
    }
}
