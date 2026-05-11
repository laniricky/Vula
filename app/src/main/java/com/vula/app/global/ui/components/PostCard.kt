package com.vula.app.global.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vula.app.core.model.Post
import com.vula.app.core.ui.components.UserAvatar
import com.vula.app.core.util.TimeAgo
import kotlinx.coroutines.delay

val REACTION_EMOJIS = listOf("❤️", "😂", "😮", "😢", "🔥")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCard(
    post: Post,
    currentUserId: String,
    contactName: String? = null,
    isAuthorOnline: Boolean = false,
    authorRichStatus: String? = null,
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

    LaunchedEffect(showHeartOverlay) { if (showHeartOverlay) { delay(800); showHeartOverlay = false } }
    LaunchedEffect(showEmojiPicker) { if (showEmojiPicker) { delay(3000); showEmojiPicker = false } }

    // Online glow pulse
    val infiniteTransition = rememberInfiniteTransition(label = "online_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_alpha"
    )

    Column(modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {

        // ── Author row ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUserClick(post.authorId) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with optional online glow ring
            Box(modifier = Modifier.size(42.dp)) {
                val avatarMod = Modifier.clearAndSetSemantics {}
                if (isAuthorOnline) {
                    Box(
                        modifier = Modifier.fillMaxSize().clip(CircleShape).border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF4CAF50).copy(alpha = glowAlpha), Color(0xFF8BC34A).copy(alpha = glowAlpha))
                            ),
                            shape = CircleShape
                        )
                    )
                    Box(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                        UserAvatar(post.authorProfileImageUrl, contactName ?: post.authorUsername, 36.dp, avatarMod)
                    }
                } else {
                    UserAvatar(post.authorProfileImageUrl, contactName ?: post.authorUsername, 42.dp, avatarMod)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contactName ?: "@${post.authorUsername}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "· ${TimeAgo.format(post.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
                if (!authorRichStatus.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier.padding(top = 3.dp)
                    ) {
                        Text(
                            authorRichStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            IconButton(onClick = {}, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.MoreHoriz, null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(18.dp))
            }
        }

        // ── Full-width media + floating action column ─────────────────────────
        if (post.imageUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (currentUserEmoji == null) onReactToPost(post.id, "❤️")
                            showHeartOverlay = true
                        })
                    }
            ) {
                if (post.mediaType == "video" && post.videoUrl != null) {
                    VideoPlayer(post.videoUrl, Modifier.fillMaxSize())
                    // Video badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "▶ VIDEO",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (post.imageUrl != null) {
                    AsyncImage(post.imageUrl, "Post image", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }

                // Heart burst on double-tap — extracted to avoid ColumnScope AnimatedVisibility ambiguity
                HeartOverlay(
                    visible  = showHeartOverlay,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Floating vertical action buttons (right edge)
                val likeScale by animateFloatAsState(
                    if (currentUserEmoji != null || isLiked) 1.22f else 1f,
                    spring(Spring.DampingRatioMediumBouncy), label = "like_scale"
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Like
                    FloatingActionIcon(
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (currentUserEmoji != null) onRemoveReaction(post.id)
                                else onReactToPost(post.id, "❤️")
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showEmojiPicker = true
                            }
                        ).graphicsLayer { scaleX = likeScale; scaleY = likeScale }
                    ) {
                        if (currentUserEmoji != null) Text(currentUserEmoji, fontSize = 20.sp)
                        else Icon(
                            if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            null, tint = if (isLiked) Color(0xFFE91E63) else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    if (post.likesCount > 0) {
                        Text(fmtCount(post.likesCount), fontSize = 11.sp, color = Color.White,
                            fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-10).dp))
                    }

                    // Comment
                    FloatingActionIcon(modifier = Modifier.clickable { onCommentClick(post.id) }) {
                        Icon(Icons.Default.ChatBubbleOutline, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    if (post.commentsCount > 0) {
                        Text(fmtCount(post.commentsCount), fontSize = 11.sp, color = Color.White,
                            fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-10).dp))
                    }

                    // DM
                    FloatingActionIcon(modifier = Modifier.clickable { onDmReplyToPost(post) }) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                // Emoji picker — extracted to avoid ColumnScope AnimatedVisibility ambiguity
                EmojiPickerOverlay(
                    visible          = showEmojiPicker,
                    currentUserEmoji = currentUserEmoji,
                    onEmojiSelected  = { emoji ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showEmojiPicker = false
                        if (currentUserEmoji == emoji) onRemoveReaction(post.id)
                        else onReactToPost(post.id, emoji)
                    },
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 12.dp)
                )
            }
        }

        // ── Caption ───────────────────────────────────────────────────────────
        if (post.caption.isNotEmpty()) {
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                Text(contactName ?: post.authorUsername, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(6.dp))
                Text(post.caption, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // ── Top comment preview ───────────────────────────────────────────────
        post.topComment?.let { c ->
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)) {
                Text(c.username, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                Spacer(Modifier.width(5.dp))
                Text(c.text, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (post.commentsCount > 0 && post.topComment == null) {
            Text("View all ${post.commentsCount} comments",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier.clickable { onCommentClick(post.id) }
                    .padding(horizontal = 14.dp, vertical = 4.dp))
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
    }
}

@Composable
private fun FloatingActionIcon(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier         = modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.38f)),
        contentAlignment = Alignment.Center,
        content          = content
    )
}

/** Standalone so AnimatedVisibility uses the global overload, not ColumnScope's. */
@Composable
private fun HeartOverlay(visible: Boolean, modifier: Modifier = Modifier) {
    androidx.compose.animation.AnimatedVisibility(
        visible  = visible,
        enter    = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
        exit     = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        Text("❤️", fontSize = 88.sp)
    }
}

/** Standalone so AnimatedVisibility uses the global overload, not ColumnScope's. */
@Composable
private fun EmojiPickerOverlay(
    visible: Boolean,
    currentUserEmoji: String?,
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.animation.AnimatedVisibility(
        visible  = visible,
        enter    = slideInVertically { it } + fadeIn(),
        exit     = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape          = RoundedCornerShape(32.dp),
            shadowElevation = 8.dp,
            color          = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
        ) {
            Row(
                modifier              = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                REACTION_EMOJIS.forEach { emoji ->
                    val sel = currentUserEmoji == emoji
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { onEmojiSelected(emoji) }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(emoji, fontSize = 26.sp) }
                }
            }
        }
    }
}

private fun fmtCount(n: Int): String = when {
    n >= 1_000_000 -> "${n / 1_000_000}M"
    n >= 1_000     -> "${n / 1_000}K"
    n == 0         -> ""
    else           -> n.toString()
}
