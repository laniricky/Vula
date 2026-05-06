package com.vula.app.global.ui.discover

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vula.app.core.model.User
import com.vula.app.core.ui.components.UserAvatar

// ─── Discover Screen ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    currentUserId: String,
    onUserClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onCreatePost: () -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val uiState        by viewModel.uiState.collectAsState()
    val activeFilter   by viewModel.activeFilter.collectAsState()
    val selectedTopic  by viewModel.selectedTopic.collectAsState()
    val searchQuery    by viewModel.searchQuery.collectAsState()
    val searchResults  by viewModel.searchResults.collectAsState()
    val isSearching    by viewModel.isSearching.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val followingState by viewModel.followingState.collectAsState()
    val haptic         = LocalHapticFeedback.current
    val listState      = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            kotlinx.coroutines.delay(100)
            focusRequester.requestFocus()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Main scrollable content ────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Space for sticky top bar
            item { Spacer(Modifier.height(72.dp)) }

            // Filter chips
            item {
                DiscoverFilterRow(
                    activeFilter    = activeFilter,
                    onFilterSelected = { viewModel.setFilter(it) },
                    modifier        = Modifier.padding(vertical = 8.dp)
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Content driven by uiState
            when (val state = uiState) {
                is DiscoverUiState.Loading -> {
                    item {
                        // Skeleton shimmer rows
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                DiscoverSkeletonCell(height = 200.dp)
                                DiscoverSkeletonCell(height = 140.dp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                DiscoverSkeletonCell(height = 140.dp)
                                DiscoverSkeletonCell(height = 200.dp)
                            }
                        }
                    }
                }

                is DiscoverUiState.Error -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("😕", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Couldn't load posts",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { viewModel.refresh() }) { Text("Try again") }
                        }
                    }
                }

                is DiscoverUiState.Success -> {
                    // Trending topics row
                    if (state.trendingTopics.isNotEmpty() &&
                        (activeFilter == DiscoverFilter.TRENDING || activeFilter == DiscoverFilter.NEW)
                    ) {
                        item {
                            TrendingTopicsRow(
                                topics        = state.trendingTopics,
                                selectedTopic = selectedTopic,
                                onTopicClick  = { viewModel.selectTopic(it) }
                            )
                            Spacer(Modifier.height(20.dp))
                        }
                    }

                    // Explore grid header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Explore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = when {
                                    selectedTopic != null -> "#${selectedTopic!!.hashtag}"
                                    activeFilter == DiscoverFilter.CLIPS -> "Clips"
                                    activeFilter == DiscoverFilter.PEOPLE -> "Explore"
                                    activeFilter == DiscoverFilter.NEW -> "New Posts"
                                    else -> "Explore"
                                },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    // Grid or empty state
                    if (state.explorePosts.isEmpty()) {
                        item {
                            DiscoverEmptyState(
                                onCreatePost = onCreatePost,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        item {
                            StaggeredExploreGrid(
                                posts         = state.explorePosts,
                                currentUserId = currentUserId,
                                onPostClick   = { onPostClick(it.id) },
                                onLikeClick   = { /* navigate to post detail */ },
                                modifier      = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // People you may know (inject mid-feed)
                    if (state.suggestedUsers.isNotEmpty() &&
                        activeFilter != DiscoverFilter.CLIPS
                    ) {
                        item {
                            Spacer(Modifier.height(24.dp))
                            PeopleSuggestionRow(
                                people         = state.suggestedUsers,
                                followingState = followingState,
                                onFollowClick  = { viewModel.toggleFollow(it) },
                                onUserClick    = onUserClick
                            )
                            Spacer(Modifier.height(24.dp))
                        }
                    }

                    // All caught up card
                    if (state.explorePosts.isNotEmpty()) {
                        item {
                            AllCaughtUpCard(
                                onRefresh = { viewModel.refresh() },
                                modifier  = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Sticky top bar (search + title) ───────────────────────────────────
        Surface(
            modifier        = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            color           = MaterialTheme.colorScheme.background.copy(alpha = 0.97f),
            shadowElevation = if (listState.firstVisibleItemIndex > 0) 8.dp else 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title
                AnimatedVisibility(visible = !isSearchActive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Discover",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("🧭", fontSize = 18.sp)
                        Spacer(Modifier.width(12.dp))
                    }
                }

                // Search bar
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(CircleShape)
                        .then(
                            if (!isSearchActive) Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.openSearch()
                            } else Modifier
                        ),
                    color  = MaterialTheme.colorScheme.surfaceVariant,
                    shape  = CircleShape
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint   = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        if (isSearchActive) {
                            BasicTextField(
                                value       = searchQuery,
                                onValueChange = { viewModel.onQueryChange(it) },
                                modifier    = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                singleLine  = true,
                                textStyle   = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Search people, posts, tags…",
                                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 14.sp
                                        )
                                    }
                                    inner()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick  = { viewModel.onQueryChange("") },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(16.dp),
                                        tint     = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Text(
                                "Search people, posts, tags…",
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Cancel button
                AnimatedVisibility(visible = isSearchActive) {
                    TextButton(
                        onClick = { viewModel.closeSearch() },
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            "Cancel",
                            color      = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 14.sp
                        )
                    }
                }
            }
        }

        // ── Full-screen search overlay ─────────────────────────────────────────
        AnimatedVisibility(
            visible = isSearchActive,
            enter   = fadeIn() + slideInVertically { it / 4 },
            exit    = fadeOut() + slideOutVertically { it / 4 }
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color    = MaterialTheme.colorScheme.background
            ) {
                SearchOverlay(
                    query          = searchQuery,
                    results        = searchResults,
                    isSearching    = isSearching,
                    recentSearches = recentSearches,
                    onUserClick    = { userId ->
                        viewModel.addRecentSearch("@${userId}")
                        viewModel.closeSearch()
                        onUserClick(userId)
                    },
                    onRecentClick  = { viewModel.onQueryChange(it.trimStart('@', '#')) },
                    onClearRecent  = { viewModel.clearRecentSearches() }
                )
            }
        }
    }
}

// ─── Search overlay content ───────────────────────────────────────────────────

@Composable
private fun SearchOverlay(
    query: String,
    results: List<User>,
    isSearching: Boolean,
    recentSearches: List<String>,
    onUserClick: (String) -> Unit,
    onRecentClick: (String) -> Unit,
    onClearRecent: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isSearching -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color    = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            }
            query.isBlank() -> {
                // Recent searches chips
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp, start = 16.dp, end = 16.dp)
                ) {
                    if (recentSearches.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Recent",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 15.sp,
                                color      = MaterialTheme.colorScheme.onBackground
                            )
                            TextButton(onClick = onClearRecent) {
                                Text(
                                    "Clear all",
                                    color    = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        recentSearches.forEach { term ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onRecentClick(term) }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    term,
                                    fontSize = 15.sp,
                                    color    = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔍", fontSize = 40.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Search for people to follow",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            results.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔍", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No results for \"$query\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 76.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(items = results, key = { it.id }) { user ->
                        SearchResultItem(user = user, onClick = { onUserClick(user.id) })
                        HorizontalDivider(
                            modifier  = Modifier.padding(start = 72.dp),
                            color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        UserAvatar(imageUrl = user.profileImageUrl, username = user.username, size = 48.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                user.username,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
            if (user.displayName.isNotEmpty() && user.displayName != user.username) {
                Text(
                    user.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (user.bio.isNotEmpty()) {
                Text(
                    user.bio,
                    style   = MaterialTheme.typography.bodySmall,
                    color   = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        Text(
            "${formatCount(user.followersCount)} followers",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
