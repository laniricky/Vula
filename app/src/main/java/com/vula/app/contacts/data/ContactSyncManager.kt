package com.vula.app.contacts.data

import com.vula.app.core.model.User
import com.vula.app.core.network.VulaApiService
import com.vula.app.core.util.HashUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactSyncManager @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val api: VulaApiService
) {
    private val _contactMap   = MutableStateFlow<Map<String, String>>(emptyMap())
    val contactMap: StateFlow<Map<String, String>> = _contactMap.asStateFlow()

    private val _phoneToVulaIdMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val phoneToVulaIdMap: StateFlow<Map<String, String>> = _phoneToVulaIdMap.asStateFlow()

    private val _syncedUsers = MutableStateFlow<Map<String, User>>(emptyMap())
    val syncedUsers: StateFlow<Map<String, User>> = _syncedUsers.asStateFlow()

    fun syncContacts() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val localContacts = contactsRepository.getContacts()
                if (localContacts.isEmpty()) return@launch

                val cleaned = localContacts
                    .map { it.copy(phoneNumber = cleanPhoneNumber(it.phoneNumber)) }
                    .filter { it.phoneNumber.isNotEmpty() }

                val phoneToName = cleaned.associateBy({ it.phoneNumber }, { it.name })
                val hashes      = phoneToName.keys.map { HashUtils.sha256(it) }
                val hashToPhone = phoneToName.keys.zip(hashes).associate { (phone, hash) -> hash to phone }

                // Send hashed phone numbers in chunks of 10 to the backend's /api/users/contacts endpoint
                val newContactMap    = mutableMapOf<String, String>()
                val newPhoneMap      = mutableMapOf<String, String>()
                val newSyncedUsers   = mutableMapOf<String, User>()

                hashes.chunked(10).forEach { chunk ->
                    try {
                        // The backend accepts a comma-separated list of hashes as a query param
                        val response = api.searchUsers(query = chunk.joinToString(","), limit = 10)
                        if (response.isSuccessful) {
                            response.body()?.forEach { apiUser ->
                                val originalPhone = hashToPhone[apiUser.id] // fallback: try id as hash key
                                    ?: hashToPhone.entries.firstOrNull { (_, phone) ->
                                        HashUtils.sha256(phone) == apiUser.id
                                    }?.value
                                    ?: return@forEach

                                val contactName = phoneToName[originalPhone] ?: return@forEach
                                val user = User(
                                    id             = apiUser.id,
                                    username       = apiUser.username,
                                    phoneNumber    = apiUser.phoneNumber,
                                    phoneHash      = HashUtils.sha256(apiUser.phoneNumber),
                                    displayName    = apiUser.displayName,
                                    bio            = apiUser.bio,
                                    profileImageUrl = apiUser.profileImageUrl,
                                    followersCount = apiUser.followersCount,
                                    followingCount = apiUser.followingCount,
                                    postsCount     = apiUser.postsCount,
                                    isOnline       = apiUser.isOnline,
                                    createdAt      = apiUser.createdAt
                                )
                                newContactMap[user.id]   = contactName
                                newPhoneMap[originalPhone] = user.id
                                newSyncedUsers[user.id]  = user
                            }
                        }
                    } catch (_: Exception) { /* skip chunk on error */ }
                }

                _contactMap.value   = newContactMap
                _phoneToVulaIdMap.value = newPhoneMap
                _syncedUsers.value  = newSyncedUsers
            } catch (_: SecurityException) { /* permission not granted */ }
            catch (_: Exception) { /* general error */ }
        }
    }

    private fun cleanPhoneNumber(phone: String): String =
        phone.replace(Regex("[^0-9+]"), "")
}
