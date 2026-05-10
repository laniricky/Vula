package com.vula.app.auth.data

import com.vula.app.core.data.SessionManager
import com.vula.app.core.model.User
import com.vula.app.core.network.RequestCodeBody
import com.vula.app.core.network.VerifyCodeBody
import com.vula.app.core.network.VulaApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: VulaApiService,
    private val sessionManager: SessionManager
) : AuthRepository {

    override val isUserLoggedIn: Flow<Boolean> = sessionManager.isLoggedIn

    override val currentUser: Flow<User?> = sessionManager.userId.map { userId ->
        if (userId != null) {
            // Basic stub — full profile loaded by ProfileViewModel via /api/users/me
            User(
                id          = userId,
                username    = "",
                phoneNumber = "",
                phoneHash   = "",
                displayName = "",
                createdAt   = 0L
            )
        } else null
    }

    override suspend fun requestCode(phoneNumber: String): Result<Unit> {
        return try {
            val response = apiService.requestCode(RequestCodeBody(phone = phoneNumber))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Failed to request code: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyCode(phoneNumber: String, code: String): Result<Unit> {
        return try {
            val response = apiService.verifyCode(VerifyCodeBody(phone = phoneNumber, code = code))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                sessionManager.saveSession(
                    token    = body.token,
                    userId   = body.userId,
                    username = "" // fetched later from /api/users/me
                )
                Result.success(Unit)
            } else {
                Result.failure(Exception("Invalid code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        sessionManager.clearSession()
    }
}
