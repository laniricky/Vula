package com.vula.app.global.ui.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vula.app.core.model.Comment
import com.vula.app.global.data.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommentViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadComments(postId: String) {
        viewModelScope.launch {
            postRepository.getComments(postId).collect { list ->
                _comments.value = list
            }
        }
    }

    fun addComment(postId: String, text: String, onSuccess: () -> Unit = {}) {
        if (text.isBlank()) return
        _isPosting.value = true
        viewModelScope.launch {
            val result = postRepository.addComment(postId, text.trim())
            _isPosting.value = false
            result.onSuccess { onSuccess() }
                .onFailure { _error.value = it.message }
        }
    }

    fun clearError() { _error.value = null }
}
