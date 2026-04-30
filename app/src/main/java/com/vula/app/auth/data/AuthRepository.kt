package com.vula.app.auth.data

import com.vula.app.core.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isUserLoggedIn: Flow<Boolean>
    val currentUser: Flow<User?>
    suspend fun requestCode(phoneNumber: String): Result<Unit>
    suspend fun verifyCode(phoneNumber: String, code: String): Result<Unit>
    suspend fun logout()
}
