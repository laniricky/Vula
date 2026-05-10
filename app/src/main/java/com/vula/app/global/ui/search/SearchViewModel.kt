package com.vula.app.global.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vula.app.core.model.User
import com.vula.app.core.network.VulaApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val api: VulaApiService
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<User>>(emptyList())
    val results: StateFlow<List<User>> = _results.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _results.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _isLoading.value = true
            try {
                val response = api.searchUsers(query = newQuery.trim().lowercase(), limit = 25)
                _results.value = if (response.isSuccessful) {
                    response.body()?.map {
                        User(
                            id              = it.id,
                            username        = it.username,
                            phoneNumber     = it.phoneNumber,
                            phoneHash       = "",
                            displayName     = it.displayName,
                            bio             = it.bio,
                            profileImageUrl = it.profileImageUrl,
                            followersCount  = it.followersCount,
                            followingCount  = it.followingCount,
                            postsCount      = it.postsCount,
                            isOnline        = it.isOnline,
                            createdAt       = it.createdAt
                        )
                    } ?: emptyList()
                } else emptyList()
            } catch (_: Exception) {
                _results.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
