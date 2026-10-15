package com.gamevault.app.domain.auth

/** Firebase user information safe to expose to presentation code. */
data class AuthenticatedUser(
    val uid: String,
    val displayName: String?,
    val email: String?
)
