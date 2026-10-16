package com.gamevault.app

import com.gamevault.app.domain.backup.*
import com.gamevault.app.domain.game.GameDraft
import com.gamevault.app.domain.game.Game
import com.gamevault.app.domain.game.GameplayStatus
import com.gamevault.app.domain.model.GamePlatform
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class BackupCodecTest {
    private fun game(title: String, rawg: Int? = null, queued: Boolean = false): BackupEntry =
        BackupEntry(
            UUID.randomUUID().toString(),
            GameDraft(title = title, platform = GamePlatform.PS5, rawgId = rawg,
                gameplay = GameplayStatus.PLAYING, queueRank = if (queued) 1 else null),
            100L, 101L
        )

    @Test fun backupRoundtripRetainsArtworkAndStatuses() {
        val entry = game("Elden Ring", 42, true).copy(
            draft = GameDraft(title = "Elden Ring", rawgId = 42,
                coverUrl = "https://cdn.example.com/cover.jpg",
                heroUrl = "https://cdn.example.com/hero.jpg",
                gameplay = GameplayStatus.COMPLETED,
                notes = "Final boss", queueRank = 3L)
        )
        val backup = LibraryBackup(exportedAtMillis = 1000L, games = listOf(entry), queueOrder = listOf(entry.id))
        assertEquals(backup, BackupCodec.decode(BackupCodec.encode(backup)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidImportRejectsDuplicateTitles() {
        BackupCodec.validate(LibraryBackup(
            exportedAtMillis = 1000L,
            games = listOf(game("  ELDEN RING "), game("Elden Ring")), queueOrder = emptyList()
        ))
    }

    @Test(expected = IllegalArgumentException::class)
    fun brokenQueueReferencesAreRejected() {
        BackupCodec.validate(LibraryBackup(
            exportedAtMillis = 1000L, games = listOf(game("Game", queued = true)), queueOrder = emptyList()
        ))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownSchemaIsRejected() {
        BackupCodec.decode("""{"schemaVersion":2,"exportedAtMillis":1,"games":[],"queueOrder":[]}""")
    }

    @Test fun mergeConflictKeepsCloudUnlessChosenOtherwise() {
        val cloudGame = game("Elden Ring", 42).copy(draft = GameDraft(title = "Elden Ring", rawgId = 42, notes = "cloud"))
        val importedGame = game("Elden Ring", 42).copy(draft = GameDraft(title = "Elden Ring", rawgId = 42, notes = "backup"))
        val cloud = LibraryBackup(exportedAtMillis = 1000L, games = listOf(cloudGame), queueOrder = emptyList())
        val imported = LibraryBackup(exportedAtMillis = 1000L, games = listOf(importedGame), queueOrder = emptyList())
        assertEquals("cloud", mergeBackups(cloud, imported, emptyMap()).games.single().draft.notes)
        assertEquals("backup", mergeBackups(cloud, imported, mapOf(importedGame.id to ImportChoice.USE_BACKUP)).games.single().draft.notes)
    }
}
