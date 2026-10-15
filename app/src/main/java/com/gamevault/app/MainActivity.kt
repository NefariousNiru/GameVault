package com.gamevault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.gamevault.app.ui.auth.AuthGateScreen
import com.gamevault.app.ui.auth.AuthViewModel
import com.gamevault.app.ui.theme.GameVaultTheme

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels {
        val dependencies = (application as GameVaultApplication).container
        AuthViewModel.Factory(dependencies.authRepository, dependencies.connectionRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by authViewModel.state.collectAsStateWithLifecycle()
            GameVaultTheme(darkTheme = true, dynamicColor = false) {
                AuthGateScreen(
                    state = uiState,
                    onGoogleSignIn = { authViewModel.signIn(this) },
                    onVerifyFirestore = authViewModel::verifyFirestore,
                    onSignOut = { authViewModel.signOut(this) }
                )
            }
        }
    }
}
