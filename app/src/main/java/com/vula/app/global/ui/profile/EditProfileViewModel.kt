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

    // ── Identity ──────────────────────────────────────────────────────────────
    var displayName      by mutableStateOf("")
    var username         by mutableStateOf("")
    var bio              by mutableStateOf("")

    // ── Avatar ────────────────────────────────────────────────────────────────
    var pendingPhotoUri  by mutableStateOf<Uri?>(null)
    var currentImageUrl  by mutableStateOf<String?>(null)

    // ── Banner ────────────────────────────────────────────────────────────────
    var pendingBannerUri by mutableStateOf<Uri?>(null)
    var currentBannerUrl by mutableStateOf<String?>(null)

    // ── Presence ──────────────────────────────────────────────────────────────
    var richStatus       by mutableStateOf("")
    var website          by mutableStateOf("")

    // ── Privacy ───────────────────────────────────────────────────────────────
    var isPrivate        by mutableStateOf(false)

    init { loadCurrentProfile() }

    private fun loadCurrentProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val doc = firestore.collection(Constants.USERS_COLLECTION).document(uid).get().await()
                val user = doc.toObject(User::class.java)
                user?.let {
                    displayName      = it.displayName
                    bio              = it.bio
                    currentImageUrl  = it.profileImageUrl
                }
                // Extended fields (may not exist in older documents — safe defaults)
                username         = doc.getString("username")    ?: ""
                richStatus       = doc.getString("richStatus")  ?: ""
                website          = doc.getString("website")     ?: ""
                currentBannerUrl = doc.getString("bannerUrl")   ?: null
                isPrivate        = doc.getBoolean("isPrivate")  ?: false
            } catch (_: Exception) {}
        }
    }

    fun onPhotoSelected(uri: Uri)  { pendingPhotoUri  = uri }
    fun onBannerSelected(uri: Uri) { pendingBannerUri = uri }

    fun saveProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = EditProfileUiState.Saving
            try {
                // Upload avatar
                val imageUrl: String? = if (pendingPhotoUri != null) {
                    val ref = storage.reference.child("profile_photos/$uid.jpg")
                    ref.putFile(pendingPhotoUri!!).await()
                    ref.downloadUrl.await().toString()
                } else currentImageUrl

                // Upload banner
                val bannerUrl: String? = if (pendingBannerUri != null) {
                    val ref = storage.reference.child("profile_banners/$uid.jpg")
                    ref.putFile(pendingBannerUri!!).await()
                    ref.downloadUrl.await().toString()
                } else currentBannerUrl

                val updates = mutableMapOf<String, Any>(
                    "displayName" to displayName.trim(),
                    "bio"         to bio.trim(),
                    "username"    to username.trim().lowercase(),
                    "richStatus"  to richStatus.trim(),
                    "website"     to website.trim(),
                    "isPrivate"   to isPrivate
                )
                if (imageUrl  != null) updates["profileImageUrl"] = imageUrl
                if (bannerUrl != null) updates["bannerUrl"]       = bannerUrl

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
    object Idle        : EditProfileUiState()
    object Saving      : EditProfileUiState()
    object SaveSuccess : EditProfileUiState()
    data class Error(val message: String) : EditProfileUiState()
}
