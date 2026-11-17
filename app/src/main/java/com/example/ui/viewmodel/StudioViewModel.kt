package com.example.ui.viewmodel

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.InteractionsMediaClient
import com.example.data.ai.MusicGenerationResult
import com.example.data.ai.VideoGenerationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MusicUiState {
    data object Idle : MusicUiState
    data object Loading : MusicUiState
    data class Ready(val result: MusicGenerationResult) : MusicUiState
    data class Error(val message: String) : MusicUiState
}

sealed interface VideoUiState {
    data object Idle : VideoUiState
    data class Loading(val statusMessage: String) : VideoUiState
    data class Ready(val result: VideoGenerationResult) : VideoUiState
    data class Error(val message: String) : VideoUiState
}

class StudioViewModel(appContext: Context) : ViewModel() {

    private val client = InteractionsMediaClient(appContext.applicationContext)
    private var mediaPlayer: MediaPlayer? = null

    private val _musicState = MutableStateFlow<MusicUiState>(MusicUiState.Idle)
    val musicState: StateFlow<MusicUiState> = _musicState.asStateFlow()

    private val _isPlayingMusic = MutableStateFlow(false)
    val isPlayingMusic: StateFlow<Boolean> = _isPlayingMusic.asStateFlow()

    private val _videoState = MutableStateFlow<VideoUiState>(VideoUiState.Idle)
    val videoState: StateFlow<VideoUiState> = _videoState.asStateFlow()

    fun generateMusicTrack(prompt: String, useProModel: Boolean) {
        if (prompt.isBlank()) {
            _musicState.value = MusicUiState.Error("Enter a music prompt or pick a tactical preset.")
            return
        }
        val modelId = if (useProModel) "lyria-3-pro-preview" else "lyria-3-clip-preview"
        stopAudio()
        viewModelScope.launch {
            _musicState.value = MusicUiState.Loading
            try {
                val res = client.generateMusic(prompt = prompt, modelId = modelId)
                res.onSuccess { generated ->
                    _musicState.value = MusicUiState.Ready(generated)
                }.onFailure { err ->
                    _musicState.value = MusicUiState.Error(
                        err.localizedMessage ?: "Music generation failed."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _musicState.value = MusicUiState.Error(
                    e.localizedMessage ?: "Music generation failed."
                )
            }
        }
    }

    fun toggleMusicPlayback() {
        val ready = _musicState.value as? MusicUiState.Ready ?: return
        val current = mediaPlayer
        if (current != null) {
            if (current.isPlaying) {
                current.pause()
                _isPlayingMusic.value = false
            } else {
                current.start()
                _isPlayingMusic.value = true
            }
            return
        }
        try {
            val mp = MediaPlayer().apply {
                setDataSource(ready.result.audioFile.absolutePath)
                setOnCompletionListener {
                    _isPlayingMusic.value = false
                }
                prepare()
                start()
            }
            mediaPlayer = mp
            _isPlayingMusic.value = true
        } catch (e: Exception) {
            _musicState.value = MusicUiState.Error(
                "Could not play audio file: ${e.localizedMessage}"
            )
        }
    }

    fun stopAudio() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        _isPlayingMusic.value = false
    }

    fun animatePhotoToVideo(imageUri: Uri?, prompt: String, aspectRatio: String) {
        if (imageUri == null) {
            _videoState.value = VideoUiState.Error("Select a photo first using Upload Photo.")
            return
        }
        if (prompt.isBlank()) {
            _videoState.value = VideoUiState.Error("Describe how you want to animate the photo.")
            return
        }
        viewModelScope.launch {
            _videoState.value = VideoUiState.Loading("Preparing photo for Gemini Omni 1.2 Flash...")
            try {
                val res = client.animateImageToVideo(
                    imageUri = imageUri,
                    prompt = prompt,
                    aspectRatio = aspectRatio,
                    onStatusUpdate = { status ->
                        _videoState.value = VideoUiState.Loading(status)
                    }
                )
                res.onSuccess { generated ->
                    _videoState.value = VideoUiState.Ready(generated)
                }.onFailure { err ->
                    _videoState.value = VideoUiState.Error(
                        err.localizedMessage ?: "Video animation failed."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _videoState.value = VideoUiState.Error(
                    e.localizedMessage ?: "Video animation failed."
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
    }
}
