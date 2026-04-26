package com.vula.app.core.model

data class User(
    val id: String = "",
    val username: String = "",
    val phoneNumber: String = "",
    val phoneHash: String = "", // Used for privacy-first contact matching
    val displayName: String = "",
    val bio: String = "",
    val profileImageUrl: String? = null,
    val createdAt: Long = 0L,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val isOnline: Boolean = false,
    val richStatus: String? = null, // e.g. "Listening to Spotify"
    val lastStoryTimestamp: Long = 0L
)
