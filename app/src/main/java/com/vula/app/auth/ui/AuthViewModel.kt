package com.vula.app.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vula.app.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUser = authRepository.currentUser

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Username and password cannot be empty")
            return
        }
        
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.login(username.trim(), password)
            result.onSuccess {
                _authState.value = AuthState.Success
            }.onFailure { e ->
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun register(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Username and password cannot be empty")
            return
        }

        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }
        
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.register(username.trim(), password)
            result.onSuccess {
                _authState.value = AuthState.Success
            }.onFailure { e ->
                _authState.value = AuthState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}
