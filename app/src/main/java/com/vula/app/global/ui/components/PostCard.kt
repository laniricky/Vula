package com.vula.app.global.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import coil.compose.AsyncImage
import com.vula.app.core.model.Post
import com.vula.app.core.ui.components.UserAvatar
import com.vula.app.core.util.TimeAgo
import kotlinx.coroutines.delay

@Composable
fun PostCard(
    post: Post,
    currentUserId: String,
    contactName: String? = null,
    onLikeClick: (String) -> Unit,
    onUnlikeClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onUserClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isLiked = post.likedBy.contains(currentUserId)
    var showHeartOverlay by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(showHeartOverlay) {
        if (showHeartOverlay) {
            delay(800)
            showHeartOverlay = false
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // ── Header ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUserClick(post.authorId) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    imageUrl = post.authorProfileImageUrl,
                    username = contactName ?: post.authorUsername,
                    size = 40.dp,
                    modifier = Modifier.clearAndSetSemantics {}
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contactName ?: post.authorUsername,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = TimeAgo.format(post.createdAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Post image with double-tap like ─────────────────────────────
            if (post.imageUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (!isLiked) onLikeClick(post.id)
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
                    // Animated heart overlay on double-tap
                    if (showHeartOverlay) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showHeartOverlay,
                            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(88.dp)
                            )
                        }
                    }
                }
            }

            // ── Action row ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .clearAndSetSemantics {},
                verticalAlignment = Alignment.CenterVertically
            ) {
                val likeScale by animateFloatAsState(
                    targetValue = if (isLiked) 1.2f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "like_scale"
                )
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isLiked) onUnlikeClick(post.id) else onLikeClick(post.id)
                }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint = if (isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.graphicsLayer {
                            scaleX = likeScale
                            scaleY = likeScale
                        }
                    )
                }
                IconButton(onClick = { onCommentClick(post.id) }) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comment",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ── Like count ──────────────────────────────────────────────────
            if (post.likesCount > 0) {
                Text(
                    text = "${post.likesCount} ${if (post.likesCount == 1) "like" else "likes"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            // ── Caption ─────────────────────────────────────────────────────
            if (post.caption.isNotEmpty()) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(
                        text = contactName ?: post.authorUsername,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = post.caption, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // ── Inline Comment Preview ──────────────────────────────────────
            post.topComment?.let { comment ->
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) {
                    Text(
                        text = comment.username,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = comment.text, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // ── Comments link ────────────────────────────────────────────────
            if (post.commentsCount > 0) {
                Text(
                    text = "View all ${post.commentsCount} comments",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clickable { onCommentClick(post.id) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            } else {
                Text(
                    text = "Add a comment…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier
                        .clickable { onCommentClick(post.id) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
