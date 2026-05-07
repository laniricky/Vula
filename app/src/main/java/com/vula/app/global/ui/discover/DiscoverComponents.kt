package com.vula.app.global.ui.discover

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vula.app.core.model.Post
import com.vula.app.core.ui.components.shimmerBrush

// ─── Filter Chip Row ──────────────────────────────────────────────────────────

@Composable
fun DiscoverFilterRow(
    activeFilter: DiscoverFilter,
    onFilterSelected: (DiscoverFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(DiscoverFilter.entries) { filter ->
            val isSelected = filter == activeFilter
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                label = "chip_bg_${filter.name}"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "chip_text_${filter.name}"
            )
            Surface(
                shape = CircleShape,
                color = bgColor,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFilterSelected(filter)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector        = filter.icon,
                        contentDescription = null,
                        modifier           = Modifier.size(15.dp),
                        tint               = textColor
                    )
                    Text(
                        filter.label,
                        fontSize   = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color      = textColor
                    )
                }
            }
        }
    }
}

// ─── Trending Topics Row ──────────────────────────────────────────────────────

@Composable
fun TrendingTopicsRow(
    topics: List<TrendingTopic>,
    selectedTopic: TrendingTopic?,
    onTopicClick: (TrendingTopic) -> Unit,
    modifier: Modifier = Modifier
) {
    if (topics.isEmpty()) return
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = Icons.Default.Whatshot,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Trending Now",
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 17.sp,
                color      = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(topics) { topic ->
                TrendingTopicCard(
                    topic = topic,
                    isSelected = selectedTopic?.hashtag == topic.hashtag,
                    onClick = { onTopicClick(topic) }
                )
            }
        }
    }
}

@Composable
fun TrendingTopicCard(
    topic: TrendingTopic,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "topic_border"
    )
    Box(
        modifier = Modifier
            .width(110.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
    ) {
        if (topic.coverImageUrl != null) {
            AsyncImage(
                model = topic.coverImageUrl,
                contentDescription = "#${topic.hashtag}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
            )
        }
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                "#${topic.hashtag}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${formatCount(topic.postCount)} posts",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 10.sp
            )
        }
    }
}

// ─── Skeleton shimmer grid cells ─────────────────────────────────────────────
// Heights mirror the real StaggeredExploreGrid pattern: tall=260dp, short=180dp.

@Composable
fun DiscoverSkeletonCell(height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(shimmerBrush())
    )
}

/** Convenience: a row pair whose heights match the real staggered grid. */
@Composable
fun DiscoverSkeletonRow(tallOnLeft: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DiscoverSkeletonCell(height = if (tallOnLeft) 260.dp else 180.dp)
        DiscoverSkeletonCell(height = if (tallOnLeft) 180.dp else 260.dp)
    }
}

// ─── Staggered Explore Grid (2-col, manual layout) ────────────────────────────

@Composable
fun StaggeredExploreGrid(
    posts: List<Post>,
    currentUserId: String,
    onPostClick: (Post) -> Unit,
    onLikeClick: (Post) -> Unit,
    modifier: Modifier = Modifier
) {
    // Split into two columns
    val leftPosts = posts.filterIndexed { i, _ -> i % 2 == 0 }
    val rightPosts = posts.filterIndexed { i, _ -> i % 2 != 0 }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            leftPosts.forEachIndexed { index, post ->
                val height = if (index % 3 == 0) 200.dp else 140.dp
                ExploreGridCell(
                    post = post,
                    cellHeight = height,
                    currentUserId = currentUserId,
                    onPostClick = onPostClick,
                    onLikeClick = onLikeClick
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rightPosts.forEachIndexed { index, post ->
                val height = if (index % 3 == 1) 200.dp else 140.dp
                ExploreGridCell(
                    post = post,
                    cellHeight = height,
                    currentUserId = currentUserId,
                    onPostClick = onPostClick,
                    onLikeClick = onLikeClick
                )
            }
        }
    }
}

