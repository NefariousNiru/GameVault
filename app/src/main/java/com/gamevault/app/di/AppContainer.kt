package com.gamevault.app.di

import com.gamevault.app.data.auth.FirebaseGoogleAuthRepository
import com.gamevault.app.data.diagnostics.FirestoreCloudConnectionRepository
import com.gamevault.app.domain.auth.AuthRepository
import com.gamevault.app.domain.diagnostics.CloudConnectionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** App-scoped composition root; replaceable dependencies for tests. */
class AppContainer(
    val authRepository: AuthRepository = FirebaseGoogleAuthRepository(FirebaseAuth.getInstance()),
    val connectionRepository: CloudConnectionRepository =
        FirestoreCloudConnectionRepository(FirebaseFirestore.getInstance())
)
