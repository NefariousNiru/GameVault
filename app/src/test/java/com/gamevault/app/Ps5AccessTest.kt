package com.gamevault.app

import com.gamevault.app.domain.game.GameDraft
import com.gamevault.app.domain.game.Ps5Acquisition
import com.gamevault.app.domain.game.Ps5Library
import com.gamevault.app.domain.model.GamePlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Ps5AccessTest {
    @Test fun allAccessTypesAreAvailableWithoutBreakingOldCollections() {
        assertEquals(Ps5Acquisition.PURCHASED, Ps5Acquisition.valueOf("PURCHASED"))
        assertTrue(Ps5Acquisition.entries.containsAll(listOf(
            Ps5Acquisition.DIGITAL,
            Ps5Acquisition.DISC,
            Ps5Acquisition.PS_PLUS,
            Ps5Acquisition.FREE_TO_PLAY
        )))
    }

    @Test fun switchingPlatformDoesNotDiscardPreviousPcOrPs5Fields() {
        val ps5 = GameDraft(platform = GamePlatform.PS5,
            ps5Acquisition = Ps5Acquisition.PS_PLUS,
            ps5Library = Ps5Library.IN_LIBRARY)
        val pc = ps5.copy(platform = GamePlatform.PC)
        assertEquals(Ps5Acquisition.PS_PLUS, pc.ps5Acquisition)
        assertEquals(Ps5Library.IN_LIBRARY, pc.ps5Library)
    }
}