@Composable
fun ExploreGridCell(
    post: Post,
    cellHeight: Dp,
    currentUserId: String,
    onPostClick: (Post) -> Unit,
    onLikeClick: (Post) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    var showOverlay by remember { mutableStateOf(false) }
    var liked by remember { mutableStateOf(post.likedBy.contains(currentUserId)) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "cell_scale"
    )
    // Pulsing heart on like
    var heartVisible by remember { mutableStateOf(false) }
    val heartScale by animateFloatAsState(
        targetValue = if (heartVisible) 1f else 0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label = "heart_scale",
        finishedListener = { if (heartVisible) heartVisible = false }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cellHeight)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onPostClick(post) },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showOverlay = !showOverlay
                    },
                    onDoubleTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!liked) {
                            liked = true
                            onLikeClick(post)
                            heartVisible = true
                        }
                    }
                )
            }
    ) {
        // Image
        if (post.imageUrl != null) {
            AsyncImage(
                model = post.imageUrl,
                contentDescription = post.caption.take(40),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    post.caption.take(60),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(8.dp),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Peek overlay on long-press
        if (showOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AsyncImage(
                    model = post.authorProfileImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Text(
                    "@${post.authorUsername}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(formatCount(post.likesCount), color = Color.White, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ModeComment,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(formatCount(post.commentsCount), color = Color.White, fontSize = 10.sp)
                }
            }
        }

        // Video badge
        if (post.mediaType == "video") {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("▶", color = Color.White, fontSize = 9.sp)
            }
        }

        // Double-tap heart burst
        if (heartVisible || heartScale > 0f) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .scale(heartScale)
                    .size(48.dp)
            )
        }
    }
}

// ─── People You May Know row ──────────────────────────────────────────────────

@Composable
fun PeopleSuggestionRow(
    people: List<SuggestedUser>,
    followingState: Map<String, Boolean>,
    onFollowClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (people.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = Icons.Default.Group,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "People You May Know",
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 17.sp,
                color      = MaterialTheme.colorScheme.onBackground,
                modifier   = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(people, key = { it.user.id }) { suggested ->
                SuggestedPersonCard(
                    suggested = suggested,
                    isFollowing = followingState[suggested.user.id] == true,
                    onFollowClick = { onFollowClick(suggested.user.id) },
                    onUserClick = { onUserClick(suggested.user.id) }
                )
            }
        }
    }
}

@Composable
fun SuggestedPersonCard(
    suggested: SuggestedUser,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onUserClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onUserClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Glow-ring avatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(2.dp)
                    .clip(CircleShape)
            ) {
                if (!suggested.user.profileImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = suggested.user.profileImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            suggested.user.username.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "@${suggested.user.username}",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            // Follower count gives context without faking mutual data
            if (suggested.user.followersCount > 0) {
                Text(
                    "${formatCount(suggested.user.followersCount)} followers",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            val btnColor by animateColorAsState(
                targetValue = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.primary,
                label = "follow_btn_color"
            )
            val btnTextColor by animateColorAsState(
                targetValue = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onPrimary,
                label = "follow_btn_text_color"
            )
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFollowClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = btnColor,
                    contentColor = btnTextColor
                ),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
                if (isFollowing) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("Following", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("Follow", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─── People full list (used by PEOPLE filter tab) ───────────────────────────

@Composable
fun PeopleFullList(
    people: List<SuggestedUser>,
    followingState: Map<String, Boolean>,
    onFollowClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (people.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("👥", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "No suggestions yet",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
        return
    }
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        people.forEach { suggested ->
            val isFollowing = followingState[suggested.user.id] == true
            PeopleListItem(
                suggested    = suggested,
                isFollowing  = isFollowing,
                onFollowClick = { onFollowClick(suggested.user.id) },
                onUserClick   = { onUserClick(suggested.user.id) }
            )
            HorizontalDivider(
                modifier  = Modifier.padding(start = 72.dp),
                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
private fun PeopleListItem(
    suggested: SuggestedUser,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onUserClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(2.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            if (!suggested.user.profileImageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = suggested.user.profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    suggested.user.username.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        // Name + bio
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "@${suggested.user.username}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (suggested.user.bio.isNotBlank()) {
                Text(
                    suggested.user.bio,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (suggested.user.followersCount > 0) {
                Text(
                    "${formatCount(suggested.user.followersCount)} followers",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Follow / Following button
        val btnColor by animateColorAsState(
            if (isFollowing) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.primary,
            label = "people_btn_color"
        )
        val btnTextColor by animateColorAsState(
            if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onPrimary,
            label = "people_btn_text_color"
        )
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onFollowClick()
            },
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = btnColor,
                contentColor   = btnTextColor
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
        ) {
            if (isFollowing) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Following", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Follow", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
fun DiscoverEmptyState(onCreatePost: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🧭", fontSize = 52.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Nothing here yet",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Be the first to post something here",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onCreatePost,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("+ Create Post", fontWeight = FontWeight.Bold)
        }
    }
}

// ─── All caught up card ───────────────────────────────────────────────────────

@Composable
fun AllCaughtUpCard(onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎉", fontSize = 40.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "You're all caught up!",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Come back later for more",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        TextButton(onClick = onRefresh) {
            Text("Refresh", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Utility ──────────────────────────────────────────────────────────────────

internal fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M"
    count >= 1_000     -> "${count / 1_000}K"
    else               -> count.toString()
}
