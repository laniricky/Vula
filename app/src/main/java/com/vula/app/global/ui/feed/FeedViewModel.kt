package com.vula.app.global.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.firebase.firestore.FirebaseFirestore
import com.vula.app.contacts.data.ContactSyncManager
import com.vula.app.core.model.Post
import com.vula.app.core.model.Story
import com.vula.app.global.data.FeedPagingSource
import com.vula.app.global.data.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val postRepository: PostRepository,
    private val storyRepository: com.vula.app.global.data.StoryRepository,
    private val contactSyncManager: ContactSyncManager
) : ViewModel() {
    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    val posts: Flow<PagingData<Post>> = Pager(
        config = PagingConfig(pageSize = 10, enablePlaceholders = false),
        pagingSourceFactory = { FeedPagingSource(firestore) }
    ).flow.cachedIn(viewModelScope)

    val contactMap = contactSyncManager.contactMap

    init {
        contactSyncManager.syncContacts()
        loadStories()
    }

    private fun loadStories() {
        viewModelScope.launch {
            storyRepository.getStories().collect { newStories ->
                // Filter out duplicates if multiple users posted, or group by user.
                // For now, we just display them.
                _stories.value = newStories
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
