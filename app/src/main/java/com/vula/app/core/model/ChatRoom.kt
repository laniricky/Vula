package com.vula.app.core.model

data class ChatRoom(
    val id: String = "",
    val type: String = "direct", // "direct" or "group"
    val participants: List<String> = emptyList(),
    val name: String? = null,
    val lastMessage: String? = null,
    val lastMessageAt: Long = 0L,
    val lastMessageSenderId: String? = null,
    // Participant IDs who haven't read the latest message
    val unreadFor: List<String> = emptyList(),
    // IDs of users currently typing
    val typingUsers: List<String> = emptyList(),
    val createdAt: Long = 0L
)
