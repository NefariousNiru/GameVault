package com.gamevault.app.domain.game

import kotlinx.coroutines.flow.Flow

/** Firestore is authoritative. A single user cannot have two titles with the same normalized name. */
interface GameRepository {
    fun observeGames(userId: String): Flow<List<Game>>
    suspend fun create(userId: String, draft: GameDraft)
    suspend fun update(userId: String, game: Game, draft: GameDraft)
    suspend fun delete(userId: String, game: Game)
}

class DuplicateGameException : IllegalArgumentException("This title is already in your collection")
