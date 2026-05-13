package com.vula.app.global.ui.ripples

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.vula.app.core.model.Post
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Brand colours ────────────────────────────────────────────────────────────
private val GlobalBg     = Color(0xFF0D1B2A)
private val GlobalAccent = Color(0xFF00E5FF)
private val LocalBg      = Color(0xFF1A1108)
private val LocalAccent  = Color(0xFFFFB300)

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun RipplesScreen(
    currentUserId: String,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToComments: (String) -> Unit,
    onNavigateToCreatePost: () -> Unit,
    viewModel: RipplesViewModel = hiltViewModel()
) {
    val uiState      by viewModel.uiState.collectAsState()
    val mode         by viewModel.mode.collectAsState()
    val likedPosts   by viewModel.likedPosts.collectAsState()
    val isGlobal      = mode == RipplesMode.GLOBAL
    val bgColor      by animateColorAsState(if (isGlobal) GlobalBg else LocalBg,     label = "bg")
    val accent       by animateColorAsState(if (isGlobal) GlobalAccent else LocalAccent, label = "accent")

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {

        when (val s = uiState) {
            is RipplesUiState.Loading -> CircularProgressIndicator(
                color    = accent,
                modifier = Modifier.align(Alignment.Center)
            )

            is RipplesUiState.Error -> RipplesEmptyState(
                message      = s.message,
                accent       = accent,
                onCreatePost = onNavigateToCreatePost,
                modifier     = Modifier.align(Alignment.Center)
            )

            is RipplesUiState.Success -> {
                val pagerState = rememberPagerState(pageCount = { s.ripples.size })
                LaunchedEffect(pagerState.currentPage) {
                    viewModel.setCurrentIndex(pagerState.currentPage)
                }
                VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val ripple = s.ripples[page]
                    RippleItem(
                        post            = ripple,
                        isVisible       = page == pagerState.currentPage,
                        isLiked         = likedPosts.contains(ripple.id),
                        accent          = accent,
                        onLike          = { viewModel.toggleLike(ripple) },
                        onProfileClick  = { onNavigateToProfile(ripple.authorId) },
                        onCommentsClick = { onNavigateToComments(ripple.id) }
                    )
                }
            }
        }

        // Mode pill always on top
        RipplesModePill(
            isGlobal     = isGlobal,
            accent       = accent,
            onModeChange = { viewModel.setMode(it) },
            modifier     = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
        )
    }
}

// ── Single Ripple item ───────────────────────────────────────────────────────

@Composable
private fun RippleItem(
    post: Post,
    isVisible: Boolean,
    isLiked: Boolean,
    accent: Color,
    onLike: () -> Unit,
    onProfileClick: () -> Unit,
    onCommentsClick: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    var showHeart    by remember { mutableStateOf(false) }
    var heartOffset  by remember { mutableStateOf(Offset.Zero) }

    val exoPlayer = remember(post.videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            if (!post.videoUrl.isNullOrBlank()) {
                setMediaItem(MediaItem.fromUri(Uri.parse(post.videoUrl)))
                prepare()
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
            }
        }
    }
    DisposableEffect(exoPlayer) { onDispose { exoPlayer.release() } }
    LaunchedEffect(isVisible) { if (isVisible) exoPlayer.play() else exoPlayer.pause() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { offset ->
                    if (!isLiked) onLike()
                    heartOffset = offset
                    showHeart   = true
                    scope.launch { delay(900); showHeart = false }
                })
            }
    ) {
        // Video or image
        if (!post.videoUrl.isNullOrBlank()) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player        = exoPlayer
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model              = post.imageUrl,
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize()
            )
        }

        // Gradient scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        // Double-tap heart
        DoubleTapHeart(show = showHeart, offset = heartOffset, accent = accent)

        // Right action bar
        RipplesActionBar(
            post        = post,
            isLiked     = isLiked,
            accent      = accent,
            onLike      = onLike,
            onComments  = onCommentsClick,
            modifier    = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .navigationBarsPadding()
                .padding(bottom = 80.dp)
        )

        // Bottom author info
        RipplesAuthorBar(
            post           = post,
            accent         = accent,
            onProfileClick = onProfileClick,
            modifier       = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(bottom = 80.dp, start = 16.dp, end = 80.dp)
        )
    }
}

// ── Double-tap heart overlay ─────────────────────────────────────────────────

