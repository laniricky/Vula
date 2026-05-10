package com.vula.app.global.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.vula.app.contacts.data.ContactSyncManager
import com.vula.app.core.model.Post
import com.vula.app.core.model.Story
import com.vula.app.core.util.Constants
import com.vula.app.global.data.FeedPagingSource
import com.vula.app.global.data.PostRepository
import com.vula.app.global.data.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val api: com.vula.app.core.network.VulaApiService,
    private val postRepository: PostRepository,
    private val storyRepository: StoryRepository,
    private val contactSyncManager: ContactSyncManager
) : ViewModel() {

    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    val posts: Flow<PagingData<Post>> = Pager(
        config = PagingConfig(pageSize = Constants.PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = { FeedPagingSource(api) }
    ).flow.cachedIn(viewModelScope)

    val contactMap = contactSyncManager.contactMap

    init {
        contactSyncManager.syncContacts()
        loadStories()
    }

    private fun loadStories() {
        viewModelScope.launch {
            storyRepository.getStories().collect { _stories.value = it }
        }
    }

    fun likePost(postId: String, currentUserId: String) {
        viewModelScope.launch { postRepository.reactToPost(postId, currentUserId, "❤️") }
    }

    fun unlikePost(postId: String, currentUserId: String) {
        viewModelScope.launch { postRepository.removeReaction(postId, currentUserId) }
    }

    fun reactToPost(postId: String, currentUserId: String, emoji: String) {
        viewModelScope.launch { postRepository.reactToPost(postId, currentUserId, emoji) }
    }

    fun removeReaction(postId: String, currentUserId: String) {
        viewModelScope.launch { postRepository.removeReaction(postId, currentUserId) }
    }
}
