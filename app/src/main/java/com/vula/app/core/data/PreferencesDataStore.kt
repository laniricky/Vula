package com.vula.app.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
    }

    val hasSeenOnboarding: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[HAS_SEEN_ONBOARDING] ?: false }

    suspend fun setHasSeenOnboarding(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[HAS_SEEN_ONBOARDING] = value
        }
    }
}
