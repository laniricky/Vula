package com.vula.app.core.model

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val text: String = "",
    val createdAt: Long = 0L,
    val readBy: List<String> = emptyList()
)
