package com.vula.app.global.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vula.app.core.model.Post
import com.vula.app.core.ui.components.UserAvatar
import com.vula.app.core.util.TimeAgo
import kotlinx.coroutines.delay

val REACTION_EMOJIS = listOf("❤️", "😂", "😮", "😢", "🔥")

// ─── Brand gradients ──────────────────────────────────────────────────────────
private val HeartGradient    = Brush.linearGradient(listOf(Color(0xFFFF6B9D), Color(0xFFE91E63)))
private val CommentGradient  = Brush.linearGradient(listOf(Color(0xFF64B5F6), Color(0xFF1976D2)))
private val SendGradient     = Brush.linearGradient(listOf(Color(0xFF4DD0E1), Color(0xFF0097A7)))
private val GlassWhite       = Color.White.copy(alpha = 0.10f)
private val GlassBorder      = Color.White.copy(alpha = 0.18f)

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
    val isLiked          = post.likedBy.contains(currentUserId)
    var showHeartOverlay  by remember { mutableStateOf(false) }
    var showEmojiPicker   by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(showHeartOverlay) { if (showHeartOverlay) { delay(900); showHeartOverlay = false } }
    LaunchedEffect(showEmojiPicker)  { if (showEmojiPicker)  { delay(3500); showEmojiPicker  = false } }

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
            Box(modifier = Modifier.size(42.dp)) {
                val avatarMod = Modifier.clearAndSetSemantics {}
                if (isAuthorOnline) {
                    Box(
                        modifier = Modifier.fillMaxSize().clip(CircleShape).border(
                            2.dp,
                            Brush.linearGradient(listOf(
                                Color(0xFF4CAF50).copy(alpha = glowAlpha),
                                Color(0xFF8BC34A).copy(alpha = glowAlpha)
                            )),
                            CircleShape
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
                        text  = contactName ?: "@${post.authorUsername}",
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
                    tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(18.dp))
            }
        }

        // ── Media ─────────────────────────────────────────────────────────────
        if (post.imageUrl != null || post.videoUrl != null) {
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
                // Media content
                if (post.mediaType == "video" && post.videoUrl != null) {
                    VideoPlayer(post.videoUrl, Modifier.fillMaxSize())
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.50f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text("▶ VIDEO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (post.imageUrl != null) {
                    AsyncImage(post.imageUrl, "Post image", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }

                // Double-tap heart burst
                HeartOverlay(visible = showHeartOverlay, modifier = Modifier.align(Alignment.Center))

                // Staggered emoji picker (bottom-left)
                StaggeredEmojiPicker(
                    visible          = showEmojiPicker,
                    currentUserEmoji = currentUserEmoji,
                    onEmojiSelected  = { emoji ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showEmojiPicker = false
                        if (currentUserEmoji == emoji) onRemoveReaction(post.id)
                        else onReactToPost(post.id, emoji)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 10.dp, bottom = 70.dp)
                )

                // ── Glassmorphism action bar ──────────────────────────────────
                GlassActionBar(
                    modifier       = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(8.dp),
                    currentUserEmoji  = currentUserEmoji,
                    isLiked           = isLiked,
                    likesCount        = post.likesCount,
                    commentsCount     = post.commentsCount,
                    onHeartClick      = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (currentUserEmoji != null) onRemoveReaction(post.id)
                        else onReactToPost(post.id, "❤️")
                    },
                    onHeartLongClick  = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showEmojiPicker = true
                    },
                    onCommentClick    = { onCommentClick(post.id) },
                    onShareClick      = { onDmReplyToPost(post) }
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
                style  = MaterialTheme.typography.bodySmall,
                color  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier
                    .clickable { onCommentClick(post.id) }
                    .padding(horizontal = 14.dp, vertical = 4.dp))
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
    }
}

// ─── Glassmorphism action bar ─────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GlassActionBar(
    modifier: Modifier,
    currentUserEmoji: String?,
    isLiked: Boolean,
    likesCount: Int,
    commentsCount: Int,
    onHeartClick: () -> Unit,
    onHeartLongClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.42f))
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // ── Heart / Reaction
            SpringIconButton(
                modifier = Modifier.combinedClickable(
                    onClick    = onHeartClick,
                    onLongClick = onHeartLongClick
                ),
                isActive   = currentUserEmoji != null || isLiked,
                activeGradient = HeartGradient,
                count      = likesCount
            ) {
                if (currentUserEmoji != null) {
                    Text(currentUserEmoji, fontSize = 22.sp)
                } else {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint   = if (isLiked) Color(0xFFFF4081) else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Thin separator
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(28.dp)
                    .background(Color.White.copy(alpha = 0.20f))
            )

            // ── Comment
            SpringIconButton(
                modifier = Modifier.clickable(onClick = onCommentClick),
                isActive = commentsCount > 0,
                activeGradient = CommentGradient,
                count    = commentsCount
            ) {
                Icon(
                    Icons.Default.ChatBubbleOutline, null,
                    tint     = Color.White,
                    modifier = Modifier.size(21.dp)
                )
            }

            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(28.dp)
                    .background(Color.White.copy(alpha = 0.20f))
            )

            // ── DM / Share
            SpringIconButton(
                modifier = Modifier.clickable(onClick = onShareClick),
                isActive = false,
                activeGradient = SendGradient,
                count    = 0
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send, null,
                    tint     = Color.White,
                    modifier = Modifier.size(20.dp).rotate(-20f)
                )
            }
        }
    }
}

