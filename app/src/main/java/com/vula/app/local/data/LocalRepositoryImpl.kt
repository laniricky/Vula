package com.vula.app.local.data

import com.vula.app.core.model.LocalPost
import com.vula.app.core.network.JoinNetworkBody
import com.vula.app.core.network.LocalPostBody
import com.vula.app.core.network.LocalReactBody
import com.vula.app.core.network.VulaApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class LocalRepositoryImpl @Inject constructor(
    private val api: VulaApiService
) : LocalRepository {

    override suspend fun joinNetwork(networkId: String, deviceHash: String, alias: String): Result<Unit> {
        return try {
            val response = api.joinNetwork(JoinNetworkBody(networkId, deviceHash, alias))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Join failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveNetwork(networkId: String, deviceHash: String): Result<Unit> {
        return try {
            val response = api.leaveNetwork(JoinNetworkBody(networkId, deviceHash, alias = ""))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Leave failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun postToLocal(
        networkId: String,
        deviceHash: String,
        alias: String,
        text: String
    ): Result<Unit> {
        return try {
            val response = api.postToLocal(LocalPostBody(networkId, deviceHash, alias, text))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Post failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reactToLocalPost(
        postId: String,
        deviceHash: String,
        emoji: String
    ): Result<Unit> {
        return try {
            val response = api.reactToLocalPost(postId, LocalReactBody(deviceHash, emoji))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("React failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getLocalFeed(networkId: String): Flow<List<LocalPost>> = flow {
        while (true) {
            try {
                val response = api.getLocalFeed(networkId)
                if (response.isSuccessful) {
                    emit(response.body()?.map {
                        LocalPost(
                            id            = it.id,
                            networkId     = it.networkId,
                            alias         = it.alias,
                            deviceIdHash  = it.deviceIdHash,
                            text          = it.text,
                            createdAt     = it.createdAt,
                            expiresAt     = it.expiresAt,
                            reactionsCount = it.reactionsCount
                        )
                    }?.sortedByDescending { it.createdAt } ?: emptyList())
                }
            } catch (_: Exception) {}
            delay(4_000)
        }
    }

    override fun getPeopleHere(networkId: String): Flow<List<String>> = flow {
        while (true) {
            try {
                val response = api.getPeopleHere(networkId)
                if (response.isSuccessful) emit(response.body() ?: emptyList())
            } catch (_: Exception) {}
            delay(6_000)
        }
    }
}
