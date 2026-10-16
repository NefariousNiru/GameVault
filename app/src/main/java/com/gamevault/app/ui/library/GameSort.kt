package com.gamevault.app.ui.library

import com.gamevault.app.domain.game.Game
import java.time.LocalDate
import java.util.Locale

enum class GameSort(val label: String) {
    TITLE_ASC("A–Z"),
    TITLE_DESC("Z–A"),
    ADDED_DESC("Recent ↓"),
    ADDED_ASC("Recent ↑"),
    RELEASE_DESC("Release ↓"),
    RELEASE_ASC("Release ↑")
}

/** Six deterministic sort orders; missing or malformed release dates always come last. */
fun sortedGames(games: List<Game>, mode: GameSort): List<Game> = when (mode) {
    GameSort.TITLE_ASC -> games.sortedWith(compareBy<Game> { it.draft.title.lowercase(Locale.ROOT) }.thenBy { it.id })
    GameSort.TITLE_DESC -> games.sortedWith(compareByDescending<Game> { it.draft.title.lowercase(Locale.ROOT) }.thenBy { it.id })
    GameSort.ADDED_DESC -> games.sortedWith(compareByDescending<Game> { it.createdAtMillis }.thenBy { it.id })
    GameSort.ADDED_ASC -> games.sortedWith(compareBy<Game> { it.createdAtMillis }.thenBy { it.id })
    GameSort.RELEASE_DESC, GameSort.RELEASE_ASC -> games.sortedWith(Comparator { a, b ->
        val first = parseDate(a.draft.releaseDate)
        val second = parseDate(b.draft.releaseDate)
        when {
            first == null && second == null -> a.id.compareTo(b.id)
            first == null -> 1
            second == null -> -1
            mode == GameSort.RELEASE_DESC -> second.compareTo(first).takeIf { it != 0 } ?: a.id.compareTo(b.id)
            else -> first.compareTo(second).takeIf { it != 0 } ?: a.id.compareTo(b.id)
        }
    })
}

private fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()
