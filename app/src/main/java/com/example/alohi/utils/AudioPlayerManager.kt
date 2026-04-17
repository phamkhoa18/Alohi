package com.example.alohi.utils

import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AudioPlaybackState(
    val url: String = "",
    val isPlaying: Boolean = false,
    val progress: Float = 0f, 
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0
)

object AudioPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    
    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> get() = _playbackState

    fun togglePlayPause(url: String) {
        if (_playbackState.value.url == url && mediaPlayer != null) {
            if (mediaPlayer!!.isPlaying) {
                mediaPlayer!!.pause()
                updateState { it.copy(isPlaying = false) }
            } else {
                mediaPlayer!!.start()
                updateState { it.copy(isPlaying = true) }
            }
            return
        }

        // New URL
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                prepare()
                start()
                setOnCompletionListener {
                    updateState { s -> s.copy(isPlaying = false, progress = 1f, currentPositionMs = duration) }
                }
            }
            updateState { 
                AudioPlaybackState(
                    url = url,
                    isPlaying = true,
                    durationMs = mediaPlayer?.duration ?: 0,
                    currentPositionMs = mediaPlayer?.currentPosition ?: 0
                ) 
            }
        } catch (e: Exception) {
            e.printStackTrace()
            updateState { AudioPlaybackState() } // Reset on error
        }
    }

    fun seekTo(progress: Float) {
        mediaPlayer?.let {
            val newPosition = (progress * it.duration).toInt()
            it.seekTo(newPosition)
            updateState { s -> s.copy(progress = progress, currentPositionMs = newPosition) }
        }
    }

    fun updateProgressIfPlaying() {
        mediaPlayer?.let {
            if (it.isPlaying && it.duration > 0) {
                val pos = it.currentPosition
                val dur = it.duration
                updateState { s -> 
                    s.copy(
                        progress = pos.toFloat() / dur.toFloat(),
                        currentPositionMs = pos,
                        durationMs = dur
                    )
                }
            }
        }
    }
    
    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        updateState { AudioPlaybackState() }
    }

    private inline fun updateState(updater: (AudioPlaybackState) -> AudioPlaybackState) {
        _playbackState.value = updater(_playbackState.value)
    }
}
