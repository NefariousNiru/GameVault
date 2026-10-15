package com.gamevault.app.data.auth

import android.app.Activity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.gamevault.app.R
import com.gamevault.app.domain.auth.AuthRepository
import com.gamevault.app.domain.auth.AuthenticatedUser
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Google Credential Manager -> Firebase Authentication. */
class FirebaseGoogleAuthRepository(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override fun observeUser(): Flow<AuthenticatedUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            trySend(user?.let {
                AuthenticatedUser(it.uid, it.displayName, it.email)
            })
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    /**
     * Authenticates the user through Google's explicit sign-in flow,
     * then exchanges the Google ID token for a Firebase session.
     */
    override suspend fun signInWithGoogle(activity: Activity) {
        val webClientId = activity.getString(R.string.default_web_client_id)

        val googleOption = GetSignInWithGoogleOption.Builder(
            serverClientId = webClientId
        ).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        val credentialManager = CredentialManager.create(activity)

        val result = credentialManager.getCredential(
            context = activity,
            request = request
        )

        val credential = result.credential

        require(
            credential is CustomCredential &&
                    credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            "Unexpected credential type returned by Google"
        }

        val googleToken = GoogleIdTokenCredential
            .createFrom(credential.data)
            .idToken

        firebaseAuth.signInWithCredential(
            GoogleAuthProvider.getCredential(googleToken, null)
        ).await()
    }

    override suspend fun signOut(activity: Activity) {
        firebaseAuth.signOut()
        // Clear cached provider selection; auth sign-out remains effective
        // even if the credential provider cannot be reached.
        try {
            CredentialManager.create(activity)
                .clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {
            // The next sign-in still presents an explicit account chooser.
        }
    }
}
