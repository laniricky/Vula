package com.vula.app.global.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.vula.app.core.model.Post
import com.vula.app.core.ui.components.AddStoryCard
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
    onNavigateToCreatePost: () -> Unit = {},
    onDmReplyToPost: (Post) -> Unit = {},
    onMenuClick: (() -> Unit)? = null,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val posts = viewModel.posts.collectAsLazyPagingItems()
    val stories by viewModel.stories.collectAsState()
    val contactMap by viewModel.contactMap.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("For you", "Following", "Popular")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {

            // ── Top Bar ───────────────────────────────────────────────────────
            item {
                VulaTopBar(
                    title = "Follow",
                    onMenuClick = onMenuClick
                )
            }

            // ── Stories Row ───────────────────────────────────────────────────
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        AddStoryCard(onClick = onNavigateToCreateStory)
                    }
                    items(stories) { story ->
                        val index = stories.indexOf(story)
                        StoryCard(
                            story = story,
                            onClick = { onNavigateToStory(index) }
                        )
                    }
                }
            }

            // ── Divider ───────────────────────────────────────────────────────
            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    thickness = 1.dp
                )
            }

            // ── "What's on your mind?" bar ────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onNavigateToCreatePost() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small avatar placeholder
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "What's on your mind?",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Add image",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Default.AddBox,
                        contentDescription = "Add post",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // ── Tabs: For you / Following / Popular ───────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, label ->
                        val isSelected = selectedTab == index
                        Column(
                            modifier = Modifier
                                .clickable { selectedTab = index }
                                .padding(end = 20.dp, bottom = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onBackground
                                else
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 3.dp)
                                        .width(24.dp)
                                        .height(2.5f.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.onBackground)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filter",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    thickness = 1.dp
                )
            }

            // ── Initial loading skeletons ─────────────────────────────────────
            if (posts.loadState.refresh is LoadState.Loading) {
                items(3) { SkeletonPostCard() }
            }

            // ── Empty state ───────────────────────────────────────────────────
            if (posts.loadState.refresh is LoadState.NotLoading && posts.itemCount == 0) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📸", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No posts yet. Be the first!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Feed Posts ────────────────────────────────────────────────────
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

            // ── Append loading ────────────────────────────────────────────────
            if (posts.loadState.append is LoadState.Loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}
