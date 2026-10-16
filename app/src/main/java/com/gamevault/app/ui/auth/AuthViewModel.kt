package com.gamevault.app.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gamevault.app.domain.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Production authentication state: Google sign-in and sign-out only. */
class AuthViewModel(private val auth: AuthRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            auth.observeUser().collect { user ->
                mutableState.update { it.copy(loading = false, user = user, error = null) }
            }
        }
    }

    fun signIn(activity: Activity) = runOperation { auth.signInWithGoogle(activity) }
    fun signOut(activity: Activity) = runOperation { auth.signOut(activity) }

    private fun runOperation(block: suspend () -> Unit) {
        if (mutableState.value.busy) return
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, error = null) }
            try {
                block()
            } catch (exception: Exception) {
                mutableState.update { it.copy(error = exception.localizedMessage ?: "Operation failed") }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    class Factory(private val auth: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AuthViewModel::class.java))
            return AuthViewModel(auth) as T
        }
    }
}
