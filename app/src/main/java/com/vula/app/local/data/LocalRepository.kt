package com.vula.app.local.data

import com.vula.app.core.model.LocalPost
import kotlinx.coroutines.flow.Flow

interface LocalRepository {
    suspend fun joinNetwork(networkId: String, deviceHash: String, alias: String): Result<Unit>
    suspend fun leaveNetwork(networkId: String, deviceHash: String): Result<Unit>
    suspend fun postToLocal(networkId: String, deviceHash: String, alias: String, text: String): Result<Unit>
    fun getLocalFeed(networkId: String): Flow<List<LocalPost>>
    fun getPeopleHere(networkId: String): Flow<List<String>>
}
