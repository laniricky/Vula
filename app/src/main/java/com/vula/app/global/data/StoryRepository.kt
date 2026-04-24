package com.vula.app.global.data

import android.net.Uri
import com.vula.app.core.model.Story
import kotlinx.coroutines.flow.Flow

interface StoryRepository {
    fun getStories(): Flow<List<Story>>
    suspend fun createStory(mediaUri: Uri, mediaType: String): Result<Unit>
}
