package com.vula.app.global.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vula.app.core.model.Post
import com.vula.app.core.model.User
import com.vula.app.core.util.Constants
import com.vula.app.global.data.FollowRepository
import com.vula.app.global.data.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val followRepository: FollowRepository,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(userId: String?) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: throw Exception("Not logged in")
                val targetUserId = userId ?: currentUserId
                val isOwnProfile = targetUserId == currentUserId

                val userDoc = firestore.collection(Constants.USERS_COLLECTION).document(targetUserId).get().await()
                val user = userDoc.toObject(User::class.java) ?: throw Exception("User not found")

                var isFollowing = false
                if (!isOwnProfile) {
                    val followingDoc = firestore.collection(Constants.USERS_COLLECTION)
                        .document(currentUserId)
                        .collection(Constants.FOLLOWING_SUBCOLLECTION)
                        .document(targetUserId)
                        .get()
                        .await()
                    isFollowing = followingDoc.exists()
                }

                postRepository.getUserPosts(targetUserId).collect { posts ->
                    _uiState.value = ProfileUiState.Success(
                        user = user,
                        posts = posts,
                        isOwnProfile = isOwnProfile,
                        isFollowing = isFollowing
                    )
                }

            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Error loading profile")
            }
        }
    }

    fun toggleFollow(targetUserId: String, isCurrentlyFollowing: Boolean) {
        viewModelScope.launch {
            val currentUserId = auth.currentUser?.uid ?: return@launch
            if (isCurrentlyFollowing) {
                followRepository.unfollowUser(targetUserId, currentUserId)
            } else {
                followRepository.followUser(targetUserId, currentUserId)
            }
        }
    }

    fun logout() {
        auth.signOut()
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
