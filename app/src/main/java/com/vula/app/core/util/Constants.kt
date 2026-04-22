package com.vula.app.core.util

object Constants {
    // Firestore Collections
    const val USERS_COLLECTION = "users"
    const val USERNAMES_COLLECTION = "usernames"
    const val POSTS_COLLECTION = "posts"
    const val CHAT_ROOMS_COLLECTION = "chatRooms"
    const val MESSAGE_REQUESTS_COLLECTION = "messageRequests"
    const val LOCAL_POSTS_COLLECTION = "localPosts"
    const val LOCAL_PRESENCE_COLLECTION = "localPresence"

    // Subcollections
    const val COMMENTS_SUBCOLLECTION = "comments"
    const val LIKES_SUBCOLLECTION = "likes"
    const val FOLLOWERS_SUBCOLLECTION = "followers"
    const val FOLLOWING_SUBCOLLECTION = "following"
    const val MESSAGES_SUBCOLLECTION = "messages"
    const val REACTIONS_SUBCOLLECTION = "reactions"

    // Limits
    const val PAGE_SIZE = 20
    const val LOCAL_POST_EXPIRY_HOURS = 24L
}
