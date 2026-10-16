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

    // A catalog ID is unique even if its display title changes or differs by region.
    private fun catalogClaim(uid: String, id: Int) =
        db.collection("users").document(uid).collection("catalogClaims").document(id.toString())

    override suspend fun create(userId: String, draft: GameDraft) {
        requireValid(draft)
        val key = titleKey(draft.title)
        val gameDoc = games(userId).document(UUID.randomUUID().toString())
        val title = titles(userId).document(key)
        val rawg = draft.rawgId?.let { catalogClaim(userId, it) }
        db.runTransaction { tx ->
            if (tx.get(title).exists()) throw DuplicateGameException()
            if (rawg != null && tx.get(rawg).exists()) throw DuplicateGameException()
            tx.set(title, mapOf("gameId" to gameDoc.id, "normalizedTitle" to normalizeTitle(draft.title)))
            if (rawg != null) tx.set(rawg, mapOf("gameId" to gameDoc.id, "rawgId" to draft.rawgId))
            tx.set(gameDoc, draft.fields(gameDoc.id, key))
        }.await()
    }

    override suspend fun update(userId: String, game: Game, draft: GameDraft) {
        requireValid(draft)
        val newKey = titleKey(draft.title)
        val gameDoc = games(userId).document(game.id)
        val sameTitle = newKey == game.titleKey
        val sameCatalog = game.draft.rawgId == draft.rawgId
        if (sameTitle && sameCatalog) {
            // Normal field changes may queue offline; claim-changing edits require transactions.
            gameDoc.set(draft.fields(game.id, newKey)).await()
            return
        }
        val oldTitle = titles(userId).document(game.titleKey)
        val newTitle = titles(userId).document(newKey)
        val oldCatalog = game.draft.rawgId?.let { catalogClaim(userId, it) }
        val newCatalog = draft.rawgId?.let { catalogClaim(userId, it) }
        db.runTransaction { tx ->
            val actual = tx.get(gameDoc)
            check(actual.exists()) { "Game was removed from another device" }
            check(actual.getString("titleKey") == game.titleKey && actual.getLong("rawgId")?.toInt() == game.draft.rawgId) {
                "Game was updated on another device; refresh and retry"
            }
            if (!sameTitle) {
                check(tx.get(oldTitle).getString("gameId") == game.id) { "Title index changed" }
                if (tx.get(newTitle).exists()) throw DuplicateGameException()
            }
            if (!sameCatalog) {
                if (oldCatalog != null) check(tx.get(oldCatalog).getString("gameId") == game.id) { "Catalog index changed" }
                if (newCatalog != null && tx.get(newCatalog).exists()) throw DuplicateGameException()
            }
            if (!sameTitle) {
                tx.set(newTitle, mapOf("gameId" to game.id, "normalizedTitle" to normalizeTitle(draft.title)))
                tx.delete(oldTitle)
            }
            if (!sameCatalog) {
                if (newCatalog != null) tx.set(newCatalog, mapOf("gameId" to game.id, "rawgId" to draft.rawgId))
                if (oldCatalog != null) tx.delete(oldCatalog)
            }
            tx.set(gameDoc, draft.fields(game.id, newKey))
        }.await()
    }

    override suspend fun delete(userId: String, game: Game) {
        val gameDoc = games(userId).document(game.id)
        val claim = titles(userId).document(game.titleKey)
        val catalog = game.draft.rawgId?.let { catalogClaim(userId, it) }
        db.runTransaction { tx ->
            val actual = tx.get(gameDoc)
            if (!actual.exists()) return@runTransaction
            check(actual.getString("titleKey") == game.titleKey && actual.getLong("rawgId")?.toInt() == game.draft.rawgId) {
                "Game was updated on another device; refresh and retry"
            }
            check(tx.get(claim).getString("gameId") == game.id) { "Title index conflict" }
            if (catalog != null) check(tx.get(catalog).getString("gameId") == game.id) { "Catalog index conflict" }
            tx.delete(gameDoc)
            tx.delete(claim)
            if (catalog != null) tx.delete(catalog)
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
    "rawgId" to rawgId, "rawgSlug" to rawgSlug,
    "developer" to developer, "publisher" to publisher,
    "description" to description, "genres" to genres, "tags" to tags,
    "supportedPlatforms" to supportedPlatforms, "rawgRating" to rawgRating,
    "steamGridId" to steamGridId,
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
            queueRank = getLong("queueRank"),
            rawgId = getLong("rawgId")?.toInt(),
            rawgSlug = getString("rawgSlug") ?: "",
            developer = getString("developer") ?: "",
            publisher = getString("publisher") ?: "",
            description = getString("description") ?: "",
            genres = getString("genres") ?: "",
            tags = getString("tags") ?: "",
            supportedPlatforms = getString("supportedPlatforms") ?: "",
            rawgRating = getDouble("rawgRating") ?: getLong("rawgRating")?.toDouble(),
            steamGridId = getLong("steamGridId")?.toInt()
        )
    )
}
