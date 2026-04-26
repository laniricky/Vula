package com.vula.app.contacts.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vula.app.contacts.data.Contact
import com.vula.app.contacts.data.ContactSyncManager
import com.vula.app.contacts.data.ContactsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val contactSyncManager: ContactSyncManager
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Map of clean phone number -> Vula User ID
    val phoneToVulaIdMap: StateFlow<Map<String, String>> = contactSyncManager.phoneToVulaIdMap
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
        
    private val _syncedUsers = contactSyncManager.syncedUsers

    val processedContacts: StateFlow<List<Contact>> = kotlinx.coroutines.flow.combine(
        _contacts,
        phoneToVulaIdMap,
        _syncedUsers
    ) { contactsList, phoneMap, userMap ->
        contactsList.map { contact ->
            val cleanPhone = cleanPhoneNumber(contact.phoneNumber)
            val vulaId = phoneMap[cleanPhone]
            val user = vulaId?.let { userMap[it] }
            contact.copy(
                vulaUserId = vulaId,
                isOnline = user?.isOnline ?: false,
                richStatus = user?.richStatus,
                lastStoryTimestamp = user?.lastStoryTimestamp ?: 0L
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun cleanPhoneNumber(phone: String): String {
        return phone.replace(Regex("[^0-9+]"), "")
    }

    fun loadContacts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fetched = contactsRepository.getContacts()
                _contacts.value = fetched
                // Sync against Firestore to find which contacts are on Vula
                contactSyncManager.syncContacts()
            } catch (e: Exception) {
                _contacts.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
