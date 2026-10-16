package com.gamevault.app.data.game

import com.gamevault.app.domain.game.*
import com.gamevault.app.domain.model.GamePlatform
import com.google.firebase.firestore.Source
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.mapLatest
import java.util.concurrent.ConcurrentHashMap
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
    private fun shared(id: Int) = db.collection("gameCatalog").document(id.toString())
    private val cachedCatalog = ConcurrentHashMap<Int, Map<String, Any?>>()
    private fun titles(uid: String) = db.collection("users").document(uid).collection("titleClaims")

    /**
     * Firestore keeps only user data in private game documents for catalog-linked
     * games. Immutable RAWG details live in gameCatalog/{rawgId}, shared by all.
     * Legacy records still load using their embedded metadata until migrated.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeGames(userId: String): Flow<List<Game>> = callbackFlow {
        val registration = games(userId).addSnapshotListener { snapshot, error ->
            if (error != null) close(error)
            else if (snapshot != null) trySend(snapshot.documents)
        }
        awaitClose { registration.remove() }
    }.mapLatest { documents ->
        val base = documents.mapNotNull { it.toGame() }
        coroutineScope {
            base.map { game -> async {
                val id = game.draft.rawgId ?: return@async game
                val data = cachedCatalog[id] ?: try {
                    shared(id).get(Source.DEFAULT).await().data?.also { cachedCatalog[id] = it }
                } catch (_: Exception) { null }
                if (data == null) game else game.copy(draft = game.draft.fromShared(data))
            } }.awaitAll().sortedWith(compareBy<Game> { it.draft.title.lowercase() }.thenBy { it.id })
        }
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
        val sharedRef = draft.rawgId?.let(::shared)
        db.runTransaction { tx ->
            val existingShared = sharedRef?.let(tx::get)
            if (tx.get(title).exists()) throw DuplicateGameException()
            if (rawg != null && tx.get(rawg).exists()) throw DuplicateGameException()
            tx.set(title, mapOf("gameId" to gameDoc.id, "normalizedTitle" to normalizeTitle(draft.title)))
            if (rawg != null) tx.set(rawg, mapOf("gameId" to gameDoc.id, "rawgId" to draft.rawgId))
            if (sharedRef != null && existingShared != null && !existingShared.exists()) tx.set(sharedRef, draft.catalogFields())
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
            // Keep existing user metadata in older documents until the first catalog migration.
            val sharedRef = draft.rawgId?.let(::shared)
            if (sharedRef != null && !sharedRef.get(Source.DEFAULT).await().exists()) {
                db.runTransaction { tx ->
                    if (!tx.get(sharedRef).exists()) tx.set(sharedRef, draft.catalogFields())
                    tx.set(gameDoc, draft.fields(game.id, newKey, game.createdAtMillis))
                }.await()
            } else gameDoc.set(draft.fields(game.id, newKey, game.createdAtMillis)).await()
            return
        }
        val oldTitle = titles(userId).document(game.titleKey)
        val newTitle = titles(userId).document(newKey)
        val oldCatalog = game.draft.rawgId?.let { catalogClaim(userId, it) }
        val newCatalog = draft.rawgId?.let { catalogClaim(userId, it) }
        val sharedRef = draft.rawgId?.let(::shared)
        db.runTransaction { tx ->
            val existingShared = sharedRef?.let(tx::get)
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
            if (sharedRef != null && existingShared != null && !existingShared.exists()) tx.set(sharedRef, draft.catalogFields())
            tx.set(gameDoc, draft.fields(game.id, newKey, game.createdAtMillis))
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

/** Fields owned by an individual user; RAWG metadata is never duplicated for newly-linked games. */
private fun GameDraft.fields(id: String, key: String, createdAt: Long = System.currentTimeMillis()): Map<String, Any?> {
    val fields = mutableMapOf<String, Any?>(
        "id" to id, "titleKey" to key,
        "platform" to platform.name, "release" to release.name, "interest" to interest.name,
        "ps5Acquisition" to ps5Acquisition.name, "ps5Library" to ps5Library.name,
        "pcAcquisition" to pcAcquisition.name, "gameplay" to gameplay.name,
        "notes" to notes, "rating" to rating,
        "completionDate" to completionDate.trim(), "coverUrl" to coverUrl.trim(),
        "heroUrl" to heroUrl.trim(), "queueRank" to queueRank,
        "rawgId" to rawgId, "steamGridId" to steamGridId,
        "updatedAtMillis" to System.currentTimeMillis(), "createdAtMillis" to createdAt
    )
    if (rawgId == null) {
        fields += mapOf("title" to title.trim(), "releaseDate" to releaseDate.trim(),
            "rawgSlug" to rawgSlug, "developer" to developer, "publisher" to publisher,
            "description" to description, "genres" to genres, "tags" to tags,
            "supportedPlatforms" to supportedPlatforms, "rawgRating" to rawgRating)
    }
    return fields
}

/** Shared catalog fields may only be created, never updated from the client. */
private fun GameDraft.catalogFields(): Map<String, Any?> = mapOf(
    "rawgId" to requireNotNull(rawgId), "title" to title.trim(), "rawgSlug" to rawgSlug,
    "releaseDate" to releaseDate, "developer" to developer, "publisher" to publisher,
    "description" to description, "genres" to genres, "tags" to tags,
    "supportedPlatforms" to supportedPlatforms, "rawgRating" to rawgRating
)

private fun GameDraft.fromShared(data: Map<String, Any?>): GameDraft = copy(
    title = data["title"] as? String ?: title,
    rawgSlug = data["rawgSlug"] as? String ?: rawgSlug,
    releaseDate = data["releaseDate"] as? String ?: releaseDate,
    developer = data["developer"] as? String ?: developer,
    publisher = data["publisher"] as? String ?: publisher,
    description = data["description"] as? String ?: description,
    genres = data["genres"] as? String ?: genres,
    tags = data["tags"] as? String ?: tags,
    supportedPlatforms = data["supportedPlatforms"] as? String ?: supportedPlatforms,
    rawgRating = (data["rawgRating"] as? Number)?.toDouble() ?: rawgRating
)

private inline fun <reified T : Enum<T>> DocumentSnapshot.readEnum(field: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == getString(field) } ?: fallback

private fun DocumentSnapshot.toGame(): Game? {
    val title = getString("title") ?: "" // Shared RAWG title resolved through gameCatalog/{rawgId}.
    return Game(
        id = id,
        titleKey = getString("titleKey") ?: return null,
        updatedAtMillis = getLong("updatedAtMillis") ?: 0L,
        createdAtMillis = getLong("createdAtMillis") ?: getLong("updatedAtMillis") ?: 0L,
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
