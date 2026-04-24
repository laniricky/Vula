package com.vula.app.global.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.vula.app.core.model.Post
import com.vula.app.core.util.Constants
import kotlinx.coroutines.tasks.await

class FeedPagingSource(
    private val firestore: FirebaseFirestore
) : PagingSource<QuerySnapshot, Post>() {

    override fun getRefreshKey(state: PagingState<QuerySnapshot, Post>): QuerySnapshot? {
        return null
    }

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, Post> {
        return try {
            val query = firestore.collection(Constants.POSTS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(params.loadSize.toLong())

            val currentPage = params.key ?: query.get().await()
            val posts = currentPage.toObjects(Post::class.java)

            val nextPage = if (currentPage.size() > 0 && currentPage.size() == params.loadSize) {
                val lastVisibleProduct = currentPage.documents[currentPage.size() - 1]
                query.startAfter(lastVisibleProduct).get().await()
            } else {
                null
            }

            LoadResult.Page(
                data = posts,
                prevKey = null, // Only paging forward
                nextKey = nextPage
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
