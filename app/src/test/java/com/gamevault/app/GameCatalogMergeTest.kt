package com.gamevault.app

import com.gamevault.app.domain.catalog.CatalogMetadata
import com.gamevault.app.domain.catalog.applyCatalog
import com.gamevault.app.domain.game.*
import com.gamevault.app.domain.model.GamePlatform
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class GameCatalogMergeTest {
    private val elden = CatalogMetadata(99, "elden-ring", "Elden Ring", "2022-02-25",
        "FromSoftware", "Bandai Namco", "Fantasy action RPG", "RPG", "Soulslike", "PC, PS5",
        4.5, "https://images.example/cover", "https://images.example/hero")

    @Test fun metadataNeverTouchesIndependentPersonalStatuses() {
        val prior = GameDraft(title = "Handwritten", platform = GamePlatform.PS5,
            gameplay = GameplayStatus.COMPLETED, interest = InterestStatus.DEFERRED,
            ps5Acquisition = Ps5Acquisition.PURCHASED, ps5Library = Ps5Library.IN_LIBRARY,
            pcAcquisition = PcAcquisition.INSTALLED, notes = "My notes", rating = 10,
            queueRank = 17L, completionDate = "2025-10-01")
        val result = prior.applyCatalog(elden, LocalDate.parse("2026-10-15"))
        assertEquals("Elden Ring", result.title)
        assertEquals(99, result.rawgId)
        assertEquals(GameplayStatus.COMPLETED, result.gameplay)
        assertEquals(InterestStatus.DEFERRED, result.interest)
        assertEquals(Ps5Acquisition.PURCHASED, result.ps5Acquisition)
        assertEquals(Ps5Library.IN_LIBRARY, result.ps5Library)
        assertEquals(PcAcquisition.INSTALLED, result.pcAcquisition)
        assertEquals(10, result.rating)
        assertEquals(17L, result.queueRank)
        assertEquals("My notes", result.notes)
        assertEquals("2025-10-01", result.completionDate)
        assertEquals(ReleaseStatus.RELEASED, result.release)
        assertNull(result.validationError())
    }

    @Test fun existingArtworkAndManuallyOverriddenReleaseSurviveRefresh() {
        val before = GameDraft(title = "Elden Ring", rawgId = 99,
            release = ReleaseStatus.ANNOUNCED,
            coverUrl = "https://user.example/cover", heroUrl = "https://user.example/hero")
        val result = before.applyCatalog(elden)
        assertEquals("https://user.example/cover", result.coverUrl)
        assertEquals("https://user.example/hero", result.heroUrl)
        assertEquals(ReleaseStatus.ANNOUNCED, result.release)
    }
}
