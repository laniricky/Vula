package com.vula.app.global.ui.post

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vula.app.global.data.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreatePostUiState>(CreatePostUiState.Idle)
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun createPost(caption: String, mediaUri: Uri?, mediaType: String = "image") {
        viewModelScope.launch {
            _uiState.value = CreatePostUiState.Loading
            val result = postRepository.createPost(caption, mediaUri, mediaType)
            result.onSuccess {
                _uiState.value = CreatePostUiState.Success
            }.onFailure { e ->
                _uiState.value = CreatePostUiState.Error(e.message ?: "Failed to create post")
            }
        }
    }
}

sealed class CreatePostUiState {
    object Idle : CreatePostUiState()
    object Loading : CreatePostUiState()
    object Success : CreatePostUiState()
    data class Error(val message: String) : CreatePostUiState()
}
