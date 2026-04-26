package com.vula.app.contacts.data

import com.google.firebase.firestore.FirebaseFirestore
import com.vula.app.core.model.User
import com.vula.app.core.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactSyncManager @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val firestore: FirebaseFirestore
) {
    private val _contactMap = MutableStateFlow<Map<String, String>>(emptyMap())
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

                val cleanLocalContacts = localContacts.map { 
                    it.copy(phoneNumber = cleanPhoneNumber(it.phoneNumber))
                }.filter { it.phoneNumber.isNotEmpty() }

                val phoneToNameMap = cleanLocalContacts.associateBy({ it.phoneNumber }, { it.name })
                val phoneNumbersToQuery = phoneToNameMap.keys.toList()
                
                // Hash all phone numbers locally before sending to server
                val hashedPhoneToOriginalMap = phoneNumbersToQuery.associateBy { phone -> 
                    com.vula.app.core.util.HashUtils.sha256(phone) 
                }
                val hashesToQuery = hashedPhoneToOriginalMap.keys.toList()

                val chunks = hashesToQuery.chunked(10)
                val newContactMap = mutableMapOf<String, String>()
                val newPhoneMap = mutableMapOf<String, String>()
                val newSyncedUsersMap = mutableMapOf<String, User>()

                for (chunk in chunks) {
                    val snapshot = firestore.collection(Constants.USERS_COLLECTION)
                        .whereIn("phoneHash", chunk)
                        .get()
                        .await()

                    for (doc in snapshot.documents) {
                        val user = doc.toObject(User::class.java)
                        if (user != null && user.phoneHash.isNotEmpty()) {
                            val originalPhone = hashedPhoneToOriginalMap[user.phoneHash]
                            if (originalPhone != null) {
                                val contactName = phoneToNameMap[originalPhone]
                                if (contactName != null) {
                                    newContactMap[user.id] = contactName
                                    newPhoneMap[originalPhone] = user.id
                                    newSyncedUsersMap[user.id] = user
                                }
                            }
                        }
                    }
                }

                _contactMap.value = newContactMap
                _phoneToVulaIdMap.value = newPhoneMap
                _syncedUsers.value = newSyncedUsersMap
            } catch (e: SecurityException) {
                // Permission not granted
            } catch (e: Exception) {
                // Other error
            }
        }
    }

    private fun cleanPhoneNumber(phone: String): String {
        return phone.replace(Regex("[^0-9+]"), "")
    }
}
