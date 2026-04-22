package com.vula.app.core.model

data class MessageRequest(
    val id: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val fromProfileImageUrl: String? = null,
    val toUserId: String = "",
    val status: String = "pending", // "pending" | "accepted" | "declined"
    val createdAt: Long = 0L
)
