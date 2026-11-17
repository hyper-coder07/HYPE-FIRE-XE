package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.PlayerRepository
import com.example.data.model.CosmeticItem
import com.example.data.model.GameMode
import com.example.data.model.GraphicsConfig
import com.example.data.model.MatchRecord
import com.example.data.model.PlayerProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class GameHubViewModel(
    private val repository: PlayerRepository,
    private val currentUserId: String,
    private val initialDisplayName: String?
) : ViewModel() {

    val profileState: StateFlow<UiState<PlayerProfile>> = repository
        .observePlayerProfile(currentUserId)
        .map<PlayerProfile?, UiState<PlayerProfile>> { profile ->
            if (profile != null) {
                UiState.Success(profile)
            } else {
                UiState.Loading
            }
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing player profile", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load player profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val matchesState: StateFlow<UiState<List<MatchRecord>>> = repository
        .observeMatches(currentUserId)
        .map<List<MatchRecord>, UiState<List<MatchRecord>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing match history", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load match history"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _selectedMode = MutableStateFlow(GameMode.BERMUDA_BLITZ)
    val selectedMode: StateFlow<GameMode> = _selectedMode.asStateFlow()

    private val _actionFeedback = MutableStateFlow<String?>(null)
    val actionFeedback: StateFlow<String?> = _actionFeedback.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureProfileExists(initialDisplayName).onFailure { err ->
                _actionFeedback.value = err.localizedMessage ?: "Failed to initialize profile"
            }
        }
    }

    fun selectMode(mode: GameMode) {
        _selectedMode.value = mode
    }

    fun clearFeedback() {
        _actionFeedback.value = null
    }

    fun buyOrEquipCosmetic(profile: PlayerProfile, item: CosmeticItem) {
        viewModelScope.launch {
            val result = if (profile.ownedCosmeticIds.contains(item.id)) {
                repository.equipCosmetic(item)
            } else {
                repository.purchaseAndEquipCosmetic(profile, item)
            }
            result.onSuccess {
                _actionFeedback.value = if (profile.ownedCosmeticIds.contains(item.id)) {
                    "Equipped ${item.name}"
                } else {
                    "Unlocked & equipped ${item.name}!"
                }
            }.onFailure { err ->
                _actionFeedback.value = err.localizedMessage ?: "Could not complete cosmetic action"
            }
        }
    }

    fun saveGraphicsAndControls(config: GraphicsConfig, callsign: String) {
        viewModelScope.launch {
            repository.updateGraphicsAndControls(config, callsign)
                .onSuccess {
                    _actionFeedback.value = "Optimizer profile saved (${config.preset.label})"
                }
                .onFailure { err ->
                    _actionFeedback.value = err.localizedMessage ?: "Failed to save optimizer settings"
                }
        }
    }

    fun submitMatchResult(
        profile: PlayerProfile,
        placement: Int,
        eliminations: Int,
        damageDealt: Int,
        survivalSeconds: Int,
        goldEarned: Int,
        diamondsEarned: Int,
        modeId: String
    ) {
        viewModelScope.launch {
            repository.recordMatchResult(
                profile = profile,
                placement = placement,
                eliminations = eliminations,
                damageDealt = damageDealt,
                survivalSeconds = survivalSeconds,
                goldEarned = goldEarned,
                diamondsEarned = diamondsEarned,
                modeId = modeId
            ).onFailure { err ->
                _actionFeedback.value = err.localizedMessage ?: "Failed to save match stats"
            }
        }
    }

    private companion object {
        const val TAG = "GameHubVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
