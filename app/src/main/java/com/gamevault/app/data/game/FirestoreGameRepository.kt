package com.gamevault.app.data.game

import com.gamevault.app.domain.game.*
import com.gamevault.app.domain.model.GamePlatform
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

/**
 * User-owned Firestore repository. Title claims are reserved transactionally.
 * Transactions need network access; cached observation continues offline.
 */
class FirestoreGameRepository(private val db: FirebaseFirestore) : GameRepository {
    private fun games(uid: String) = db.collection("users").document(uid).collection("games")
    private fun titles(uid: String) = db.collection("users").document(uid).collection("titleClaims")

    override fun observeGames(userId: String): Flow<List<Game>> = callbackFlow {
        val registration = games(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
            } else if (snapshot != null) {
                trySend(snapshot.documents.mapNotNull { it.toGame() }
                    .sortedWith(compareBy<Game> { it.draft.title.lowercase() }.thenBy { it.id }))
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun create(userId: String, draft: GameDraft) {
        requireValid(draft)
        val key = titleKey(draft.title)
        val gameDoc = games(userId).document(UUID.randomUUID().toString())
        val claim = titles(userId).document(key)
        db.runTransaction { tx ->
            if (tx.get(claim).exists()) throw DuplicateGameException()
            tx.set(claim, mapOf("gameId" to gameDoc.id, "normalizedTitle" to normalizeTitle(draft.title)))
            tx.set(gameDoc, draft.fields(gameDoc.id, key))
        }.await()
    }

    override suspend fun update(userId: String, game: Game, draft: GameDraft) {
        requireValid(draft)
        val updatedKey = titleKey(draft.title)
        val gameDoc = games(userId).document(game.id)
        val oldClaim = titles(userId).document(game.titleKey)
        if (updatedKey == game.titleKey) {
            // A normal edit is queued by Firestore when offline.
            gameDoc.set(draft.fields(game.id, updatedKey)).await()
            return
        }
        val newClaim = titles(userId).document(updatedKey)
        db.runTransaction { tx ->
            check(tx.get(gameDoc).exists()) { "Game was removed from another device" }
            check(tx.get(oldClaim).getString("gameId") == game.id) { "Game title index changed" }
            if (tx.get(newClaim).exists()) throw DuplicateGameException()
            tx.set(newClaim, mapOf("gameId" to game.id, "normalizedTitle" to normalizeTitle(draft.title)))
            tx.set(gameDoc, draft.fields(game.id, updatedKey))
            tx.delete(oldClaim)
        }.await()
    }

    override suspend fun delete(userId: String, game: Game) {
        val gameDoc = games(userId).document(game.id)
        val claim = titles(userId).document(game.titleKey)
        db.runTransaction { tx ->
            val existingGame = tx.get(gameDoc)
            val existingClaim = tx.get(claim)
            if (!existingGame.exists()) return@runTransaction
            check(existingClaim.getString("gameId") == game.id) { "Title index conflict" }
            tx.delete(gameDoc)
            tx.delete(claim)
        }.await()
    }

    private fun requireValid(draft: GameDraft) {
        draft.validationError()?.let { throw IllegalArgumentException(it) }
    }

    /** SHA-256 is a stable Firestore document ID for a normalized title. */
    private fun titleKey(title: String): String = MessageDigest.getInstance("SHA-256")
        .digest(normalizeTitle(title).toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

private fun GameDraft.fields(id: String, key: String): Map<String, Any?> = mapOf(
    "id" to id, "titleKey" to key, "title" to title.trim(),
    "platform" to platform.name, "release" to release.name, "interest" to interest.name,
    "ps5Acquisition" to ps5Acquisition.name, "ps5Library" to ps5Library.name,
    "pcAcquisition" to pcAcquisition.name, "gameplay" to gameplay.name,
    "releaseDate" to releaseDate.trim(), "notes" to notes, "rating" to rating,
    "completionDate" to completionDate.trim(), "coverUrl" to coverUrl.trim(),
    "heroUrl" to heroUrl.trim(), "queueRank" to queueRank,
    "updatedAtMillis" to System.currentTimeMillis()
)

private inline fun <reified T : Enum<T>> DocumentSnapshot.readEnum(field: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == getString(field) } ?: fallback

private fun DocumentSnapshot.toGame(): Game? {
    val title = getString("title") ?: return null
    return Game(
        id = id,
        titleKey = getString("titleKey") ?: return null,
        updatedAtMillis = getLong("updatedAtMillis") ?: 0L,
        draft = GameDraft(
            title = title,
            platform = readEnum("platform", GamePlatform.PC),
            release = readEnum("release", ReleaseStatus.TBA),
            interest = readEnum("interest", InterestStatus.WISHLIST),
            ps5Acquisition = readEnum("ps5Acquisition", Ps5Acquisition.NOT_OWNED),
            ps5Library = readEnum("ps5Library", Ps5Library.NOT_ADDED),
            pcAcquisition = readEnum("pcAcquisition", PcAcquisition.NOT_DOWNLOADED),
            gameplay = readEnum("gameplay", GameplayStatus.NOT_STARTED),
            releaseDate = getString("releaseDate") ?: "",
            notes = getString("notes") ?: "",
            rating = getLong("rating")?.toInt(),
            completionDate = getString("completionDate") ?: "",
            coverUrl = getString("coverUrl") ?: "",
            heroUrl = getString("heroUrl") ?: "",
            queueRank = getLong("queueRank")
        )
    )
}
