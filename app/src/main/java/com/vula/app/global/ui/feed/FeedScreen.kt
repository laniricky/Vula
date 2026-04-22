package com.vula.app.global.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.vula.app.core.ui.components.FullScreenLoading
import com.vula.app.core.ui.components.VulaTopBar
import com.vula.app.global.ui.components.PostCard

@Composable
fun FeedScreen(
    currentUserId: String,
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToComments: (String) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            VulaTopBar(
                title = "Vula",
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search users")
                    }
                }
            )

            when (uiState) {
                is FeedUiState.Loading -> FullScreenLoading()
                is FeedUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = (uiState as FeedUiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is FeedUiState.Success -> {
                    val posts = (uiState as FeedUiState.Success).posts
                    if (posts.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📸", style = MaterialTheme.typography.displayMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No posts yet. Be the first!",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(posts, key = { it.id }) { post ->
                                PostCard(
                                    post = post,
                                    currentUserId = currentUserId,
                                    onLikeClick    = { viewModel.likePost(it, currentUserId) },
                                    onUnlikeClick  = { viewModel.unlikePost(it, currentUserId) },
                                    onCommentClick = { onNavigateToComments(it) },
                                    onUserClick    = { onNavigateToProfile(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
