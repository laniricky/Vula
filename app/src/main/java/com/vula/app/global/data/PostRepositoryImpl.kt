package com.vula.app.global.data

import android.content.Context
import android.net.Uri
import com.vula.app.core.model.Comment
import com.vula.app.core.model.Post
import com.vula.app.core.network.CommentBody
import com.vula.app.core.network.ReactBody
import com.vula.app.core.network.VulaApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val api: VulaApiService,
    @ApplicationContext private val context: Context
) : PostRepository {

    // ── Feed ─────────────────────────────────────────────────────────────────

    override fun getGlobalFeed(): Flow<List<Post>> = flow {
        val response = api.getFeed()
        if (response.isSuccessful) {
            emit(response.body()?.map { it.toPost() } ?: emptyList())
        } else {
            emit(emptyList())
        }
    }

    override fun getUserPosts(userId: String): Flow<List<Post>> = flow {
        val response = api.getUserPosts(userId)
        if (response.isSuccessful) {
            emit(response.body()?.map { it.toPost() } ?: emptyList())
        } else {
            emit(emptyList())
        }
    }

    // ── Create ────────────────────────────────────────────────────────────────

    override suspend fun createPost(
        caption: String,
        mediaUri: Uri?,
        mediaType: String
    ): Result<Unit> {
        return try {
            val captionBody   = caption.toRequestBody("text/plain".toMediaTypeOrNull())
            val mediaTypeBody = mediaType.toRequestBody("text/plain".toMediaTypeOrNull())

            val mediaPart: MultipartBody.Part? = mediaUri?.let { uri ->
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return Result.failure(Exception("Cannot open media"))
                val ext  = if (mediaType == "video") "mp4" else "jpg"
                val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.$ext")
                FileOutputStream(file).use { out -> inputStream.copyTo(out) }
                val requestBody = file.asRequestBody(
                    (if (mediaType == "video") "video/mp4" else "image/jpeg").toMediaTypeOrNull()
                )
                MultipartBody.Part.createFormData("media", file.name, requestBody)
            }

            val response = api.createPost(mediaPart, captionBody, mediaTypeBody)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Create post failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Reactions ─────────────────────────────────────────────────────────────

    override suspend fun likePost(postId: String, userId: String): Result<Unit> =
        reactToPost(postId, userId, "❤️")

    override suspend fun unlikePost(postId: String, userId: String): Result<Unit> =
        removeReaction(postId, userId)

    override suspend fun reactToPost(
        postId: String,
        userId: String,
        emoji: String
    ): Result<Unit> {
        return try {
            val response = api.reactToPost(postId, ReactBody(emoji))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("React failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeReaction(postId: String, userId: String): Result<Unit> {
        return try {
            val response = api.removeReaction(postId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Remove reaction failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    override suspend fun addComment(postId: String, text: String): Result<Unit> {
        return try {
            val response = api.addComment(postId, CommentBody(text))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Comment failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getComments(postId: String): Flow<List<Comment>> = flow {
        val response = api.getComments(postId)
        if (response.isSuccessful) {
            emit(response.body()?.map {
                Comment(
                    id             = it.id,
                    authorId       = it.authorId,
                    authorUsername = it.authorUsername,
                    text           = it.text,
                    createdAt      = it.createdAt
                )
            } ?: emptyList())
        } else {
            emit(emptyList())
        }
    }
}

// ── Extension ─────────────────────────────────────────────────────────────────

private fun com.vula.app.core.network.ApiPost.toPost() = Post(
    id                    = id,
    authorId              = authorId,
    authorUsername        = authorUsername,
    authorProfileImageUrl = authorProfileImageUrl,
    caption               = caption,
    imageUrl              = imageUrl,
    videoUrl              = videoUrl,
    mediaType             = mediaType,
    likesCount            = likesCount,
    commentsCount         = commentsCount,
    createdAt             = createdAt,
    likedBy               = likedBy,
    reactions             = reactions
)
