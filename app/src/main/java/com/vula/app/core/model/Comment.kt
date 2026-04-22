package com.vula.app.core.model

data class Comment(
    val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)
