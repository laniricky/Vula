package com.vula.app.core.util

object Constants {
    // REST API route segments (for logging/reference; Retrofit uses annotations)
    const val USERS_PATH         = "users"
    const val POSTS_PATH         = "posts"
    const val STORIES_PATH       = "stories"
    const val CHAT_PATH          = "chat"
    const val LOCAL_PATH         = "local"

    // Pagination
    const val PAGE_SIZE                = 20
    const val LOCAL_POST_EXPIRY_HOURS  = 24L
}
