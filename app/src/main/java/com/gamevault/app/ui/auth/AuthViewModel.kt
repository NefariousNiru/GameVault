package com.gamevault.app.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gamevault.app.domain.auth.AuthRepository
import com.gamevault.app.domain.diagnostics.CloudConnectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val auth: AuthRepository,
    private val connection: CloudConnectionRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            auth.observeUser().collect { user ->
                mutableState.update { old ->
                    old.copy(
                        loading = false,
                        user = user,
                        connectionMessage = null,
                        error = null
                    )
                }
            }
        }
    }

    fun signIn(activity: Activity) = runOperation {
        auth.signInWithGoogle(activity)
    }

    fun signOut(activity: Activity) = runOperation {
        auth.signOut(activity)
    }

    fun verifyFirestore() {
        val uid = state.value.user?.uid ?: return
        runOperation {
            val verified = connection.verify(uid)
            mutableState.update {
                it.copy(connectionMessage = if (verified) {
                    "Cloud write + server read succeeded"
                } else {
                    "Cloud verification returned unexpected data"
                })
            }
        }
    }

    private fun runOperation(block: suspend () -> Unit) {
        if (mutableState.value.busy) return
        viewModelScope.launch {
            mutableState.update {
                it.copy(busy = true, error = null, connectionMessage = null)
            }
            try {
                block()
            } catch (exception: Exception) {
                mutableState.update {
                    it.copy(error = exception.localizedMessage ?: "Operation failed")
                }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    class Factory(
        private val auth: AuthRepository,
        private val connection: CloudConnectionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AuthViewModel::class.java))
            return AuthViewModel(auth, connection) as T
        }
    }
}
