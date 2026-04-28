package com.vula.app.global.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.vula.app.core.model.Post
import com.vula.app.core.ui.components.FullScreenLoading
import com.vula.app.core.ui.components.SkeletonPostCard
import com.vula.app.core.ui.components.StoryCard
import com.vula.app.core.ui.components.VulaTopBar
import com.vula.app.global.ui.components.PostCard

@Composable
fun FeedScreen(
    currentUserId: String,
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToComments: (String) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToStory: (Int) -> Unit = {},
    onNavigateToCreateStory: () -> Unit = {},
    onDmReplyToPost: (Post) -> Unit = {},
    onMenuClick: (() -> Unit)? = null,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val posts = viewModel.posts.collectAsLazyPagingItems()
    val stories by viewModel.stories.collectAsState()
    val contactMap by viewModel.contactMap.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            VulaTopBar(
                title = "Vula",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search users")
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Stories Row
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            com.vula.app.core.ui.components.AddStoryCard(onClick = onNavigateToCreateStory)
                        }
                        items(stories) { story ->
                            val index = stories.indexOf(story)
                            StoryCard(
                                story = story,
                                onClick = { onNavigateToStory(index) }
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                }

                // Initial loading state
                if (posts.loadState.refresh is LoadState.Loading) {
                    items(3) { SkeletonPostCard() }
                }

                // Empty state
                if (posts.loadState.refresh is LoadState.NotLoading && posts.itemCount == 0) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📸", style = MaterialTheme.typography.displayMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No posts yet. Be the first!",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Feed Posts
                items(
                    count = posts.itemCount,
                    key = posts.itemKey { it.id }
                ) { index ->
                    val post = posts[index]
                    if (post != null) {
                        val contactName = contactMap[post.authorId]
                        PostCard(
                            post = post,
                            currentUserId = currentUserId,
                            contactName = contactName,
                            onLikeClick    = { viewModel.likePost(it, currentUserId) },
                            onUnlikeClick  = { viewModel.unlikePost(it, currentUserId) },
                            onReactToPost  = { postId, emoji -> viewModel.reactToPost(postId, currentUserId, emoji) },
                            onRemoveReaction = { postId -> viewModel.removeReaction(postId, currentUserId) },
                            onCommentClick = { onNavigateToComments(it) },
                            onDmReplyToPost = { onDmReplyToPost(it) },
                            onUserClick    = { onNavigateToProfile(it) }
                        )
                    }
                }

                // Append loading state
                if (posts.loadState.append is LoadState.Loading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

