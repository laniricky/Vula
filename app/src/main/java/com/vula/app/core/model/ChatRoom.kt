package com.vula.app.core.model

data class ChatRoom(
    val id: String = "",
    val type: String = "direct", // "direct" or "group"
    val participants: List<String> = emptyList(),
    val name: String? = null,
    val lastMessage: String? = null,
    val lastMessageAt: Long = 0L,
    val createdAt: Long = 0L
)