// ─── Spring-physics icon button with count ────────────────────────────────────
@Composable
private fun SpringIconButton(
    modifier: Modifier = Modifier,
    isActive: Boolean,
    activeGradient: Brush,
    count: Int,
    content: @Composable BoxScope.() -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = when {
            pressed  -> 0.72f
            isActive -> 1.18f
            else     -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh
        ),
        label = "icon_spring"
    )

    // Glow shadow when active
    val glowRadius by animateFloatAsState(
        targetValue  = if (isActive) 14f else 0f,
        animationSpec = tween(300),
        label        = "glow_radius"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .then(
                    if (isActive) Modifier.softGlow(glowRadius)
                    else Modifier
                )
                .clip(CircleShape)
                .then(
                    if (isActive) Modifier.background(activeGradient)
                    else Modifier.background(Color.White.copy(alpha = 0.12f))
                ),
            contentAlignment = Alignment.Center,
            content          = content
        )
        if (count > 0) {
            Spacer(Modifier.width(5.dp))
            Text(
                text  = fmtCount(count),
                color = Color.White,
                fontSize     = 12.sp,
                fontWeight   = FontWeight.SemiBold
            )
        }
    }
}

// Soft glow extension
private fun Modifier.softGlow(radius: Float, color: Color = Color(0xFFFF4081)): Modifier =
    if (radius <= 0f) this
    else this.drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                asFrameworkPaint().apply {
                    isAntiAlias = true
                    this.color  = android.graphics.Color.TRANSPARENT
                    setShadowLayer(radius, 0f, 0f, color.copy(alpha = 0.55f).toArgb())
                }
            }
            canvas.drawCircle(
                center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f),
                radius = size.minDimension / 2f,
                paint  = paint
            )
        }
    }

// ─── Staggered emoji picker (replaces the old EmojiPickerOverlay) ─────────────
@Composable
private fun StaggeredEmojiPicker(
    visible: Boolean,
    currentUserEmoji: String?,
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible  = visible,
        enter    = fadeIn(tween(150)) + slideInVertically(tween(200)) { it / 2 },
        exit     = fadeOut(tween(120)) + slideOutVertically(tween(160)) { it / 2 },
        modifier = modifier
    ) {
        Surface(
            shape           = RoundedCornerShape(36.dp),
            shadowElevation = 12.dp,
            color           = Color.Black.copy(alpha = 0.72f)
        ) {
            Row(
                modifier              = Modifier
                    .border(1.dp, GlassBorder, RoundedCornerShape(36.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                REACTION_EMOJIS.forEachIndexed { index, emoji ->
                    val sel     = currentUserEmoji == emoji
                    var entered by remember { mutableStateOf(false) }

                    LaunchedEffect(visible) {
                        if (visible) {
                            delay(index * 40L)
                            entered = true
                        } else {
                            entered = false
                        }
                    }

                    val emojiScale by animateFloatAsState(
                        targetValue  = if (entered) (if (sel) 1.30f else 1f) else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness    = Spring.StiffnessMedium
                        ),
                        label = "emoji_${index}_scale"
                    )

                    Box(
                        modifier = Modifier
                            .graphicsLayer { scaleX = emojiScale; scaleY = emojiScale }
                            .clip(CircleShape)
                            .background(
                                if (sel) Color.White.copy(alpha = 0.20f) else Color.Transparent
                            )
                            .clickable { onEmojiSelected(emoji) }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 28.sp)
                    }
                }
            }
        }
    }
}

// ─── Double-tap heart burst ───────────────────────────────────────────────────
@Composable
private fun HeartOverlay(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible  = visible,
        enter    = scaleIn(spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)) + fadeIn(),
        exit     = scaleOut(tween(200)) + fadeOut(tween(300)),
        modifier = modifier
    ) {
        Text(
            "❤️",
            fontSize = 92.sp,
            modifier = Modifier
                .graphicsLayer { shadowElevation = 24f }
        )
    }
}

private fun fmtCount(n: Int): String = when {
    n >= 1_000_000 -> "${n / 1_000_000}M"
    n >= 1_000     -> "${n / 1_000}K"
    n == 0         -> ""
    else           -> n.toString()
}
