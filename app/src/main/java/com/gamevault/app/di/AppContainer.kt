package com.gamevault.app.di

import com.gamevault.app.data.backup.FirestoreBackupStore
import com.gamevault.app.data.artwork.ArtworkPrefetcher
import com.gamevault.app.data.auth.FirebaseGoogleAuthRepository
import com.gamevault.app.domain.auth.AuthRepository
import com.gamevault.app.data.game.FirestoreGameRepository
import com.gamevault.app.domain.game.GameRepository
import android.content.Context
import com.gamevault.app.data.catalog.ApiKeyOnboardingStore
import com.gamevault.app.data.catalog.ApiKeySettings
import com.gamevault.app.data.catalog.RemoteGameCatalogRepository
import com.gamevault.app.domain.catalog.GameCatalogRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** App-scoped composition root; replaceable dependencies for tests. */
class AppContainer(
    context: Context,
    val catalogRepository: GameCatalogRepository = RemoteGameCatalogRepository(),
    val apiKeySettings: ApiKeySettings = ApiKeySettings(context),
    val apiKeyOnboardingStore: ApiKeyOnboardingStore = ApiKeyOnboardingStore(context),
    val gameRepository: GameRepository = FirestoreGameRepository(FirebaseFirestore.getInstance(), context.applicationContext),
    val backupStore: FirestoreBackupStore = FirestoreBackupStore(FirebaseFirestore.getInstance(), context.applicationContext),
    val artworkPrefetcher: ArtworkPrefetcher = ArtworkPrefetcher(context.applicationContext),
    val authRepository: AuthRepository = FirebaseGoogleAuthRepository(FirebaseAuth.getInstance())
)
