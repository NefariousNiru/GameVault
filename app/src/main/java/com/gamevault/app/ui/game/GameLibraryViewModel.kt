package com.gamevault.app.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gamevault.app.domain.game.Game
import com.gamevault.app.domain.game.GameDraft
import com.gamevault.app.domain.game.GameRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameLibraryUiState(
    val games: List<Game> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null
)

/** Shared between Library and Queue; no Firebase types leak into UI. */
class GameLibraryViewModel(private val repository: GameRepository) : ViewModel() {
    private val mutable = MutableStateFlow(GameLibraryUiState())
    val state: StateFlow<GameLibraryUiState> = mutable.asStateFlow()
    private var observedUid: String? = null
    private var subscription: Job? = null

    fun observe(uid: String) {
        if (uid == observedUid) return
        observedUid = uid
        subscription?.cancel()
        mutable.value = GameLibraryUiState()
        subscription = viewModelScope.launch {
            try {
                repository.observeGames(uid).collect { games ->
                    mutable.update { it.copy(games = games, loading = false, error = null) }
                }
            } catch (error: Exception) {
                mutable.update { it.copy(loading = false, error = error.message ?: "Could not load games") }
            }
        }
    }

    fun clearError() = mutable.update { it.copy(error = null) }

    fun save(game: Game?, draft: GameDraft, onSuccess: () -> Unit) {
        val uid = observedUid ?: return
        if (mutable.value.saving) return
        mutable.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                if (game == null) repository.create(uid, draft)
                else repository.update(uid, game, draft)
                onSuccess()
            } catch (error: Exception) {
                mutable.update { it.copy(error = error.message ?: "Could not save game") }
            } finally {
                mutable.update { it.copy(saving = false) }
            }
        }
    }

    /** Add/remove explicitly, without coupling queue membership to Play Status. */
    fun setQueued(game: Game, queued: Boolean) {
        val uid = observedUid ?: return
        if (mutable.value.saving) return
        mutable.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                repository.setQueued(uid, game.id, queued)
            } catch (error: Exception) {
                mutable.update { it.copy(error = error.message ?: "Could not update queue") }
            } finally {
                mutable.update { it.copy(saving = false) }
            }
        }
    }

    fun delete(game: Game, onSuccess: () -> Unit) {
        val uid = observedUid ?: return
        if (mutable.value.saving) return
        mutable.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                repository.delete(uid, game)
                onSuccess()
            } catch (error: Exception) {
                mutable.update { it.copy(error = error.message ?: "Could not delete game") }
            } finally {
                mutable.update { it.copy(saving = false) }
            }
        }
    }

    class Factory(private val repository: GameRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(GameLibraryViewModel::class.java))
            return GameLibraryViewModel(repository) as T
        }
    }
}
