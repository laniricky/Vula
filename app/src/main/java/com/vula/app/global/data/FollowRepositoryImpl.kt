package com.vula.app.global.data

import com.vula.app.core.network.VulaApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class FollowRepositoryImpl @Inject constructor(
    private val api: VulaApiService
) : FollowRepository {

    override fun isFollowing(targetUserId: String, currentUserId: String): Flow<Boolean> = flow {
        try {
            val response = api.getFollowStatus(targetUserId)
            emit(response.body()?.isFollowing == true)
        } catch (e: Exception) {
            emit(false)
        }
    }

    override suspend fun followUser(targetUserId: String, currentUserId: String): Result<Unit> {
        return try {
            val response = api.followUser(targetUserId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Follow failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unfollowUser(targetUserId: String, currentUserId: String): Result<Unit> {
        return try {
            val response = api.unfollowUser(targetUserId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Unfollow failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
