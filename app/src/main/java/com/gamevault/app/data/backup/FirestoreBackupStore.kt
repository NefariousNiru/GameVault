package com.gamevault.app.data.backup

import android.content.Context
import com.gamevault.app.data.game.catalogFields
import com.gamevault.app.data.game.firestoreFields
import com.gamevault.app.data.game.fromShared
import com.gamevault.app.data.game.titleClaimKey
import com.gamevault.app.data.game.toGame
import com.gamevault.app.domain.backup.BackupCodec
import com.gamevault.app.domain.backup.ImportChoice
import com.gamevault.app.domain.backup.ImportMode
import com.gamevault.app.domain.backup.LibraryBackup
import com.gamevault.app.domain.backup.mergeBackups
import com.gamevault.app.domain.game.Game
import com.gamevault.app.domain.game.normalizeTitle
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Crash-safe restore: upload to a new inactive dataset, verify, then switch one
 * pointer transactionally. Failed staging never alters the active collection.
 */
class FirestoreBackupStore(
    private val db: FirebaseFirestore,
    private val appContext: Context
) {
    private fun user(uid: String) = db.collection("users").document(uid)
    private fun pointer(uid: String) = user(uid).collection("libraryState").document("active")
    private fun dataset(uid: String, id: String) = user(uid).collection("librarySets").document(id)
    private fun catalog(id: Int) = db.collection("gameCatalog").document(id.toString())

    suspend fun loadServer(uid: String): LibraryBackup {
        val set = pointer(uid).get(Source.SERVER).await().getString("activeSetId") ?: "main"
        return loadSet(uid, set)
    }

    private suspend fun loadSet(uid: String, setId: String): LibraryBackup {
        val gameDocs = dataset(uid, setId).collection("games").get(Source.SERVER).await()
        val orderDoc = dataset(uid, setId).collection("queueOrder").document("main")
            .get(Source.SERVER).await()
        val hydrated = gameDocs.documents.mapNotNull { snapshot ->
            val game = snapshot.toGame() ?: error("An existing game has invalid data")
            val data = game.draft.rawgId?.let { catalog(it).get(Source.SERVER).await().data }
            if (game.draft.rawgId != null && data == null) {
                error("Shared game information missing for '${game.draft.title}'")
            }
            if (data == null) game else game.copy(draft = game.draft.fromShared(data))
        }
        val order = (orderDoc.get("order") as? List<*>)?.filterIsInstance<String>().orEmpty()
        return BackupCodec.export(hydrated, order)
    }

    /** Returns the basename of the recoverable backup stored privately on the phone. */
    private fun persistRecovery(current: LibraryBackup): String {
        val directory = File(appContext.filesDir, "gamevault_recovery").apply { mkdirs() }
        val file = File(directory, "before-import-${System.currentTimeMillis()}.json")
        val bytes = BackupCodec.encode(current).toByteArray(Charsets.UTF_8)
        FileOutputStream(file).use { output ->
            output.write(bytes)
            output.fd.sync()
        }
        return file.name
    }

    fun latestRecovery(): String? = File(appContext.filesDir, "gamevault_recovery")
        .listFiles()?.filter { it.isFile && it.extension == "json" }
        ?.maxByOrNull { it.lastModified() }?.readText(Charsets.UTF_8)

    suspend fun restore(
        uid: String,
        backup: LibraryBackup,
        mode: ImportMode,
        choices: Map<String, ImportChoice>
    ): String {
        BackupCodec.validate(backup)
        if (mode == ImportMode.KEEP_CLOUD) return "Cloud library kept"

        // Cloud reads intentionally fail offline. Never stage against stale cached data.
        val start = pointer(uid).get(Source.SERVER).await().getString("activeSetId") ?: "main"
        val current = loadSet(uid, start)
        val target = when (mode) {
            ImportMode.REPLACE -> backup
            ImportMode.MERGE -> mergeBackups(current, backup, choices)
            ImportMode.KEEP_CLOUD -> current
        }
        BackupCodec.validate(target)
        val recoveryName = withContext(Dispatchers.IO) { persistRecovery(current) }
        val stageId = UUID.randomUUID().toString().replace("-", "")
        val base = dataset(uid, stageId)
        val catalogCache = mutableMapOf<Int, Map<String, Any?>>()

        // Existing canonical RAWG entries take precedence over imported catalog text.
        val canonical = target.games.map { entry ->
            val id = entry.draft.rawgId
            if (id == null) entry else {
                val data = catalogCache[id] ?: catalog(id).get(Source.SERVER).await().data
                if (data != null) {
                    catalogCache[id] = data
                    entry.copy(draft = entry.draft.fromShared(data))
                } else entry
            }
        }
        val staged = target.copy(games = canonical)
        BackupCodec.validate(staged)

        // Shared catalog metadata cannot be edited. Create missing IDs only.
        canonical.filter { it.draft.rawgId != null }.forEach { entry ->
            val id = requireNotNull(entry.draft.rawgId)
            if (id !in catalogCache) {
                try { catalog(id).set(entry.draft.catalogFields()).await() }
                catch (failure: Exception) {
                    // Concurrent first-write is fine if another user created it.
                    if (!catalog(id).get(Source.SERVER).await().exists()) throw failure
                }
                catalogCache[id] = requireNotNull(catalog(id).get(Source.SERVER).await().data)
            }
        }

        // Stage small independent atomic batches. Nothing under stageId is visible
        // until the active pointer changes. No existing games are deleted.
        staged.games.chunked(2).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { entry ->
                val gameDoc = base.collection("games").document(entry.id)
                val titleKey = titleClaimKey(entry.draft.title)
                batch.set(gameDoc, entry.draft.firestoreFields(entry.id, titleKey, entry.createdAtMillis))
                batch.set(base.collection("titleClaims").document(titleKey), mapOf(
                    "gameId" to entry.id,
                    "normalizedTitle" to normalizeTitle(entry.draft.title)
                ))
                entry.draft.rawgId?.let { rawgId ->
                    batch.set(base.collection("catalogClaims").document(rawgId.toString()), mapOf(
                        "gameId" to entry.id, "rawgId" to rawgId
                    ))
                }
            }
            batch.commit().await()
        }

        val extra = db.batch()
        extra.set(base.collection("queueOrder").document("main"), mapOf("order" to staged.queueOrder))
        extra.set(base.collection("queueState").document("main"), mapOf(
            "nextRank" to ((staged.games.mapNotNull { it.draft.queueRank }.maxOrNull() ?: -1L) + 1L)
                .coerceAtLeast(1L)
        ))
        extra.set(base, mapOf(
            "status" to "ready", "gameCount" to staged.games.size,
            "schemaVersion" to staged.schemaVersion,
            "createdAtMillis" to System.currentTimeMillis()
        ))
        extra.commit().await()

        val checkCount = base.collection("games").get(Source.SERVER).await().size()
        require(checkCount == staged.games.size) { "Restore staging incomplete. Original library unchanged." }
        db.runTransaction { tx ->
            val ref = pointer(uid)
            val live = tx.get(ref)
            val ready = tx.get(base)
            check(ready.getString("status") == "ready" && ready.getLong("gameCount") == staged.games.size.toLong()) {
                "Staged library is not ready"
            }
            check((live.getString("activeSetId") ?: "main") == start) {
                "Your library was replaced on another device; nothing was changed"
            }
            tx.set(ref, mapOf(
                "activeSetId" to stageId,
                "previousSetId" to start,
                "schemaVersion" to 1,
                "updatedAtMillis" to System.currentTimeMillis()
            ))
        }.await()
        return "Imported ${staged.games.size} games. Recovery: $recoveryName"
    }

    /** Flip back to the previously active dataset without deleting the current one. */
    suspend fun undoLastRestore(uid: String) {
        db.runTransaction { tx ->
            val ref = pointer(uid)
            val snap = tx.get(ref)
            val previous = snap.getString("previousSetId") ?: error("No previous library")
            val current = snap.getString("activeSetId") ?: error("No active library")
            tx.set(ref, mapOf(
                "activeSetId" to previous,
                "previousSetId" to current,
                "schemaVersion" to 1,
                "updatedAtMillis" to System.currentTimeMillis()
            ))
        }.await()
    }
}
