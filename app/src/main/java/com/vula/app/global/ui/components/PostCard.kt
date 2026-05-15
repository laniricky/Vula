package com.vula.app.global.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vula.app.core.model.Post
import com.vula.app.core.ui.components.UserAvatar
import com.vula.app.core.ui.components.VulaIcons
import com.vula.app.core.util.TimeAgo
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// PostCard — Feed item (photo posts only; videos live in Ripples)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
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
    val isLiked       = post.likedBy.contains(currentUserId)
    var showHeart     by remember { mutableStateOf(false) }
    var showLikes     by remember { mutableStateOf(false) }
    var showComments  by remember { mutableStateOf(false) }
    var showReposts   by remember { mutableStateOf(false) }
    var commentText   by remember { mutableStateOf("") }
    var commentFocus  by remember { mutableStateOf(false) }
    val focusMgr      = LocalFocusManager.current
    val haptic        = LocalHapticFeedback.current

    LaunchedEffect(showHeart) { if (showHeart) { delay(900); showHeart = false } }

    Column(modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {

        // ── Author row ────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .clickable { onUserClick(post.authorId) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                imageUrl = post.authorProfileImageUrl,
                username = contactName ?: post.authorUsername,
                size     = 42.dp,
                modifier = Modifier.clearAndSetSemantics {}
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = contactName ?: "@${post.authorUsername}",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "· ${TimeAgo.format(post.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }
            IconButton(onClick = {}, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Default.MoreHoriz, null,
                    tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ── Image + stats pill ────────────────────────────────────────────────
        if (post.imageUrl != null || post.videoUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (!isLiked) onLikeClick(post.id)
                            showHeart = true
                        })
                    }
            ) {
                if (post.mediaType == "video" && post.videoUrl != null) {
                    VideoPlayer(post.videoUrl, Modifier.fillMaxSize())
                } else if (post.imageUrl != null) {
                    AsyncImage(
                        model              = post.imageUrl,
                        contentDescription = "Post image",
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop
                    )
                }

                // Double-tap heart burst
                HeartOverlay(visible = showHeart, modifier = Modifier.align(Alignment.Center))

                // Stats pill — bottom-right of image
                PostStatsPill(
                    likesCount    = post.likesCount,
                    commentsCount = post.commentsCount,
                    repostsCount  = 0,
                    onLikesClick    = { showLikes    = true },
                    onCommentsClick = { showComments = true },
                    onRepostsClick  = { showReposts  = true },
                    modifier        = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                )
            }
        }

        // ── Caption ───────────────────────────────────────────────────────────
        if (post.caption.isNotEmpty()) {
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)) {
                Text(
                    contactName ?: post.authorUsername,
                    fontWeight = FontWeight.Bold,
                    style      = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.width(5.dp))
                Text(post.caption, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // ── Inline action bar ─────────────────────────────────────────────────
        InlineActionBar(
            isLiked       = isLiked,
            commentText   = commentText,
            isFocused     = commentFocus,
            onCommentChange = { commentText = it },
            onFocusChange   = { commentFocus = it },
            onLike          = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (isLiked) onUnlikeClick(post.id) else onLikeClick(post.id)
            },
            onDm            = { onDmReplyToPost(post) },
            onRepost        = { /* TODO: repost API */ },
            onSubmitComment = {
                commentText = ""
                focusMgr.clearFocus()
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )

        Spacer(Modifier.height(4.dp))
        HorizontalDivider(
            color     = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            thickness = 0.5.dp
        )
    }

    // ── Bottom sheets ─────────────────────────────────────────────────────────
    if (showLikes)    LikesSheet(post    = post, onDismiss = { showLikes    = false })
    if (showComments) CommentsSheet(post = post, onDismiss = { showComments = false }, onSeeAll = { showComments = false; onCommentClick(post.id) })
    if (showReposts)  RepostsSheet(onDismiss = { showReposts = false })
}

// ── Stats pill overlaid on image (bottom-right) ───────────────────────────────

@Composable
private fun PostStatsPill(
    likesCount: Int,
    commentsCount: Int,
    repostsCount: Int,
    onLikesClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onRepostsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape    = RoundedCornerShape(24.dp),
        color    = Color.Black.copy(alpha = 0.60f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier              = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            PillStat(
                icon    = VulaIcons.VulaPulse,
                count   = likesCount,
                tint    = Color(0xFFFF6B9D),
                onClick = onLikesClick
            )
            // Thin divider
            Box(Modifier.width(0.5.dp).height(18.dp).background(Color.White.copy(alpha = 0.25f)))
            PillStat(
                icon    = VulaIcons.VulaWave,
                count   = commentsCount,
                tint    = Color(0xFF64B5F6),
                onClick = onCommentsClick
            )
            Box(Modifier.width(0.5.dp).height(18.dp).background(Color.White.copy(alpha = 0.25f)))
            PillStat(
                icon    = VulaIcons.VulaCycle,
                count   = repostsCount,
                tint    = Color(0xFF80CBC4),
                onClick = onRepostsClick
            )
        }
    }
}

