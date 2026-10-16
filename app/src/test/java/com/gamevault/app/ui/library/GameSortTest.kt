package com.gamevault.app.ui.library

import com.gamevault.app.domain.game.Game
import com.gamevault.app.domain.game.GameDraft
import org.junit.Assert.assertEquals
import org.junit.Test

class GameSortTest {
    private val games = listOf(
        game("z", "Zelda", "2017-03-03", 1L),
        game("a", "Alan Wake", "2010-05-14", 3L),
        game("m", "Missing release", "", 2L)
    )

    @Test fun titleAscendingAndDescending() {
        assertEquals(listOf("a", "m", "z"), sortedGames(games, GameSort.TITLE_ASC).map { it.id })
        assertEquals(listOf("z", "m", "a"), sortedGames(games, GameSort.TITLE_DESC).map { it.id })
    }

    @Test fun recentAscendingAndDescendingUseCreatedAtNotUpdatedAt() {
        assertEquals(listOf("a", "m", "z"), sortedGames(games, GameSort.ADDED_DESC).map { it.id })
        assertEquals(listOf("z", "m", "a"), sortedGames(games, GameSort.ADDED_ASC).map { it.id })
    }

    @Test fun releaseDatesPlaceUnknownLastForBothDirections() {
        assertEquals(listOf("z", "a", "m"), sortedGames(games, GameSort.RELEASE_DESC).map { it.id })
        assertEquals(listOf("a", "z", "m"), sortedGames(games, GameSort.RELEASE_ASC).map { it.id })
    }

    private fun game(id: String, name: String, date: String, added: Long) = Game(
        id, id, GameDraft(title = name, releaseDate = date), updatedAtMillis = 99L, createdAtMillis = added
    )
}
