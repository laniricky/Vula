package com.vula.app.auth.data

import com.vula.app.core.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isUserLoggedIn: Boolean
    val currentUser: Flow<User?>
    suspend fun register(username: String, password: String): Result<Unit>
    suspend fun login(username: String, password: String): Result<Unit>
    suspend fun resetPassword(username: String): Result<Unit>
    fun logout()
}