@Composable
private fun DoubleTapHeart(show: Boolean, offset: Offset, accent: Color) {
    val scale by animateFloatAsState(
        targetValue   = if (show) 1f else 0.4f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label         = "heart_scale"
    )
    val alpha by animateFloatAsState(
        targetValue   = if (show) 1f else 0f,
        animationSpec = tween(350),
        label         = "heart_alpha"
    )
    if (alpha > 0.01f) {
        Box(Modifier.fillMaxSize()) {
            Icon(
                imageVector        = Icons.Filled.Favorite,
                contentDescription = null,
                tint               = accent,
                modifier           = Modifier
                    .size(80.dp)
                    .graphicsLayer {
                        scaleX       = scale
                        scaleY       = scale
                        this.alpha   = alpha
                        translationX = offset.x - 40.dp.toPx()
                        translationY = offset.y - 40.dp.toPx()
                    }
            )
        }
    }
}

// ── Action bar ───────────────────────────────────────────────────────────────

@Composable
private fun RipplesActionBar(
    post: Post,
    isLiked: Boolean,
    accent: Color,
    onLike: () -> Unit,
    onComments: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier              = modifier,
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(20.dp)
    ) {
        val heartScale by animateFloatAsState(
            targetValue   = if (isLiked) 1.3f else 1f,
            animationSpec = spring(Spring.DampingRatioMediumBouncy),
            label         = "heart_btn"
        )
        // Like
        ActionBtn(
            icon    = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            tint    = if (isLiked) accent else Color.White,
            label   = formatCount(post.likesCount),
            scale   = heartScale,
            onClick = onLike
        )
        // Comments
        ActionBtn(
            icon    = Icons.Outlined.ChatBubbleOutline,
            tint    = Color.White,
            label   = formatCount(post.commentsCount),
            onClick = onComments
        )
        // Echo (share)
        ActionBtn(icon = Icons.Outlined.Send, tint = Color.White, label = "Echo", onClick = {})
        // Ripple reply
        ActionBtn(icon = Icons.Filled.Waves, tint = Color.White, label = "Ripple", onClick = {})
    }
}

@Composable
private fun ActionBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    scale: Float = 1f,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = tint,
                modifier           = Modifier.size(28.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            )
        }
        Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Author bar ───────────────────────────────────────────────────────────────

@Composable
private fun RipplesAuthorBar(
    post: Post,
    accent: Color,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier              = Modifier.clickable(onClick = onProfileClick)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(2.dp, accent, CircleShape)
            ) {
                AsyncImage(
                    model              = post.authorProfileImageUrl,
                    contentDescription = "Avatar",
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
            }
            Text(
                text       = "@${post.authorUsername}",
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp
            )
        }
        if (post.caption.isNotBlank()) {
            Text(
                text     = post.caption,
                color    = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        val tags = Regex("#(\\w+)").findAll(post.caption).map { it.groupValues[1] }.toList().take(3)
        if (tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tags.forEach { tag ->
                    Surface(
                        shape  = RoundedCornerShape(20.dp),
                        color  = accent.copy(alpha = 0.18f),
                        border = BorderStroke(0.5.dp, accent.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text     = "#$tag",
                            color    = accent,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Mode pill ────────────────────────────────────────────────────────────────

@Composable
private fun RipplesModePill(
    isGlobal: Boolean,
    accent: Color,
    onModeChange: (RipplesMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape           = CircleShape,
        color           = Color.Black.copy(alpha = 0.5f),
        shadowElevation = 12.dp,
        modifier        = modifier
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            ModeChip(label = "🌐 Global", selected = isGlobal,  accent = accent, onClick = { onModeChange(RipplesMode.GLOBAL) })
            ModeChip(label = "📍 Local",  selected = !isGlobal, accent = accent, onClick = { onModeChange(RipplesMode.LOCAL) })
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) accent.copy(alpha = 0.22f) else Color.Transparent, label = "chip_bg")
    Surface(onClick = onClick, shape = CircleShape, color = bg) {
        Text(
            text       = label,
            color      = if (selected) accent else Color.White.copy(alpha = 0.55f),
            fontSize   = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun RipplesEmptyState(
    message: String,
    accent: Color,
    onCreatePost: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier              = modifier.padding(32.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(16.dp)
    ) {
        Icon(imageVector = Icons.Filled.Waves, contentDescription = null, tint = accent, modifier = Modifier.size(56.dp))
        Text(text = message, color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
        Button(onClick = onCreatePost, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
            Text("Post a Ripple", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatCount(n: Int) = when {
    n >= 1_000_000 -> "${n / 1_000_000}M"
    n >= 1_000     -> "${n / 1_000}K"
    else           -> n.toString()
}
