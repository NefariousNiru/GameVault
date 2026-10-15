package com.gamevault.app

import com.gamevault.app.domain.game.*
import com.gamevault.app.domain.model.GamePlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GameDraftTest {
    @Test fun `title normalization deduplicates whitespace case and Unicode width`() {
        assertEquals("elden ring", normalizeTitle(" ELDEN  Ring "))
        assertEquals("final fantasy", normalizeTitle("ＦＩＮＡＬ  ＦＡＮＴＡＳＹ"))
    }

    @Test fun `statuses are independent and platform migration archives previous acquisition`() {
        val original = GameDraft(title = "Test", platform = GamePlatform.PC,
            pcAcquisition = PcAcquisition.INSTALLED, gameplay = GameplayStatus.COMPLETED)
        val onPs5 = original.copy(platform = GamePlatform.PS5)
        assertEquals(PcAcquisition.INSTALLED, onPs5.pcAcquisition)
        assertEquals(GameplayStatus.COMPLETED, onPs5.gameplay)
        assertEquals(Ps5Acquisition.NOT_OWNED, onPs5.ps5Acquisition)
    }

    @Test fun `input validation rejects unsupported artwork URL and out of range rating`() {
        assertNotNull(GameDraft(title = "Game", coverUrl = "http://bad.test/cover.jpg").validationError())
        assertNotNull(GameDraft(title = "Game", rating = 11).validationError())
        assertNull(GameDraft(title = "Game", heroUrl = "https://example.com/hero.jpg").validationError())
    }
}
