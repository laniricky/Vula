package com.vula.app.global.ui.feed

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.vula.app.core.model.Post
import com.vula.app.core.model.Story
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
    val posts      = viewModel.posts.collectAsLazyPagingItems()
    val stories    by viewModel.stories.collectAsState()
    val contactMap by viewModel.contactMap.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val listState   = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        LazyColumn(
            state          = listState,
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {

            // ── Top bar matching Activity/Profile theme ───────────────────────
            item {
                VulaTopBar(
                    title     = "Home",
                    showStats = false,
                    actions   = {
                        IconButton(onClick = onNavigateToSearch) {
                            Icon(Icons.Default.Search, contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onBackground)
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                )
            }

            // ── Stories bar ───────────────────────────────────────────────────
            item {
                FeedStoriesRow(
                    stories          = stories,
                    onStoryClick     = onNavigateToStory,
                    onAddStoryClick  = onNavigateToCreateStory
                )
            }

            // ── For You / Following FilterChip row ────────────────────────────
            item {
                FeedTabChips(
                    selectedTab   = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }

            // ── Skeleton loading ──────────────────────────────────────────────
            if (posts.loadState.refresh is LoadState.Loading) {
                items(3) { SkeletonPostCard() }
            }

            // ── Empty state ───────────────────────────────────────────────────
            if (posts.loadState.refresh is LoadState.NotLoading && posts.itemCount == 0) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(vertical = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📸", fontSize = 52.sp)
                            Spacer(Modifier.height(14.dp))
                            Text("Nothing here yet.", fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground)
                            Spacer(Modifier.height(6.dp))
                            Text("Follow people to see their posts",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(20.dp))
                            Button(onClick = onNavigateToSearch, shape = RoundedCornerShape(14.dp)) {
                                Text("Find people")
                            }
                        }
                    }
                }
            }

            // ── Posts ─────────────────────────────────────────────────────────
            items(count = posts.itemCount, key = posts.itemKey { it.id }) { index ->
                val post = posts[index]
                if (post != null) {
                    PostCard(
                        post             = post,
                        currentUserId    = currentUserId,
                        contactName      = contactMap[post.authorId],
                        onLikeClick      = { viewModel.likePost(it, currentUserId) },
                        onUnlikeClick    = { viewModel.unlikePost(it, currentUserId) },
                        onReactToPost    = { id, emoji -> viewModel.reactToPost(id, currentUserId, emoji) },
                        onRemoveReaction = { viewModel.removeReaction(it, currentUserId) },
                        onCommentClick   = { onNavigateToComments(it) },
                        onDmReplyToPost  = { onDmReplyToPost(it) },
                        onUserClick      = { onNavigateToProfile(it) }
                    )
                }
            }

            // ── Append loading ─────────────────────────────────────────────────
            if (posts.loadState.append is LoadState.Loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(20.dp), Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}

// ── Compact stories row ───────────────────────────────────────────────────────

@Composable
private fun FeedStoriesRow(
    stories: List<Story>,
    onStoryClick: (Int) -> Unit,
    onAddStoryClick: () -> Unit
) {
    LazyRow(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentPadding    = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { AddStoryCard(onClick = onAddStoryClick) }
        items(stories, key = { it.id }) { story ->
            val index = stories.indexOf(story)
            StoryCard(story = story, onClick = { onStoryClick(index) })
        }
    }
}

// ── FilterChip tab row (matches Activity tab style) ──────────────────────────

@Composable
private fun FeedTabChips(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs  = listOf("For You", "Following")
    val haptic = LocalHapticFeedback.current
    LazyRow(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tabs.size) { i ->
            val isSelected = selectedTab == i
            FilterChip(
                selected = isSelected,
                onClick  = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(i)
                },
                label = {
                    Text(
                        tabs[i],
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                    selectedLabelColor     = MaterialTheme.colorScheme.background
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
