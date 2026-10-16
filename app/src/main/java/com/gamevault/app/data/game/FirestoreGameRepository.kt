package com.gamevault.app.data.game

import com.gamevault.app.domain.game.*
import android.content.Context
import android.util.Log
import com.gamevault.app.platform.NetworkStatus
import com.google.android.gms.tasks.Task
import com.gamevault.app.domain.model.GamePlatform
import com.google.firebase.firestore.Source
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
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
class FirestoreGameRepository(
    private val db: FirebaseFirestore,
    private val context: Context? = null
) : GameRepository {
    private suspend fun Task<Void>.finishWrite() {
        if (context != null && !NetworkStatus.isConnected(context)) {
            // Firestore persists the pending write and replays it after reconnection.
            addOnFailureListener { Log.e("GameVaultSync", "Deferred write rejected", it) }
        } else await()
    }
    private val activeSets = ConcurrentHashMap<String, String>()
    private fun set(uid: String): String = activeSets[uid] ?: "main"
    private fun parent(uid: String) = db.collection("users").document(uid)
        .collection("librarySets").document(set(uid))
    private fun games(uid: String) = parent(uid).collection("games")
    private fun shared(id: Int) = db.collection("gameCatalog").document(id.toString())
    private val cachedCatalog = ConcurrentHashMap<Int, Map<String, Any?>>()
    private fun titles(uid: String) = parent(uid).collection("titleClaims")
    private fun queueOrder(uid: String) = parent(uid).collection("queueOrder").document("main")

    private fun activeSetFlow(uid: String): Flow<String> = callbackFlow {
        val ref = parentRoot(uid)
        val registration = ref.addSnapshotListener { snapshot, error ->
            if (error != null) close(error)
            else {
                val id = snapshot?.getString("activeSetId") ?: "main"
                activeSets[uid] = id
                trySend(id)
            }
        }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    private fun parentRoot(uid: String) = db.collection("users").document(uid)
        .collection("libraryState").document("active")

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeQueueOrder(userId: String): Flow<List<String>> = activeSetFlow(userId).flatMapLatest { setId ->
        activeSets[userId] = setId
        callbackFlow {
        val registration = queueOrder(userId).addSnapshotListener { snapshot, error ->
            if (error != null) close(error)
            else trySend(snapshot?.get("order")?.let { value ->
                (value as? List<*>)?.filterIsInstance<String>()
            } ?: emptyList())
        }
        awaitClose { registration.remove() }
        }
    }

    /**
     * Firestore keeps only user data in private game documents for catalog-linked
     * games. Immutable RAWG details live in gameCatalog/{rawgId}, shared by all.
     * Each catalog-linked game resolves immutable fields from the shared catalog cache.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeGames(userId: String): Flow<List<Game>> = activeSetFlow(userId).flatMapLatest { setId ->
        activeSets[userId] = setId
        callbackFlow {
        val registration = games(userId).addSnapshotListener { snapshot, error ->
            if (error != null) close(error)
            else if (snapshot != null) trySend(snapshot.documents)
        }
        awaitClose { registration.remove() }
        }
    }.mapLatest { documents ->
        val base = documents.mapNotNull { it.toGame() }
        coroutineScope {
            base.map { game -> async {
                val id = game.draft.rawgId ?: return@async game
                val data = cachedCatalog[id] ?: try {
                    shared(id).get(Source.DEFAULT).await().data?.also { cachedCatalog[id] = it }
                } catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { null }
                if (data == null) game else game.copy(draft = game.draft.fromShared(data))
            } }.awaitAll().sortedWith(compareBy<Game> { it.draft.title.lowercase() }.thenBy { it.id })
        }
    }

    // A catalog ID is unique even if its display title changes or differs by region.
    private fun catalogClaim(uid: String, id: Int) =
        parent(uid).collection("catalogClaims").document(id.toString())

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
            tx.set(gameDoc, draft.firestoreFields(gameDoc.id, key))
        }.await()
    }

    override suspend fun update(userId: String, game: Game, draft: GameDraft) {
        requireValid(draft)
        val newKey = titleKey(draft.title)
        val gameDoc = games(userId).document(game.id)
        val sameTitle = newKey == game.titleKey
        val sameCatalog = game.draft.rawgId == draft.rawgId
        if (sameTitle && sameCatalog) {
            // Avoid a network read for metadata that is already immutable.
            // Firestore queues this user-owned write in persistent offline storage.
            gameDoc.set(draft.firestoreFields(game.id, newKey, game.createdAtMillis)).finishWrite()
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
            tx.set(gameDoc, draft.firestoreFields(game.id, newKey, game.createdAtMillis))
        }.await()
    }

    override suspend fun delete(userId: String, game: Game) {
        val gameDoc = games(userId).document(game.id)
        val claim = titles(userId).document(game.titleKey)
        val catalog = game.draft.rawgId?.let { catalogClaim(userId, it) }
        val orderRef = queueOrder(userId)
        db.runTransaction { tx ->
            val actual = tx.get(gameDoc)
            val orderSnap = tx.get(orderRef)
            if (!actual.exists()) return@runTransaction
            check(actual.getString("titleKey") == game.titleKey && actual.getLong("rawgId")?.toInt() == game.draft.rawgId) {
                "Game was updated on another device; refresh and retry"
            }
            check(tx.get(claim).getString("gameId") == game.id) { "Title index conflict" }
            if (catalog != null) check(tx.get(catalog).getString("gameId") == game.id) { "Catalog index conflict" }
            tx.delete(gameDoc)
            tx.delete(claim)
            if (catalog != null) tx.delete(catalog)
            if (orderSnap.exists()) {
                val order = (orderSnap.get("order") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                if (game.id in order) tx.set(orderRef, mapOf("order" to order.filterNot { it == game.id }))
            }
        }.await()
    }

    /**
     * The per-game queueRank indicates membership; queueOrder/main holds the
     * actual order. Both are changed in one transaction when membership changes.
     */
    override suspend fun setQueued(userId: String, gameId: String, queued: Boolean) {
        val counter = parent(userId).collection("queueState").document("main")
        val orderRef = queueOrder(userId)
        val gameDoc = games(userId).document(gameId)
        db.runTransaction { tx ->
            val counterSnapshot = tx.get(counter)
            val orderSnapshot = tx.get(orderRef)
            val gameSnapshot = tx.get(gameDoc)
            require(gameSnapshot.exists()) { "Game no longer exists" }
            val oldOrder = (orderSnapshot.get("order") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val existingRank = gameSnapshot.getLong("queueRank")
            if (queued && existingRank == null) {
                val next = counterSnapshot.getLong("nextRank") ?: 0L
                require(next < Long.MAX_VALUE) { "Queue capacity exceeded" }
                require(oldOrder.size < 500) { "Queue capacity exceeded" }
                tx.set(counter, mapOf("nextRank" to next + 1L))
                tx.update(gameDoc, mapOf("queueRank" to next, "updatedAtMillis" to System.currentTimeMillis()))
                tx.set(orderRef, mapOf("order" to (oldOrder + gameId)))
            } else if (!queued && existingRank != null) {
                tx.update(gameDoc, mapOf("queueRank" to null, "updatedAtMillis" to System.currentTimeMillis()))
                if (orderSnapshot.exists()) tx.set(orderRef, mapOf("order" to oldOrder.filterNot { it == gameId }))
            }
        }.await()
    }

    /** One-document atomic reorder; checks concurrent additions/removals through the order doc. */
    override suspend fun reorderQueue(userId: String, orderedGameIds: List<String>) {
        require(orderedGameIds.size <= 500) { "Queue capacity exceeded" }
        require(orderedGameIds.size == orderedGameIds.distinct().size) { "Duplicate queue entries" }
        if (orderedGameIds.isEmpty()) return
        val orderRef = queueOrder(userId)
        db.runTransaction { tx ->
            val snapshot = tx.get(orderRef)
            val current = (snapshot.get("order") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            require(!snapshot.exists() || orderedGameIds.toSet() == current.toSet()) {
                "Queue changed on another device. Refresh and try again."
            }
            tx.set(orderRef, mapOf("order" to orderedGameIds))
        }.await()
    }

    override suspend fun updateStatus(userId: String, gameId: String, field: QuickStatusField, value: String) {
        require(field.isAllowed(value)) { "Unsupported status" }
        val changes = mutableMapOf<String, Any?>(
            field.firestoreKey to value,
            "updatedAtMillis" to System.currentTimeMillis()
        )
        if (field == QuickStatusField.PS5_ACCESS) {
            // PS5 access and the legacy PS5 library field stay consistent.
            changes["ps5Library"] = if (value == Ps5Acquisition.NOT_OWNED.name ||
                value == Ps5Acquisition.PREORDERED.name) Ps5Library.NOT_ADDED.name
            else Ps5Library.IN_LIBRARY.name
        }
        games(userId).document(gameId).update(changes).finishWrite()
    }

    private fun requireValid(draft: GameDraft) {
        draft.validationError()?.let { throw IllegalArgumentException(it) }
    }

    /** SHA-256 is a stable Firestore document ID for a normalized title. */
    private fun titleKey(title: String): String = titleClaimKey(title)
}

/** Fields owned by an individual user; RAWG metadata is never duplicated for newly-linked games. */
internal fun GameDraft.firestoreFields(id: String, key: String, createdAt: Long = System.currentTimeMillis()): Map<String, Any?> {
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
internal fun GameDraft.catalogFields(): Map<String, Any?> = mapOf(
    "rawgId" to requireNotNull(rawgId), "title" to title.trim(), "rawgSlug" to rawgSlug,
    "releaseDate" to releaseDate, "developer" to developer, "publisher" to publisher,
    "description" to description, "genres" to genres, "tags" to tags,
    "supportedPlatforms" to supportedPlatforms, "rawgRating" to rawgRating
)

internal fun GameDraft.fromShared(data: Map<String, Any?>): GameDraft = copy(
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

internal fun DocumentSnapshot.toGame(): Game? {
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

/** Deterministic title index reused by safe restore staging. */
internal fun titleClaimKey(title: String): String = MessageDigest.getInstance("SHA-256")
    .digest(normalizeTitle(title).toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }
