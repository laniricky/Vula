package com.vula.app.global.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.vula.app.core.model.Story
import com.vula.app.core.model.User
import com.vula.app.core.util.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class StoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) : StoryRepository {

    override fun getStories(): Flow<List<Story>> = callbackFlow {
        val now = System.currentTimeMillis()
        val query = firestore.collection("stories")
            .whereGreaterThan("expiresAt", now)
            .orderBy("expiresAt", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val stories = snapshot.documents.mapNotNull { it.toObject(Story::class.java) }
                trySend(stories)
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun createStory(mediaUri: Uri, mediaType: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("Not authenticated")
            val userDoc = firestore.collection(Constants.USERS_COLLECTION).document(currentUser.uid).get().await()
            val user = userDoc.toObject(User::class.java) ?: throw Exception("User not found")

            // Upload to storage
            val extension = if (mediaType == "video") "mp4" else "jpg"
            val mediaRef = storage.reference.child("stories/${currentUser.uid}/${UUID.randomUUID()}.$extension")
            mediaRef.putFile(mediaUri).await()
            val mediaUrl = mediaRef.downloadUrl.await().toString()

            val storyRef = firestore.collection("stories").document()
            
            val now = System.currentTimeMillis()
            val expiresAt = now + (24 * 60 * 60 * 1000) // 24 hours

            val story = Story(
                id = storyRef.id,
                authorId = user.id,
                authorUsername = user.username,
                authorProfileImageUrl = user.profileImageUrl,
                imageUrl = mediaUrl,
                mediaType = mediaType,
                createdAt = now,
                expiresAt = expiresAt
            )

            storyRef.set(story).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