@Composable
private fun PillStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier              = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = tint,
            modifier           = Modifier.size(19.dp)
        )
        if (count > 0) {
            Text(
                fmtCount(count),
                color      = Color.White,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Inline action bar (below image) ──────────────────────────────────────────

@Composable
private fun InlineActionBar(
    isLiked: Boolean,
    commentText: String,
    isFocused: Boolean,
    onCommentChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onLike: () -> Unit,
    onDm: () -> Unit,
    onRepost: () -> Unit,
    onSubmitComment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconAlpha by animateFloatAsState(
        targetValue   = if (isFocused) 0f else 1f,
        animationSpec = tween(180),
        label         = "icon_alpha"
    )
    val heartScale by animateFloatAsState(
        targetValue   = if (isLiked) 1.2f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label         = "heart_scale"
    )

    Row(
        modifier              = modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Comment text field — expands when focused
        OutlinedTextField(
            value       = commentText,
            onValueChange = onCommentChange,
            placeholder = {
                Text(
                    "Comment...",
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    fontSize = 14.sp
                )
            },
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { onFocusChange(it.isFocused) },
            shape       = RoundedCornerShape(24.dp),
            singleLine  = true,
            textStyle   = LocalTextStyle.current.copy(fontSize = 14.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSubmitComment() }),
            colors      = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor   = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                focusedBorderColor     = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        )

        // Action icons — hidden while comment field is focused
        AnimatedVisibility(
            visible = !isFocused,
            enter   = fadeIn(tween(180)) + slideInHorizontally(tween(180)) { it / 2 },
            exit    = fadeOut(tween(140)) + slideOutHorizontally(tween(140)) { it / 2 }
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                // Like
                IconButton(onClick = onLike, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector        = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint               = if (isLiked) Color(0xFFE91E63)
                                             else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier           = Modifier
                            .size(22.dp)
                            .graphicsLayer { scaleX = heartScale; scaleY = heartScale }
                    )
                }
                // DM
                IconButton(onClick = onDm, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send, "DM",
                        tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Repost
                IconButton(onClick = onRepost, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.Repeat, "Repost",
                        tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ── Bottom Sheets ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LikesSheet(post: Post, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            Text("Liked by", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(12.dp))
            if (post.likesCount == 0) {
                Text("No likes yet — be the first!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            } else {
                // Reaction emoji groups
                val grouped = post.reactions.entries.groupBy { it.value }
                if (grouped.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        grouped.forEach { (emoji, users) ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    modifier              = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text(emoji, fontSize = 16.sp)
                                    Text("${users.size}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Text(
                    "${post.likesCount} ${if (post.likesCount == 1) "person" else "people"} liked this",
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentsSheet(post: Post, onDismiss: () -> Unit, onSeeAll: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Comments", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                TextButton(onClick = onSeeAll) { Text("See all") }
            }
            Spacer(Modifier.height(8.dp))
            if (post.commentsCount == 0) {
                Text(
                    "No comments yet. Start the conversation!",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else {
                post.topComment?.let { c ->
                    Row(
                        modifier              = Modifier.padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(c.username, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(c.text, fontSize = 14.sp)
                    }
                }
                if (post.commentsCount > 1) {
                    TextButton(onClick = onSeeAll) {
                        Text("View all ${post.commentsCount} comments →")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepostsSheet(onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            Text("Reposted by", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(12.dp))
            Text("No reposts yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

// ── Double-tap heart burst ────────────────────────────────────────────────────

@Composable
private fun HeartOverlay(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible  = visible,
        enter    = scaleIn(spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)) + fadeIn(),
        exit     = scaleOut(tween(200)) + fadeOut(tween(300)),
        modifier = modifier
    ) {
        Text("❤️", fontSize = 92.sp, modifier = Modifier.graphicsLayer { shadowElevation = 24f })
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun fmtCount(n: Int): String = when {
    n >= 1_000_000 -> "${n / 1_000_000}M"
    n >= 1_000     -> "${n / 1_000}K"
    n == 0         -> ""
    else           -> n.toString()
}
