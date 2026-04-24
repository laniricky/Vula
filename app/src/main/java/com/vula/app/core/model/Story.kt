package com.vula.app.core.model

data class Story(
    val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorProfileImageUrl: String? = null,
    val imageUrl: String = "",
    val createdAt: Long = 0L,
    val isViewed: Boolean = false // Client-side property
)
