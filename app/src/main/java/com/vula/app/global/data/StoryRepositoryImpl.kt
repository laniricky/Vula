package com.vula.app.global.data

import android.content.Context
import android.net.Uri
import com.vula.app.core.model.Story
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

class StoryRepositoryImpl @Inject constructor(
    private val api: VulaApiService,
    @ApplicationContext private val context: Context
) : StoryRepository {

    override fun getStories(): Flow<List<Story>> = flow {
        val response = api.getStories()
        if (response.isSuccessful) {
            emit(response.body()?.map {
                Story(
                    id                    = it.id,
                    authorId              = it.authorId,
                    authorUsername        = it.authorUsername,
                    authorProfileImageUrl = it.authorProfileImageUrl,
                    imageUrl              = it.imageUrl,
                    mediaType             = it.mediaType,
                    createdAt             = it.createdAt,
                    expiresAt             = it.expiresAt
                )
            } ?: emptyList())
        } else {
            emit(emptyList())
        }
    }

    override suspend fun createStory(mediaUri: Uri, mediaType: String): Result<Unit> {
        return try {
            val inputStream = context.contentResolver.openInputStream(mediaUri)
                ?: return Result.failure(Exception("Cannot open media"))
            val ext  = if (mediaType == "video") "mp4" else "jpg"
            val file = File(context.cacheDir, "story_${System.currentTimeMillis()}.$ext")
            FileOutputStream(file).use { out -> inputStream.copyTo(out) }

            val mime        = if (mediaType == "video") "video/mp4" else "image/jpeg"
            val requestBody = file.asRequestBody(mime.toMediaTypeOrNull())
            val mediaPart   = MultipartBody.Part.createFormData("media", file.name, requestBody)
            val typePart    = mediaType.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = api.createStory(mediaPart, typePart)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Create story failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
