package com.vula.app.core.model

data class Post(
    val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorProfileImageUrl: String? = null,
    val caption: String = "",
    val imageUrl: String? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val createdAt: Long = 0L
)
