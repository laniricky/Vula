package com.vula.app.local.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vula.app.core.model.LocalPost
import com.vula.app.core.util.AliasGenerator
import com.vula.app.core.util.HashUtils
import com.vula.app.local.data.LocalRepository
import com.vula.app.local.data.wifi.WifiDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalViewModel @Inject constructor(
    private val wifiDetector: WifiDetector,
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LocalUiState>(LocalUiState.Disabled)
    val uiState: StateFlow<LocalUiState> = _uiState.asStateFlow()

    private var currentNetworkId: String? = null
    private var currentDeviceHash: String? = null
    private var currentAlias: String? = null

    private val deviceId = java.util.UUID.randomUUID().toString()

    fun enableLocalMode() {
        val wifiDetails = wifiDetector.getCurrentWifiDetails()
        if (wifiDetails == null) {
            _uiState.value = LocalUiState.Error("Please connect to a Wi-Fi network first.")
            return
        }

        val networkId = HashUtils.sha256(wifiDetails.ssid + wifiDetails.bssid)
        val deviceHash = HashUtils.sha256(deviceId)
        val alias = AliasGenerator.generate()

        currentNetworkId = networkId
        currentDeviceHash = deviceHash
        currentAlias = alias

        viewModelScope.launch {
            _uiState.value = LocalUiState.Loading
            localRepository.joinNetwork(networkId, deviceHash, alias).onSuccess {
                observeLocalNetwork(networkId)
            }.onFailure { e ->
                _uiState.value = LocalUiState.Error(e.message ?: "Failed to join local network")
            }
        }
    }

    private fun observeLocalNetwork(networkId: String) {
        viewModelScope.launch {
            localRepository.getPeopleHere(networkId).collect { people ->
                val currentState = _uiState.value
                if (currentState is LocalUiState.Active) {
                    _uiState.value = currentState.copy(peopleHere = people)
                } else {
                    _uiState.value = LocalUiState.Active(
                        networkId = networkId,
                        alias = currentAlias!!,
                        peopleHere = people,
                        posts = emptyList()
                    )
                }
            }
        }

        viewModelScope.launch {
            localRepository.getLocalFeed(networkId).collect { posts ->
                val currentState = _uiState.value
                if (currentState is LocalUiState.Active) {
                    _uiState.value = currentState.copy(posts = posts)
                }
            }
        }
    }

    fun postText(text: String) {
        if (text.isBlank()) return
        val networkId = currentNetworkId ?: return
        val deviceHash = currentDeviceHash ?: return
        val alias = currentAlias ?: return

        viewModelScope.launch {
            localRepository.postToLocal(networkId, deviceHash, alias, text.trim())
        }
    }

    fun disableLocalMode() {
        val networkId = currentNetworkId ?: return
        val deviceHash = currentDeviceHash ?: return

        viewModelScope.launch {
            localRepository.leaveNetwork(networkId, deviceHash)
            _uiState.value = LocalUiState.Disabled
            currentNetworkId = null
            currentDeviceHash = null
            currentAlias = null
        }
    }
}

sealed class LocalUiState {
    object Disabled : LocalUiState()
    object Loading : LocalUiState()
    data class Active(
        val networkId: String,
        val alias: String,
        val peopleHere: List<String>,
        val posts: List<LocalPost>
    ) : LocalUiState()
    data class Error(val message: String) : LocalUiState()
}
