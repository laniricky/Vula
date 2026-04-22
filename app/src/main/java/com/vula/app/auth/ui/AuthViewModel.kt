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

    private val _resetState = MutableStateFlow<ResetState>(ResetState.Idle)
    val resetState: StateFlow<ResetState> = _resetState.asStateFlow()

    val isUserLoggedIn: Boolean
        get() = authRepository.isUserLoggedIn

    val currentUser = authRepository.currentUser

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Username and password cannot be empty")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.login(username.trim(), password)
            result.onSuccess { _authState.value = AuthState.Success }
                .onFailure { e -> _authState.value = AuthState.Error(e.message ?: "Login failed") }
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
            result.onSuccess { _authState.value = AuthState.Success }
                .onFailure { e -> _authState.value = AuthState.Error(e.message ?: "Registration failed") }
        }
    }

    fun resetPassword(username: String) {
        if (username.isBlank()) {
            _resetState.value = ResetState.Error("Enter your username first")
            return
        }
        viewModelScope.launch {
            _resetState.value = ResetState.Loading
            val result = authRepository.resetPassword(username.trim())
            result.onSuccess { _resetState.value = ResetState.Sent }
                .onFailure { e -> _resetState.value = ResetState.Error(e.message ?: "Failed") }
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) _authState.value = AuthState.Idle
    }

    fun clearResetState() { _resetState.value = ResetState.Idle }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class ResetState {
    object Idle : ResetState()
    object Loading : ResetState()
    object Sent : ResetState()
    data class Error(val message: String) : ResetState()
}
