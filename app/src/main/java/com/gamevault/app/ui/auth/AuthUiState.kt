package com.gamevault.app.ui.auth

import com.gamevault.app.domain.auth.AuthenticatedUser

data class AuthUiState(
    val loading: Boolean = true,
    val busy: Boolean = false,
    val user: AuthenticatedUser? = null,
    val error: String? = null
)
