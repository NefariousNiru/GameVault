package com.gamevault.app

import com.gamevault.app.domain.model.GamePlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class GamePlatformTest {
    @Test fun platformsHaveDistinctIdentities() {
        assertNotEquals(GamePlatform.PC, GamePlatform.PS5)
        assertEquals("PC", GamePlatform.PC.label)
        assertEquals("PS5", GamePlatform.PS5.label)
    }
}
