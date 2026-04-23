package com.vula.app.contacts.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vula.app.contacts.data.Contact
import com.vula.app.contacts.data.ContactsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadContacts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fetchedContacts = contactsRepository.getContacts()
                _contacts.value = fetchedContacts
            } catch (e: Exception) {
                // Handle error if necessary
                _contacts.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
