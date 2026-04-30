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

    // Expose login state as a Flow from DataStore (JWT token presence)
    val isUserLoggedIn = authRepository.isUserLoggedIn

    val currentUser = authRepository.currentUser

    /**
     * Step 1 of OTP flow: ask the backend to send a code to the phone number.
     */
    fun requestCode(countryCode: String, phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            _authState.value = AuthState.Error("Phone number cannot be empty")
            return
        }
        val fullPhone = "$countryCode${phoneNumber.trim()}"
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.requestCode(fullPhone)
            result
                .onSuccess { _authState.value = AuthState.CodeSent(fullPhone) }
                .onFailure { e -> _authState.value = AuthState.Error(e.message ?: "Failed to send code") }
        }
    }

    /**
     * Step 2 of OTP flow: verify the code entered by the user.
     */
    fun verifyCode(phoneNumber: String, code: String) {
        if (code.isBlank()) {
            _authState.value = AuthState.Error("Code cannot be empty")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.verifyCode(phoneNumber, code)
            result
                .onSuccess { _authState.value = AuthState.Success }
                .onFailure { e -> _authState.value = AuthState.Error(e.message ?: "Invalid code") }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = AuthState.Idle
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    /** Emitted after the OTP code is dispatched — carries the phone number for the verify step. */
    data class CodeSent(val phone: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
