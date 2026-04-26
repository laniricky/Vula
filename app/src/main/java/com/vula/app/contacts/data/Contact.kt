package com.vula.app.contacts.data

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val vulaUserId: String? = null,
    val isOnline: Boolean = false,
    val richStatus: String? = null,
    val lastStoryTimestamp: Long = 0L
)
