package com.vula.app.global.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vula.app.chat.data.ChatRepository
import com.vula.app.core.data.SessionManager
import com.vula.app.core.model.Post
import com.vula.app.core.model.User
import com.vula.app.core.network.VulaApiService
import com.vula.app.global.data.FollowRepository
import com.vula.app.global.data.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val api: VulaApiService,
    private val postRepository: PostRepository,
    private val followRepository: FollowRepository,
    private val chatRepository: ChatRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _messageState = MutableStateFlow<MessageButtonState>(MessageButtonState.Idle)
    val messageState: StateFlow<MessageButtonState> = _messageState.asStateFlow()

    fun loadProfile(userId: String?) {
        viewModelScope.launch {
            try {
                val currentUserId = sessionManager.getUserIdNow() ?: throw Exception("Not logged in")
                val targetUserId  = if (userId.isNullOrBlank()) currentUserId else userId
                val isOwnProfile  = targetUserId == currentUserId

                // Fetch user
                val userResponse = if (isOwnProfile) api.getMe() else api.getUser(targetUserId)
                if (!userResponse.isSuccessful || userResponse.body() == null) {
                    _uiState.value = ProfileUiState.Error("User not found")
                    return@launch
                }
                val apiUser = userResponse.body()!!
                val user = User(
                    id              = apiUser.id,
                    username        = apiUser.username,
                    phoneNumber     = apiUser.phoneNumber,
                    phoneHash       = "",
                    displayName     = apiUser.displayName,
                    bio             = apiUser.bio,
                    profileImageUrl = apiUser.profileImageUrl,
                    richStatus      = apiUser.richStatus,
                    followersCount  = apiUser.followersCount,
                    followingCount  = apiUser.followingCount,
                    postsCount      = apiUser.postsCount,
                    isOnline        = apiUser.isOnline,
                    createdAt       = apiUser.createdAt
                )

                // Follow status
                var isFollowing = false
                if (!isOwnProfile) {
                    val fsResponse = api.getFollowStatus(targetUserId)
                    isFollowing = fsResponse.body()?.isFollowing == true
                }

                // Posts
                postRepository.getUserPosts(targetUserId).collect { posts ->
                    _uiState.value = ProfileUiState.Success(
                        user         = user,
                        posts        = posts,
                        isOwnProfile = isOwnProfile,
                        isFollowing  = isFollowing
                    )
                }

                if (!isOwnProfile) watchMessageStatus(targetUserId)

            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Error loading profile")
            }
        }
    }

    private fun watchMessageStatus(targetUserId: String) {
        viewModelScope.launch {
            chatRepository.getRequestStatusTo(targetUserId).collect { status ->
                _messageState.value = when (status) {
                    "pending", "accepted" -> MessageButtonState.RequestSent
                    else                  -> MessageButtonState.Idle
                }
            }
        }
    }

    fun sendMessageRequest(targetUser: User) {
        _messageState.value = MessageButtonState.Loading
        viewModelScope.launch {
            val result = chatRepository.sendMessageRequest(
                toUserId           = targetUser.id,
                toUsername         = targetUser.username,
                toProfileImageUrl  = targetUser.profileImageUrl
            )
            _messageState.value = if (result.isSuccess) MessageButtonState.RequestSent
                                  else MessageButtonState.Idle
        }
    }

    fun openOrCreateChat(targetUserId: String, onRoomReady: (String) -> Unit) {
        viewModelScope.launch {
            chatRepository.createDirectChat(targetUserId).getOrNull()?.let { onRoomReady(it) }
        }
    }

    fun toggleFollow(targetUserId: String, isCurrentlyFollowing: Boolean) {
        viewModelScope.launch {
            val currentUserId = sessionManager.getUserIdNow() ?: return@launch
            if (isCurrentlyFollowing) followRepository.unfollowUser(targetUserId, currentUserId)
            else                      followRepository.followUser(targetUserId, currentUserId)
        }
    }

    fun logout() {
        viewModelScope.launch { sessionManager.clearSession() }
    }
}

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val user: User,
        val posts: List<Post>,
        val isOwnProfile: Boolean,
        val isFollowing: Boolean
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

sealed class MessageButtonState {
    object Idle        : MessageButtonState()
    object Loading     : MessageButtonState()
    object RequestSent : MessageButtonState()
}
