package com.gamevault.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Temporary auth/Firestore checkpoint UI; replaced by the phase 3 shell. */
@Composable
fun AuthGateScreen(
    state: AuthUiState,
    onGoogleSignIn: () -> Unit,
    onVerifyFirestore: () -> Unit,
    onSignOut: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("GAMEVAULT", style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Cloud foundation", style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when {
                        state.loading -> {
                            CircularProgressIndicator()
                            Text("Restoring session…")
                        }
                        state.user == null -> {
                            Text("Sign in with Google to start your private game library.")
                            Button(onClick = onGoogleSignIn, enabled = !state.busy,
                                modifier = Modifier.fillMaxWidth()) {
                                Text("Continue with Google")
                            }
                        }
                        else -> {
                            Text("Signed in: ${state.user.displayName ?: state.user.email ?: "Google account"}")
                            Text("User ID: ${state.user.uid.take(8)}…",
                                style = MaterialTheme.typography.bodySmall)
                            Button(onClick = onVerifyFirestore, enabled = !state.busy,
                                modifier = Modifier.fillMaxWidth()) {
                                Text("Test Firestore connection")
                            }
                            OutlinedButton(onClick = onSignOut, enabled = !state.busy,
                                modifier = Modifier.fillMaxWidth()) {
                                Text("Sign out")
                            }
                        }
                    }
                    if (state.busy) CircularProgressIndicator()
                    state.connectionMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary)
                    }
                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Library · Queue · Settings — next UI checkpoint",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
