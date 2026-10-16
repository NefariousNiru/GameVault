package com.gamevault.app.ui.library

import com.gamevault.app.domain.model.GamePlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDisplayPreferencesTest {
    @Test fun defaultsAreUsefulOnFreshInstall() {
        val settings = LocalDisplayPreferences()
        assertEquals("library", settings.route)
        assertEquals(GamePlatform.PC, settings.platform)
        assertTrue(settings.pc.grid)
        assertEquals("All", settings.pc.filter)
        assertEquals(GameSort.TITLE_ASC, settings.pc.sort)
        assertEquals("", settings.pc.query)
    }

    @Test fun pcAndPs5SelectionsRemainIndependent() {
        val pc = LibraryDisplayPreferences(
            query = "khazan", grid = false, filter = "Playing", sort = GameSort.RELEASE_DESC
        )
        val ps5 = LibraryDisplayPreferences(
            query = "control", grid = true, filter = "Wishlist", sort = GameSort.ADDED_DESC
        )
        val settings = LocalDisplayPreferences()
            .withPlatformPreferences(GamePlatform.PC, pc)
            .withPlatformPreferences(GamePlatform.PS5, ps5)
            .copy(platform = GamePlatform.PS5, route = "queue")
        assertEquals(pc, settings.forPlatform(GamePlatform.PC))
        assertEquals(ps5, settings.forPlatform(GamePlatform.PS5))
        assertEquals("queue", settings.route)
        assertFalse(settings.forPlatform(GamePlatform.PC).grid)
        assertTrue(settings.forPlatform(GamePlatform.PS5).grid)
    }

    @Test fun invalidDiskValuesFallBackToKnownOptions() {
        assertEquals("All", restoredLibraryFilter("unknown-filter"))
        assertEquals("Playing", restoredLibraryFilter("Playing"))
        assertEquals(GameSort.TITLE_ASC, restoredLibrarySort("OLD_SORT_VALUE"))
        assertEquals(GameSort.TITLE_DESC, restoredLibrarySort("TITLE_DESC"))
        assertEquals("library", restoredLibraryRoute("game-detail"))
        assertEquals("settings", restoredLibraryRoute("settings"))
        assertEquals(GamePlatform.PC, restoredLibraryPlatform("unsupported"))
        assertEquals(GamePlatform.PS5, restoredLibraryPlatform("PS5"))
    }
}
