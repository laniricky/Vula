package com.vula.app.local.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.vula.app.core.model.LocalPost
import com.vula.app.core.util.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class LocalRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : LocalRepository {

    override suspend fun joinNetwork(networkId: String, deviceHash: String, alias: String): Result<Unit> {
        return try {
            val presenceRef = firestore.collection(Constants.LOCAL_PRESENCE_COLLECTION)
                .document(networkId)
                .collection("users")
                .document(deviceHash)

            presenceRef.set(
                mapOf(
                    "alias" to alias,
                    "lastSeen" to System.currentTimeMillis(),
                    "joinedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveNetwork(networkId: String, deviceHash: String): Result<Unit> {
        return try {
            firestore.collection(Constants.LOCAL_PRESENCE_COLLECTION)
                .document(networkId)
                .collection("users")
                .document(deviceHash)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun postToLocal(networkId: String, deviceHash: String, alias: String, text: String): Result<Unit> {
        return try {
            val postRef = firestore.collection(Constants.LOCAL_POSTS_COLLECTION).document()
            val now = System.currentTimeMillis()
            val expiry = now + (Constants.LOCAL_POST_EXPIRY_HOURS * 60 * 60 * 1000)

            val post = LocalPost(
                id = postRef.id,
                networkId = networkId,
                alias = alias,
                deviceIdHash = deviceHash,
                text = text,
                createdAt = now,
                expiresAt = expiry,
                reactionsCount = 0
            )

            postRef.set(post).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getLocalFeed(networkId: String): Flow<List<LocalPost>> = callbackFlow {
        val now = System.currentTimeMillis()
        val query = firestore.collection(Constants.LOCAL_POSTS_COLLECTION)
            .whereEqualTo("networkId", networkId)
            .whereGreaterThan("expiresAt", now)
            .orderBy("expiresAt", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val posts = snapshot.documents.mapNotNull { it.toObject(LocalPost::class.java) }
                    .sortedByDescending { it.createdAt }
                trySend(posts)
            }
        }
        awaitClose { listener.remove() }
    }

    override fun getPeopleHere(networkId: String): Flow<List<String>> = callbackFlow {
        val query = firestore.collection(Constants.LOCAL_PRESENCE_COLLECTION)
            .document(networkId)
            .collection("users")

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val aliases = snapshot.documents.mapNotNull { it.getString("alias") }
                trySend(aliases)
            }
        }
        awaitClose { listener.remove() }
    }
}
