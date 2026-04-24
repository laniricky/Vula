package com.vula.app.global.ui.story

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vula.app.global.data.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CreateStoryUiState {
    object Idle : CreateStoryUiState()
    object Loading : CreateStoryUiState()
    object Success : CreateStoryUiState()
    data class Error(val message: String) : CreateStoryUiState()
}

@HiltViewModel
class CreateStoryViewModel @Inject constructor(
    private val storyRepository: StoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateStoryUiState>(CreateStoryUiState.Idle)
    val uiState: StateFlow<CreateStoryUiState> = _uiState.asStateFlow()

    fun uploadStory(mediaUri: Uri, mediaType: String) {
        _uiState.value = CreateStoryUiState.Loading
        viewModelScope.launch {
            val result = storyRepository.createStory(mediaUri, mediaType)
            if (result.isSuccess) {
                _uiState.value = CreateStoryUiState.Success
            } else {
                _uiState.value = CreateStoryUiState.Error(
                    result.exceptionOrNull()?.message ?: "Unknown error"
                )
            }
        }
    }
}
