package com.vula.app.auth.data

import com.vula.app.core.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isUserLoggedIn: Boolean
    val currentUser: Flow<User?>
    suspend fun register(phoneNumber: String, username: String, password: String): Result<Unit>
    suspend fun login(phoneNumber: String, password: String): Result<Unit>
    suspend fun resetPassword(phoneNumber: String): Result<Unit>
    fun logout()
}
