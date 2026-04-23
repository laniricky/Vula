package com.vula.app.contacts.data

import android.content.Context
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface ContactsRepository {
    suspend fun getContacts(): List<Contact>
}

class ContactsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ContactsRepository {

    override suspend fun getContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val contactsList = mutableListOf<Contact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (cursor.moveToNext()) {
                val id = if (idIndex >= 0) cursor.getString(idIndex) else ""
                val name = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else ""
                val number = if (numberIndex >= 0) cursor.getString(numberIndex) ?: "" else ""

                if (name.isNotEmpty() && number.isNotEmpty()) {
                    contactsList.add(Contact(id = id, name = name, phoneNumber = number))
                }
            }
        }
        
        // Remove duplicates based on phone number without spaces
        contactsList.distinctBy { it.phoneNumber.replace("\\s".toRegex(), "") }
    }
}
