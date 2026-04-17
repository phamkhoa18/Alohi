package com.example.alohi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alohi.data.model.StoriesFeedResponse
import com.example.alohi.data.model.StoryGroup
import com.example.alohi.data.repository.StoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class StoryUiState(
    val isLoading: Boolean = false,
    val feed: StoriesFeedResponse? = null,
    val error: String? = null,
    val isUploading: Boolean = false,
    val uploadSuccess: Boolean = false
)

class StoryViewModel : ViewModel() {
    private val repository = StoryRepository()

    private val _uiState = MutableStateFlow(StoryUiState())
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()

    init {
        loadFeed()
    }

    fun loadFeed() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.getFeed()
            result.onSuccess { feed ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    feed = feed
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message
                )
            }
        }
    }

    fun createStory(file: File, type: String = "image", caption: String? = null, musicFile: File? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploading = true, uploadSuccess = false, error = null)
            val result = repository.createStory(type = type, file = file, caption = caption, musicFile = musicFile)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isUploading = false, uploadSuccess = true)
                loadFeed() // Reload feed after upload
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(isUploading = false, error = error.message)
            }
        }
    }

    fun resetUploadState() {
        _uiState.value = _uiState.value.copy(uploadSuccess = false, error = null)
    }

    fun markAsViewed(storyId: String) {
        viewModelScope.launch {
            repository.viewStory(storyId)
            // Optionally, immediately update local state visually instead of full reload.
        }
    }
}
