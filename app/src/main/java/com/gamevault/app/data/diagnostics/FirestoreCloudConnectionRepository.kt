package com.gamevault.app.data.diagnostics

import com.gamevault.app.domain.diagnostics.CloudConnectionRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/** Verifies authenticated Firestore writes and a real server read. */
class FirestoreCloudConnectionRepository(
    private val firestore: FirebaseFirestore
) : CloudConnectionRepository {

    override suspend fun verify(userId: String): Boolean {
        require(userId.isNotBlank()) { "A signed-in user is required" }
        val document = firestore.collection("users")
            .document(userId)
            .collection("diagnostics")
            .document("connection")
        val nonce = java.util.UUID.randomUUID().toString()
        document.set(mapOf("nonce" to nonce)).await()
        val serverResult = document.get(Source.SERVER).await()
        return serverResult.getString("nonce") == nonce
    }
}
