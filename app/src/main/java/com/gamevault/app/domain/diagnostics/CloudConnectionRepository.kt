package com.gamevault.app.domain.diagnostics

/** A deliberate, server-backed Firestore read/write smoke test. */
interface CloudConnectionRepository {
    suspend fun verify(userId: String): Boolean
}
