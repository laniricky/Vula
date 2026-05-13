package com.vula.app.global.ui.ripples

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vula.app.core.data.SessionManager
import com.vula.app.core.model.Post
import com.vula.app.core.network.ReactBody
import com.vula.app.core.network.VulaApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RipplesMode { GLOBAL, LOCAL }

sealed class RipplesUiState {
    object Loading : RipplesUiState()
    data class Success(val ripples: List<Post>) : RipplesUiState()
    data class Error(val message: String) : RipplesUiState()
}

@HiltViewModel
class RipplesViewModel @Inject constructor(
    private val api: VulaApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<RipplesUiState>(RipplesUiState.Loading)
    val uiState: StateFlow<RipplesUiState> = _uiState.asStateFlow()

    private val _mode = MutableStateFlow(RipplesMode.GLOBAL)
    val mode: StateFlow<RipplesMode> = _mode.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    // Tracks liked post IDs for optimistic UI
    private val _likedPosts = MutableStateFlow<Set<String>>(emptySet())
    val likedPosts: StateFlow<Set<String>> = _likedPosts.asStateFlow()

    private var currentUserId: String = ""

    init {
        viewModelScope.launch {
            currentUserId = sessionManager.getUserIdNow() ?: ""
            loadRipples()
        }
    }

    fun setMode(mode: RipplesMode) {
        _mode.value = mode
        _currentIndex.value = 0
        loadRipples()
    }

    fun setCurrentIndex(index: Int) {
        _currentIndex.value = index
        // Pre-fetch more when nearing the end
        val state = _uiState.value
        if (state is RipplesUiState.Success && index >= state.ripples.size - 3) {
            loadMore()
        }
    }

    fun toggleLike(post: Post) {
        val liked = _likedPosts.value.contains(post.id)
        _likedPosts.value = if (liked) {
            _likedPosts.value - post.id
        } else {
            _likedPosts.value + post.id
        }
        viewModelScope.launch {
            runCatching {
                if (liked) api.removeReaction(post.id)
                else api.reactToPost(post.id, ReactBody("❤️"))
            }
        }
    }

    fun refresh() {
        _currentIndex.value = 0
        loadRipples()
    }

    private fun loadRipples() {
        viewModelScope.launch {
            _uiState.value = RipplesUiState.Loading
            try {
                val filter = if (_mode.value == RipplesMode.GLOBAL) "trending" else "new"
                val response = runCatching {
                    api.getExplorePosts(filter = filter, limit = 30).body() ?: emptyList()
                }.getOrElse { emptyList() }

                // Filter to only video posts
                val videos = response.filter { it.mediaType == "video" && !it.videoUrl.isNullOrBlank() }
                    .map { apiPost ->
                        Post(
                            id                    = apiPost.id,
                            authorId              = apiPost.authorId,
                            authorUsername        = apiPost.authorUsername,
                            authorProfileImageUrl = apiPost.authorProfileImageUrl,
                            caption               = apiPost.caption,
                            imageUrl              = apiPost.imageUrl,
                            videoUrl              = apiPost.videoUrl,
                            mediaType             = apiPost.mediaType,
                            likesCount            = apiPost.likesCount,
                            commentsCount         = apiPost.commentsCount,
                            createdAt             = apiPost.createdAt,
                            likedBy               = apiPost.likedBy,
                            reactions             = apiPost.reactions
                        )
                    }

                // Pre-populate liked set from the server data
                val alreadyLiked = videos.filter { it.likedBy.contains(currentUserId) }.map { it.id }.toSet()
                _likedPosts.value = alreadyLiked

                if (videos.isEmpty()) {
                    _uiState.value = RipplesUiState.Error("No Ripples yet. Be the first to post a video!")
                } else {
                    _uiState.value = RipplesUiState.Success(videos)
                }
            } catch (e: Exception) {
                _uiState.value = RipplesUiState.Error(e.message ?: "Failed to load Ripples")
            }
        }
    }

    private fun loadMore() {
        // In a real implementation, paginate. Here we just silently try to fetch more.
        viewModelScope.launch {
            val current = (_uiState.value as? RipplesUiState.Success)?.ripples ?: return@launch
            val filter  = if (_mode.value == RipplesMode.GLOBAL) "new" else "trending"
            val more = runCatching {
                api.getExplorePosts(filter = filter, limit = 20).body() ?: emptyList()
            }.getOrElse { emptyList() }

            val newVideos = more
                .filter { it.mediaType == "video" && !it.videoUrl.isNullOrBlank() }
                .filter { apiPost -> current.none { it.id == apiPost.id } }
                .map { apiPost ->
                    Post(
                        id                    = apiPost.id,
                        authorId              = apiPost.authorId,
                        authorUsername        = apiPost.authorUsername,
                        authorProfileImageUrl = apiPost.authorProfileImageUrl,
                        caption               = apiPost.caption,
                        imageUrl              = apiPost.imageUrl,
                        videoUrl              = apiPost.videoUrl,
                        mediaType             = apiPost.mediaType,
                        likesCount            = apiPost.likesCount,
                        commentsCount         = apiPost.commentsCount,
                        createdAt             = apiPost.createdAt,
                        likedBy               = apiPost.likedBy,
                        reactions             = apiPost.reactions
                    )
                }
            if (newVideos.isNotEmpty()) {
                _uiState.value = RipplesUiState.Success(current + newVideos)
            }
        }
    }
}
