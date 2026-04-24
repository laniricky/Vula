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

    // Set of local contact phone-based IDs that are matched to Vula users
    // contactMap maps vulaUserId -> contactName; we expose the keys as "on Vula" ids
    val vulaContactIds: StateFlow<Set<String>> = contactSyncManager.contactMap
        .map { it.keys }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

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
