package com.vula.app.global.data

import com.google.firebase.firestore.FirebaseFirestore
import com.vula.app.core.util.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FollowRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FollowRepository {

    override fun isFollowing(targetUserId: String, currentUserId: String): Flow<Boolean> = callbackFlow {
        val followingRef = firestore.collection(Constants.USERS_COLLECTION)
            .document(currentUserId)
            .collection(Constants.FOLLOWING_SUBCOLLECTION)
            .document(targetUserId)

        val listener = followingRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot != null && snapshot.exists())
        }
        awaitClose { listener.remove() }
    }

    override suspend fun followUser(targetUserId: String, currentUserId: String): Result<Unit> {
        return try {
            val currentUserRef = firestore.collection(Constants.USERS_COLLECTION).document(currentUserId)
            val targetUserRef = firestore.collection(Constants.USERS_COLLECTION).document(targetUserId)
            
            val followingRef = currentUserRef.collection(Constants.FOLLOWING_SUBCOLLECTION).document(targetUserId)
            val followerRef = targetUserRef.collection(Constants.FOLLOWERS_SUBCOLLECTION).document(currentUserId)

            firestore.runTransaction { transaction ->
                val followingDoc = transaction.get(followingRef)
                if (!followingDoc.exists()) {
                    val now = System.currentTimeMillis()
                    transaction.set(followingRef, mapOf("followedAt" to now))
                    transaction.set(followerRef, mapOf("followedAt" to now))

                    val currentSnapshot = transaction.get(currentUserRef)
                    val targetSnapshot = transaction.get(targetUserRef)

                    val currentFollowingCount = currentSnapshot.getLong("followingCount") ?: 0
                    val targetFollowersCount = targetSnapshot.getLong("followersCount") ?: 0

                    transaction.update(currentUserRef, "followingCount", currentFollowingCount + 1)
                    transaction.update(targetUserRef, "followersCount", targetFollowersCount + 1)
                }
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unfollowUser(targetUserId: String, currentUserId: String): Result<Unit> {
        return try {
            val currentUserRef = firestore.collection(Constants.USERS_COLLECTION).document(currentUserId)
            val targetUserRef = firestore.collection(Constants.USERS_COLLECTION).document(targetUserId)
            
            val followingRef = currentUserRef.collection(Constants.FOLLOWING_SUBCOLLECTION).document(targetUserId)
            val followerRef = targetUserRef.collection(Constants.FOLLOWERS_SUBCOLLECTION).document(currentUserId)

            firestore.runTransaction { transaction ->
                val followingDoc = transaction.get(followingRef)
                if (followingDoc.exists()) {
                    transaction.delete(followingRef)
                    transaction.delete(followerRef)

                    val currentSnapshot = transaction.get(currentUserRef)
                    val targetSnapshot = transaction.get(targetUserRef)

                    val currentFollowingCount = currentSnapshot.getLong("followingCount") ?: 0
                    val targetFollowersCount = targetSnapshot.getLong("followersCount") ?: 0

                    if (currentFollowingCount > 0) {
                        transaction.update(currentUserRef, "followingCount", currentFollowingCount - 1)
                    }
                    if (targetFollowersCount > 0) {
                        transaction.update(targetUserRef, "followersCount", targetFollowersCount - 1)
                    }
                }
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
