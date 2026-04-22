package com.vula.app.global.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.vula.app.core.model.Comment
import com.vula.app.core.model.Post
import com.vula.app.core.model.User
import com.vula.app.core.util.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) : PostRepository {

    override fun getGlobalFeed(): Flow<List<Post>> = callbackFlow {
        val query = firestore.collection(Constants.POSTS_COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(Constants.PAGE_SIZE.toLong())

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val posts = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
                trySend(posts)
            }
        }
        awaitClose { listener.remove() }
    }

    override fun getUserPosts(userId: String): Flow<List<Post>> = callbackFlow {
        val query = firestore.collection(Constants.POSTS_COLLECTION)
            .whereEqualTo("authorId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val posts = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
                trySend(posts)
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun createPost(caption: String, imageUri: Uri?): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("Not authenticated")
            val userDoc = firestore.collection(Constants.USERS_COLLECTION).document(currentUser.uid).get().await()
            val user = userDoc.toObject(User::class.java) ?: throw Exception("User not found")

            var imageUrl: String? = null

            if (imageUri != null) {
                val imageRef = storage.reference.child("posts/${UUID.randomUUID()}.jpg")
                imageRef.putFile(imageUri).await()
                imageUrl = imageRef.downloadUrl.await().toString()
            }

            val postRef = firestore.collection(Constants.POSTS_COLLECTION).document()
            val post = Post(
                id = postRef.id,
                authorId = user.id,
                authorUsername = user.username,
                authorProfileImageUrl = user.profileImageUrl,
                caption = caption,
                imageUrl = imageUrl,
                createdAt = System.currentTimeMillis()
            )

            postRef.set(post).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun likePost(postId: String, userId: String): Result<Unit> {
        return try {
            val postRef = firestore.collection(Constants.POSTS_COLLECTION).document(postId)
            val likeRef = postRef.collection(Constants.LIKES_SUBCOLLECTION).document(userId)

            // Only like if not already liked (guard read outside batch is acceptable for MVP)
            val likeDoc = likeRef.get().await()
            if (!likeDoc.exists()) {
                firestore.runBatch { batch ->
                    batch.set(likeRef, mapOf("likedAt" to System.currentTimeMillis()))
                    batch.update(postRef, "likesCount",
                        com.google.firebase.firestore.FieldValue.increment(1))
                    batch.update(postRef, "likedBy",
                        com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                }.await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unlikePost(postId: String, userId: String): Result<Unit> {
        return try {
            val postRef = firestore.collection(Constants.POSTS_COLLECTION).document(postId)
            val likeRef = postRef.collection(Constants.LIKES_SUBCOLLECTION).document(userId)

            val likeDoc = likeRef.get().await()
            if (likeDoc.exists()) {
                firestore.runBatch { batch ->
                    batch.delete(likeRef)
                    batch.update(postRef, "likesCount",
                        com.google.firebase.firestore.FieldValue.increment(-1))
                    batch.update(postRef, "likedBy",
                        com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                }.await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addComment(postId: String, text: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("Not authenticated")
            val userDoc = firestore.collection(Constants.USERS_COLLECTION).document(currentUser.uid).get().await()
            val user = userDoc.toObject(User::class.java) ?: throw Exception("User not found")

            val postRef = firestore.collection(Constants.POSTS_COLLECTION).document(postId)
            val commentRef = postRef.collection(Constants.COMMENTS_SUBCOLLECTION).document()

            val comment = Comment(
                id = commentRef.id,
                authorId = user.id,
                authorUsername = user.username,
                text = text,
                createdAt = System.currentTimeMillis()
            )

            firestore.runTransaction { transaction ->
                transaction.set(commentRef, comment)
                val postSnapshot = transaction.get(postRef)
                val currentComments = postSnapshot.getLong("commentsCount") ?: 0
                transaction.update(postRef, "commentsCount", currentComments + 1)
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val query = firestore.collection(Constants.POSTS_COLLECTION).document(postId)
            .collection(Constants.COMMENTS_SUBCOLLECTION)
            .orderBy("createdAt", Query.Direction.ASCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val comments = snapshot.documents.mapNotNull { it.toObject(Comment::class.java) }
                trySend(comments)
            }
        }
        awaitClose { listener.remove() }
    }
}
