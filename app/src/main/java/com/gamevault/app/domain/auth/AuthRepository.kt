package com.gamevault.app.domain.auth

import android.app.Activity
import kotlinx.coroutines.flow.Flow

/** Abstracts Firebase and Android Credential Manager from the UI. */
interface AuthRepository {
    fun observeUser(): Flow<AuthenticatedUser?>
    suspend fun signInWithGoogle(activity: Activity)
    suspend fun signOut(activity: Activity)
}
