package com.vula.app.global.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.vula.app.core.model.Post
import com.vula.app.core.network.ApiPost
import com.vula.app.core.network.VulaApiService
import com.vula.app.core.util.Constants

class FeedPagingSource(
    private val api: VulaApiService
) : PagingSource<Int, Post>() {

    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        val page = params.key ?: 1
        return try {
            val response = api.getFeed(page = page, limit = Constants.PAGE_SIZE)
            if (!response.isSuccessful) {
                return LoadResult.Error(Exception("Feed error: ${response.code()}"))
            }
            val posts = response.body()
                ?.map { it.toPost() }
                ?.filter { it.mediaType != "video" } // Videos go to Ripples only
                ?: emptyList()
            LoadResult.Page(
                data     = posts,
                prevKey  = if (page == 1) null else page - 1,
                nextKey  = if (posts.size < Constants.PAGE_SIZE) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private fun ApiPost.toPost() = Post(
        id                    = id,
        authorId              = authorId,
        authorUsername        = authorUsername,
        authorProfileImageUrl = authorProfileImageUrl,
        caption               = caption,
        imageUrl              = imageUrl,
        mediaType             = mediaType,
        likesCount            = likesCount,
        commentsCount         = commentsCount,
        createdAt             = createdAt,
        likedBy               = likedBy,
        reactions             = reactions
    )
}
