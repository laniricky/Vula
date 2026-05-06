package com.vula.app.global.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vula.app.core.model.Post
import com.vula.app.core.ui.components.UserAvatar
import com.vula.app.core.util.TimeAgo
import kotlinx.coroutines.delay

// The 5 canonical reaction emojis
val REACTION_EMOJIS = listOf("❤️", "😂", "😮", "😢", "🔥")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCard(
    post: Post,
    currentUserId: String,
    contactName: String? = null,
    onLikeClick: (String) -> Unit,
    onUnlikeClick: (String) -> Unit,
    onReactToPost: (postId: String, emoji: String) -> Unit = { _, _ -> },
    onRemoveReaction: (postId: String) -> Unit = {},
    onCommentClick: (String) -> Unit,
    onDmReplyToPost: (Post) -> Unit = {},
    onUserClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUserEmoji = post.reactions[currentUserId]
    val isLiked = post.likedBy.contains(currentUserId)
    var showHeartOverlay by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(showHeartOverlay) {
        if (showHeartOverlay) {
            delay(800)
            showHeartOverlay = false
        }
    }

    LaunchedEffect(showEmojiPicker) {
        if (showEmojiPicker) {
            delay(3000)
            showEmojiPicker = false
        }
    }

    val reactionSummary = remember(post.reactions) {
        post.reactions.values
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
    }

    // Approximate views: likes × 2 + comments × 5 as a display heuristic
    val displayViews = post.likesCount * 2 + post.commentsCount * 5

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 0.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUserClick(post.authorId) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                imageUrl = post.authorProfileImageUrl,
                username = contactName ?: post.authorUsername,
                size = 38.dp,
                modifier = Modifier.clearAndSetSemantics {}
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contactName ?: post.authorUsername,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = TimeAgo.format(post.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            IconButton(
                onClick = {},
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        // ── Post Image (landscape 4:3) ─────────────────────────────────────────
        if (post.imageUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (currentUserEmoji == null) {
                                    onReactToPost(post.id, "❤️")
                                }
                                showHeartOverlay = true
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (post.mediaType == "video") {
                    VideoPlayer(
                        videoUrl = post.imageUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = "Post image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                // Double-tap heart overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = showHeartOverlay,
                    enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Text("❤️", fontSize = 88.sp)
                }
            }
        }

        // ── Action Row ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clearAndSetSemantics {},
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Like / Emoji button with long-press picker
            val likeScale by animateFloatAsState(
                targetValue = if (currentUserEmoji != null || isLiked) 1.2f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "like_scale"
            )
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .combinedClickable(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (currentUserEmoji != null) {
                                    onRemoveReaction(post.id)
                                } else {
                                    onReactToPost(post.id, "❤️")
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showEmojiPicker = true
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentUserEmoji != null) {
                        Text(
                            text = currentUserEmoji,
                            fontSize = 20.sp,
                            modifier = Modifier.graphicsLayer { scaleX = likeScale; scaleY = likeScale }
                        )
                    } else {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isLiked) "Unlike" else "Like",
                            tint = if (isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer { scaleX = likeScale; scaleY = likeScale }
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${post.likesCount}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }

                // Emoji picker popup
                androidx.compose.animation.AnimatedVisibility(
                    visible = showEmojiPicker,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.offset(y = (-56).dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            REACTION_EMOJIS.forEach { emoji ->
                                val isSelected = currentUserEmoji == emoji
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showEmojiPicker = false
                                            if (isSelected) onRemoveReaction(post.id)
                                            else onReactToPost(post.id, emoji)
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 26.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Comment button + count
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onCommentClick(post.id) }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "Comments",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${post.commentsCount}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Views count (right-aligned)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Views",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$displayViews",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        // ── Caption ──────────────────────────────────────────────────────────
        if (post.caption.isNotEmpty()) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                Text(
                    text = contactName ?: post.authorUsername,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = post.caption, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // ── Comments link ────────────────────────────────────────────────────
        if (post.commentsCount > 0) {
            Text(
                text = "View all ${post.commentsCount} comments",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier
                    .clickable { onCommentClick(post.id) }
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            thickness = 0.5.dp
        )
    }
}
