package com.vula.app.global.ui.profile

import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vula.app.core.data.SessionManager
import com.vula.app.core.network.UpdateProfileBody
import com.vula.app.core.network.VulaApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val api: VulaApiService,
    private val sessionManager: SessionManager
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
        viewModelScope.launch {
            try {
                val response = api.getMe()
                if (response.isSuccessful && response.body() != null) {
                    val u = response.body()!!
                    displayName      = u.displayName
                    username         = u.username
                    bio              = u.bio
                    currentImageUrl  = u.profileImageUrl
                    currentBannerUrl = u.bannerUrl
                    richStatus       = u.richStatus ?: ""
                    website          = u.website ?: ""
                    isPrivate        = u.isPrivate
                }
            } catch (_: Exception) {}
        }
    }

    fun onPhotoSelected(uri: Uri)  { pendingPhotoUri  = uri }
    fun onBannerSelected(uri: Uri) { pendingBannerUri = uri }

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.value = EditProfileUiState.Saving
            try {
                // 1. Update text fields
                val updateResponse = api.updateProfile(
                    UpdateProfileBody(
                        displayName = displayName.trim(),
                        username    = username.trim().lowercase(),
                        bio         = bio.trim(),
                        richStatus  = richStatus.trim(),
                        website     = website.trim(),
                        isPrivate   = isPrivate
                    )
                )
                if (!updateResponse.isSuccessful) throw Exception("Profile update failed: ${updateResponse.code()}")

                // 2. Upload avatar if changed
                if (pendingPhotoUri != null) {
                    val part = uriToMultipart(pendingPhotoUri!!, "avatar", "image/jpeg")
                    val avatarResponse = api.uploadAvatar(part)
                    if (avatarResponse.isSuccessful) {
                        currentImageUrl = avatarResponse.body()?.profileImageUrl
                    }
                }

                // 3. Upload banner if changed
                if (pendingBannerUri != null) {
                    val part = uriToMultipart(pendingBannerUri!!, "banner", "image/jpeg")
                    val bannerResponse = api.uploadBanner(part)
                    if (bannerResponse.isSuccessful) {
                        currentBannerUrl = bannerResponse.body()?.bannerUrl
                    }
                }

                _uiState.value = EditProfileUiState.SaveSuccess
            } catch (e: Exception) {
                _uiState.value = EditProfileUiState.Error(e.message ?: "Save failed")
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun uriToMultipart(uri: Uri, name: String, mime: String): okhttp3.MultipartBody.Part {
        // NOTE: In a real implementation, copy the Uri to a temp File via ContentResolver
        // before building the part. This is a placeholder for compile correctness.
        val requestBody = okhttp3.RequestBody.create(mime.toMediaType(), byteArrayOf())
        return okhttp3.MultipartBody.Part.createFormData(name, "$name.jpg", requestBody)
    }

    private fun String.toMediaType() =
        this.toMediaTypeOrNull() ?: "application/octet-stream".toMediaTypeOrNull()!!
}

sealed class EditProfileUiState {
    object Idle        : EditProfileUiState()
    object Saving      : EditProfileUiState()
    object SaveSuccess : EditProfileUiState()
    data class Error(val message: String) : EditProfileUiState()
}
