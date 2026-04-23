package com.vula.app.global.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vula.app.core.model.Post
import com.vula.app.global.data.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import com.vula.app.contacts.data.ContactSyncManager
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val contactSyncManager: ContactSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()
    
    val contactMap = contactSyncManager.contactMap

    init {
        loadFeed()
        contactSyncManager.syncContacts()
    }

    private fun loadFeed() {
        viewModelScope.launch {
            postRepository.getGlobalFeed()
                .catch { e ->
                    _uiState.value = FeedUiState.Error(e.message ?: "Failed to load feed")
                }
                .collect { posts ->
                    _uiState.value = FeedUiState.Success(posts)
                }
        }
    }

    fun likePost(postId: String, currentUserId: String) {
        viewModelScope.launch {
            postRepository.likePost(postId, currentUserId)
        }
    }

    fun unlikePost(postId: String, currentUserId: String) {
        viewModelScope.launch {
            postRepository.unlikePost(postId, currentUserId)
        }
    }
}

sealed class FeedUiState {
    object Loading : FeedUiState()
    data class Success(val posts: List<Post>) : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}
