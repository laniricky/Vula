package com.vula.app.auth.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vula.app.core.model.User
import com.vula.app.core.network.RequestCodeBody
import com.vula.app.core.network.VerifyCodeBody
import com.vula.app.core.network.VulaApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: VulaApiService,
    private val dataStore: DataStore<Preferences>
) : AuthRepository {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
    }

    override val isUserLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[TOKEN_KEY] != null
    }

    override val currentUser: Flow<User?> = dataStore.data.map { preferences ->
        // TODO: In Phase 4, we'll fetch the full User object from Postgres /api/users/me
        val token = preferences[TOKEN_KEY]
        if (token != null) {
            User(id = "temp", username = "user", phoneNumber = "", phoneHash = "", displayName = "", createdAt = 0L)
        } else {
            null
        }
    }

    override suspend fun requestCode(phoneNumber: String): Result<Unit> {
        return try {
            val response = apiService.requestCode(RequestCodeBody(phone = phoneNumber))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to request code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyCode(phoneNumber: String, code: String): Result<Unit> {
        return try {
            val response = apiService.verifyCode(VerifyCodeBody(phone = phoneNumber, code = code))
            if (response.isSuccessful && response.body() != null) {
                val token = response.body()!!.token
                dataStore.edit { preferences ->
                    preferences[TOKEN_KEY] = token
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Invalid code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
        }
    }
}
