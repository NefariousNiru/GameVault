package com.gamevault.app.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gamevault.app.domain.game.Game
import com.gamevault.app.domain.game.GameDraft
import com.gamevault.app.domain.game.GameRepository
import com.gamevault.app.domain.game.QuickStatusField
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class GameLibraryUiState(
    val games: List<Game> = emptyList(),
    val queueOrder: List<String> = emptyList(),
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
                combine(repository.observeGames(uid), repository.observeQueueOrder(uid)) { games, order ->
                    games to order
                }.collect { (games, order) ->
                    mutable.update { it.copy(games = games, queueOrder = order, loading = false, error = null) }
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

    /** One atomic reorder after release, never per-frame writes. */
    fun reorderQueue(orderedIds: List<String>, onResult: (Boolean) -> Unit) {
        val uid = observedUid
        if (uid == null || mutable.value.saving) {
            onResult(false)
            return
        }
        mutable.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            var successful = false
            try {
                repository.reorderQueue(uid, orderedIds)
                successful = true
            } catch (error: Exception) {
                mutable.update { it.copy(error = error.message ?: "Could not reorder queue") }
            } finally {
                mutable.update { it.copy(saving = false) }
                onResult(successful)
            }
        }
    }

    /**
     * Clear queue membership without deleting games or altering gameplay fields.
     * Each removal uses the existing, rules-tested transaction to avoid
     * exceeding Firestore security-rule access limits in a giant batch.
     * Interrupted clears can safely be retried.
     */
    fun clearQueue() {
        val uid = observedUid ?: return
        if (mutable.value.saving) return
        val members = mutable.value.games.filter { it.draft.queueRank != null }
        if (members.isEmpty()) return
        mutable.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                members.forEach { repository.setQueued(uid, it.id, false) }
            } catch (failure: Exception) {
                mutable.update {
                    it.copy(error = "Queue partially cleared. Retry to finish: " +
                        (failure.message ?: "connection interrupted"))
                }
            } finally {
                mutable.update { it.copy(saving = false) }
            }
        }
    }

    /** Partial updates protect other user-owned fields from stale screen state. */
    fun setQuickStatus(game: Game, field: QuickStatusField, value: String) {
        val uid = observedUid ?: return
        if (mutable.value.saving) return
        mutable.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                repository.updateStatus(uid, game.id, field, value)
            } catch (error: Exception) {
                mutable.update { it.copy(error = error.message ?: "Could not update status") }
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
