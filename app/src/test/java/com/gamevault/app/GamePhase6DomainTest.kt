package com.gamevault.app

import com.gamevault.app.domain.game.GameDraft
import com.gamevault.app.domain.game.QuickStatusField
import com.gamevault.app.domain.game.isAllowed
import com.gamevault.app.domain.game.moveQueuedId
import com.gamevault.app.domain.game.validationError
import org.junit.Assert.*
import org.junit.Test

class GamePhase6DomainTest {
    @Test fun reorderPreservesMembershipAndPosition() {
        assertEquals(listOf("b", "c", "a"), moveQueuedId(listOf("a", "b", "c"), 0, 2))
        assertEquals(listOf("c", "a", "b"), moveQueuedId(listOf("a", "b", "c"), 2, 0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun reorderedListCannotContainDuplicates() {
        moveQueuedId(listOf("a", "a"), 0, 1)
    }

    @Test fun quickStatusEnumsAreValidatedIndependently() {
        assertTrue(QuickStatusField.PLAY_STATUS.isAllowed("COMPLETED"))
        assertFalse(QuickStatusField.PLAY_STATUS.isAllowed("PS_PLUS"))
        assertTrue(QuickStatusField.PS5_ACCESS.isAllowed("PS_PLUS"))
        assertFalse(QuickStatusField.PC_DOWNLOAD.isAllowed("DROPPED"))
    }

}
