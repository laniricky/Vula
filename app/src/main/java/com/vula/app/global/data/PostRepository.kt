package com.vula.app.global.data

import android.net.Uri
import com.vula.app.core.model.Comment
import com.vula.app.core.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getGlobalFeed(): Flow<List<Post>>
    fun getUserPosts(userId: String): Flow<List<Post>>
    suspend fun createPost(caption: String, mediaUri: Uri?, mediaType: String = "image"): Result<Unit>
    suspend fun likePost(postId: String, userId: String): Result<Unit>
    suspend fun unlikePost(postId: String, userId: String): Result<Unit>
    suspend fun reactToPost(postId: String, userId: String, emoji: String): Result<Unit>
    suspend fun removeReaction(postId: String, userId: String): Result<Unit>
    suspend fun addComment(postId: String, text: String): Result<Unit>
    fun getComments(postId: String): Flow<List<Comment>>
}
