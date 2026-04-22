package com.vula.app.core.model

data class User(
    val id: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val profileImageUrl: String? = null,
    val createdAt: Long = 0L,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0
)
