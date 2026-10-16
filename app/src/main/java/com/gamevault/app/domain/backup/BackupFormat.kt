package com.gamevault.app.domain.backup

import com.gamevault.app.domain.game.Game
import com.gamevault.app.domain.game.GameDraft
import com.gamevault.app.domain.game.normalizeTitle
import com.gamevault.app.domain.game.validationError
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import java.util.UUID
import java.time.LocalDate

private const val FORMAT_VERSION = 1
private const val MAX_GAMES = 500
private const val MAX_JSON_CHARS = 5_000_000

@Serializable
data class BackupEntry(
    val id: String,
    val draft: GameDraft,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Serializable
data class LibraryBackup(
    val schemaVersion: Int = FORMAT_VERSION,
    val exportedAtMillis: Long,
    val games: List<BackupEntry>,
    val queueOrder: List<String>
)

enum class ImportChoice { KEEP_CLOUD, USE_BACKUP }
enum class ImportMode { KEEP_CLOUD, MERGE, REPLACE }

data class ImportConflict(
    val key: String,
    val existingTitle: String,
    val importedTitle: String
)

object BackupCodec {
    private val json = Json { ignoreUnknownKeys = false; encodeDefaults = true; explicitNulls = true }

    fun export(games: List<Game>, queueOrder: List<String>): LibraryBackup {
        val backup = LibraryBackup(
            exportedAtMillis = System.currentTimeMillis(),
            games = games.map { BackupEntry(it.id, it.draft, it.createdAtMillis, it.updatedAtMillis) },
            queueOrder = run {
                val queued = games.filter { it.draft.queueRank != null }
                val membership = queued.map { it.id }.toSet()
                (queueOrder.filter { it in membership } + queued.sortedWith(
                    compareBy<Game> { it.draft.queueRank }.thenBy { it.id }
                ).map { it.id }).distinct()
            }
        )
        validate(backup)
        return backup
    }

    fun encode(backup: LibraryBackup): String {
        validate(backup)
        return json.encodeToString(backup).also {
            require(it.toByteArray(Charsets.UTF_8).size <= MAX_JSON_CHARS) { "Backup exceeds the 5 MB export limit" }
        }
    }

    fun decode(content: String): LibraryBackup {
        require(content.length <= MAX_JSON_CHARS) { "Backup file exceeds 5 MB" }
        val result = try {
            val root = json.parseToJsonElement(content).jsonObject
            require(root.containsKey("schemaVersion") && root.containsKey("games") && root.containsKey("queueOrder")) {
                "Missing required backup fields"
            }
            root.getValue("games").jsonArray.forEach { element ->
                val row = element.jsonObject
                require(row.containsKey("id") && row.containsKey("draft")) { "Missing game fields" }
                val draft = row.getValue("draft").jsonObject
                require(draft.keys.containsAll(listOf("title", "platform", "release", "interest", "gameplay"))) {
                    "A game is missing required statuses"
                }
            }
            json.decodeFromString<LibraryBackup>(content)
        }
        catch (error: SerializationException) { throw IllegalArgumentException("Invalid GameVault JSON: ${error.message}", error) }
        validate(result)
        return result
    }

    fun validate(backup: LibraryBackup) {
        require(backup.schemaVersion == FORMAT_VERSION) { "Unsupported backup version: ${backup.schemaVersion}" }
        require(backup.games.size <= MAX_GAMES) { "Backup exceeds the 500-game limit" }
        require(backup.exportedAtMillis > 0) { "Invalid export timestamp" }
        require(backup.games.map { it.id }.distinct().size == backup.games.size) { "Duplicate game IDs" }
        val uniqueTitles = mutableSetOf<String>()
        val uniqueRawg = mutableSetOf<Int>()
        backup.games.forEach { entry ->
            require(runCatching { UUID.fromString(entry.id) }.isSuccess) { "Invalid game identifier" }
            require(entry.createdAtMillis > 0 && entry.updatedAtMillis > 0) { "Invalid game timestamps" }
            require(entry.draft.validationError() == null) { "Invalid game '${entry.draft.title}': ${entry.draft.validationError()}" }
            listOf(entry.draft.releaseDate, entry.draft.completionDate)
                .filter { it.isNotBlank() }.forEach { date ->
                    require(runCatching { LocalDate.parse(date) }.isSuccess) {
                        "Invalid date in '${entry.draft.title}': expected YYYY-MM-DD"
                    }
                }
            require(uniqueTitles.add(normalizeTitle(entry.draft.title))) { "Duplicate game title '${entry.draft.title}'" }
            entry.draft.rawgId?.let { require(uniqueRawg.add(it)) { "Duplicate RAWG game ID" } }
        }
        val queued = backup.games.filter { it.draft.queueRank != null }.map { it.id }.toSet()
        require(backup.queueOrder.size == queued.size && backup.queueOrder.toSet() == queued) {
            "Queue order must contain every queued game exactly once"
        }
    }
}

fun conflicts(cloud: LibraryBackup, imported: LibraryBackup): List<ImportConflict> {
    val byName = cloud.games.associateBy { normalizeTitle(it.draft.title) }
    val byRawg = cloud.games.mapNotNull { item -> item.draft.rawgId?.let { it to item } }.toMap()
    return imported.games.mapNotNull { incoming ->
        val match = incoming.draft.rawgId?.let(byRawg::get) ?: byName[normalizeTitle(incoming.draft.title)]
        match?.let { ImportConflict(incoming.id, it.draft.title, incoming.draft.title) }
    }
}

/** Resolves collisions by title OR RAWG identity; queue ordering remains independent of platform. */
fun mergeBackups(
    cloud: LibraryBackup,
    imported: LibraryBackup,
    choices: Map<String, ImportChoice>
): LibraryBackup {
    val selected = cloud.games.toMutableList()
    val replaces = mutableMapOf<String, String>()
    imported.games.forEach { incoming ->
        val existingIndex = selected.indexOfFirst { item ->
            normalizeTitle(item.draft.title) == normalizeTitle(incoming.draft.title) ||
                (item.draft.rawgId != null && item.draft.rawgId == incoming.draft.rawgId)
        }
        if (existingIndex < 0) selected += incoming
        else if (choices[incoming.id] == ImportChoice.USE_BACKUP) {
            replaces[selected[existingIndex].id] = incoming.id
            selected[existingIndex] = incoming
        }
    }
    val queueMembership = selected.filter { it.draft.queueRank != null }.map { it.id }.toSet()
    val ordered = (cloud.queueOrder.map { replaces[it] ?: it } + imported.queueOrder)
        .distinct().filter { it in queueMembership }
    val finalOrder = ordered + queueMembership.filterNot { it in ordered }.sorted()
    val result = LibraryBackup(exportedAtMillis = System.currentTimeMillis(), games = selected, queueOrder = finalOrder)
    BackupCodec.validate(result)
    return result
}
