package com.gamevault.app

import android.os.Bundle
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.gamevault.app.platform.NetworkStatus
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.gamevault.app.ui.auth.AuthViewModel
import com.gamevault.app.ui.auth.SignInScreen
import com.gamevault.app.ui.home.GameVaultShell
import com.gamevault.app.ui.theme.GameVaultTheme

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels {
        val dependencies = (application as GameVaultApplication).container
        AuthViewModel.Factory(dependencies.authRepository, dependencies.connectionRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by authViewModel.state.collectAsStateWithLifecycle()
            val networkFlow = remember { NetworkStatus.observe(applicationContext) }
            val online by networkFlow.collectAsStateWithLifecycle(initialValue = true)
            LaunchedEffect(online) {
                if (!online) Toast.makeText(this@MainActivity,
                    "No internet — showing saved data", Toast.LENGTH_LONG).show()
            }
            GameVaultTheme() {
                val signedIn = uiState.user
                if (signedIn == null) {
                    SignInScreen(
                        state = uiState,
                        onGoogleSignIn = { authViewModel.signIn(this) }
                    )
                } else {
                    GameVaultShell(
                        user = signedIn,
                        busy = uiState.busy,
                        message = uiState.connectionMessage,
                        error = uiState.error,
                        onVerifyCloud = authViewModel::verifyFirestore,
                        onSignOut = { authViewModel.signOut(this) }
                    )
                }
            }
        }
    }
}
