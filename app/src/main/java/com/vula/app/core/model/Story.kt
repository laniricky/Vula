package com.vula.app.core.model

data class Story(
    val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorProfileImageUrl: String? = null,
    val imageUrl: String = "",
    val mediaType: String = "image", // "image" or "video"
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val isViewed: Boolean = false // Client-side property
) {
    // Alias for clarity in the Story-to-Chat funnel
    val authorUserId: String get() = authorId
}
