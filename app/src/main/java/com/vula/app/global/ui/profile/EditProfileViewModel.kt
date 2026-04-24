package com.vula.app.global.ui.profile

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.vula.app.core.model.User
import com.vula.app.core.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Idle)
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    var displayName by mutableStateOf("")
    var bio by mutableStateOf("")
    var pendingPhotoUri by mutableStateOf<Uri?>(null)
    var currentImageUrl by mutableStateOf<String?>(null)

    init {
        loadCurrentProfile()
    }

    private fun loadCurrentProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val doc = firestore.collection(Constants.USERS_COLLECTION).document(uid).get().await()
                val user = doc.toObject(User::class.java)
                user?.let {
                    displayName = it.displayName
                    bio = it.bio
                    currentImageUrl = it.profileImageUrl
                }
            } catch (_: Exception) {}
        }
    }

    fun onPhotoSelected(uri: Uri) {
        pendingPhotoUri = uri
    }

    fun saveProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = EditProfileUiState.Saving
            try {
                // Upload new photo if selected
                val imageUrl: String? = if (pendingPhotoUri != null) {
                    val ref = storage.reference.child("profile_photos/$uid.jpg")
                    ref.putFile(pendingPhotoUri!!).await()
                    ref.downloadUrl.await().toString()
                } else {
                    currentImageUrl
                }

                val updates = mutableMapOf<String, Any>(
                    "displayName" to displayName.trim(),
                    "bio" to bio.trim()
                )
                if (imageUrl != null) updates["profileImageUrl"] = imageUrl

                firestore.collection(Constants.USERS_COLLECTION)
                    .document(uid)
                    .update(updates)
                    .await()

                _uiState.value = EditProfileUiState.SaveSuccess
            } catch (e: Exception) {
                _uiState.value = EditProfileUiState.Error(e.message ?: "Save failed")
            }
        }
    }
}

sealed class EditProfileUiState {
    object Idle : EditProfileUiState()
    object Saving : EditProfileUiState()
    object SaveSuccess : EditProfileUiState()
    data class Error(val message: String) : EditProfileUiState()
}
