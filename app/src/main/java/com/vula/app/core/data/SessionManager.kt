package com.vula.app.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the authenticated session.
 * Replaces FirebaseAuth throughout the app.
 */
@Singleton
class SessionManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val TOKEN_KEY   = stringPreferencesKey("jwt_token")
        val USER_ID_KEY = stringPreferencesKey("current_user_id")
        val USERNAME_KEY = stringPreferencesKey("current_username")
    }

    val token: Flow<String?> = dataStore.data.map { it[TOKEN_KEY] }
    val userId: Flow<String?> = dataStore.data.map { it[USER_ID_KEY] }
    val username: Flow<String?> = dataStore.data.map { it[USERNAME_KEY] }
    val isLoggedIn: Flow<Boolean> = token.map { it != null }

    /** Blocking read for use in interceptors / non-suspend contexts. */
    suspend fun getTokenNow(): String? = dataStore.data.first()[TOKEN_KEY]
    suspend fun getUserIdNow(): String? = dataStore.data.first()[USER_ID_KEY]

    suspend fun saveSession(token: String, userId: String, username: String) {
        dataStore.edit {
            it[TOKEN_KEY]    = token
            it[USER_ID_KEY]  = userId
            it[USERNAME_KEY] = username
        }
    }

    suspend fun clearSession() {
        dataStore.edit { it.clear() }
    }
}
