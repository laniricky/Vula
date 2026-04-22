package com.vula.app.global.data

import kotlinx.coroutines.flow.Flow

interface FollowRepository {
    fun isFollowing(targetUserId: String, currentUserId: String): Flow<Boolean>
    suspend fun followUser(targetUserId: String, currentUserId: String): Result<Unit>
    suspend fun unfollowUser(targetUserId: String, currentUserId: String): Result<Unit>
}
