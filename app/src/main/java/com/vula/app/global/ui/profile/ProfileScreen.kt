package com.vula.app.global.ui.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vula.app.core.ui.components.FullScreenLoading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String? = null,
    onLogoutClick: () -> Unit = {},
    onNavigateToConversation: (String) -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val messageState by viewModel.messageState.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    val gridState = rememberLazyGridState()
    val haptic = LocalHapticFeedback.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Calculate scroll offset for parallax and top bar opacity
    val scrollOffset = remember { derivedStateOf { gridState.firstVisibleItemScrollOffset } }
    val firstItemIndex = remember { derivedStateOf { gridState.firstVisibleItemIndex } }
    
    // Top bar background opacity becomes solid as you scroll past the header
    val topBarAlpha by animateFloatAsState(
        targetValue = if (firstItemIndex.value > 0 || scrollOffset.value > 300) 1f else 0f,
        animationSpec = tween(300),
        label = "TopBarAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is ProfileUiState.Loading -> FullScreenLoading()
            is ProfileUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is ProfileUiState.Success -> {
                val coverPhotoUrl = state.user.profileImageUrl ?: "https://images.unsplash.com/photo-1557683316-973673baf926?q=80&w=600&auto=format&fit=crop"

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // ── Header (Spans all columns) ───────────────────────────
                    item(span = { GridItemSpan(3) }) {
                        Column {
                            // Parallax Cover Photo
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .graphicsLayer {
                                        // Parallax effect
                                        if (firstItemIndex.value == 0) {
                                            translationY = scrollOffset.value * 0.5f
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = coverPhotoUrl,
                                    contentDescription = "Cover Photo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .blur(if (state.user.profileImageUrl != null) 20.dp else 0.dp), // Blur avatar if used as cover
                                    contentScale = ContentScale.Crop
                                )
                                // Dark gradient overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, MaterialTheme.colorScheme.background),
                                                startY = 0f,
                                                endY = Float.POSITIVE_INFINITY
                                            )
                                        )
                                )
                            }

                            // Avatar, Name, Stats
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = (-48).dp) // Overlap cover photo
                                    .padding(horizontal = 20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Glowing Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                                )
                                            )
                                            .padding(3.dp) // Glow border thickness
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.background)
                                            .padding(3.dp)
                                            .clip(CircleShape)
                                    ) {
                                        if (!state.user.profileImageUrl.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = state.user.profileImageUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = state.user.displayName.take(1).uppercase(),
                                                    style = MaterialTheme.typography.headlineMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }

                                    // Action Buttons
                                    if (state.isOwnProfile) {
                                        OutlinedButton(
                                            onClick = onEditProfileClick,
                                            modifier = Modifier.padding(bottom = 8.dp),
                                            shape = RoundedCornerShape(20.dp),
                                            contentPadding = PaddingValues(horizontal = 24.dp)
                                        ) {
                                            Text("Edit Profile", fontWeight = FontWeight.SemiBold)
                                        }
                                    } else {
                                        Row(modifier = Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { viewModel.openOrCreateChat(state.user.id) { roomId -> onNavigateToConversation(roomId) } },
                                                shape = RoundedCornerShape(20.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                            ) {
                                                Text("Message")
                                            }
                                            Button(
                                                onClick = { viewModel.toggleFollow(state.user.id, state.isFollowing) },
                                                shape = RoundedCornerShape(20.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (state.isFollowing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                                    contentColor = if (state.isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                                                )
                                            ) {
                                                Text(if (state.isFollowing) "Following" else "Follow", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Name and "Vibe"
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = state.user.displayName.ifBlank { state.user.username },
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 24.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Simulated "Vibe" badge
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text("🎧 Chilling", fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                                
                                Text(
                                    text = "@${state.user.username}",
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                )

                                if (state.user.bio.isNotEmpty()) {
                                    Text(
                                        text = state.user.bio,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(top = 12.dp),
                                        lineHeight = 20.sp
                                    )
                                }

                                // Link simulation
                                Text(
                                    text = "🔗 linktr.ee/${state.user.username}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Followers / Views Stats
                                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                    ProfileStat(formatCount(state.user.followersCount), "Followers")
                                    ProfileStat(formatCount(state.user.followingCount), "Views")
                                    ProfileStat(formatCount(state.posts.size), "Posts")
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))

                                // Story Highlights (Mocked)
                                Text("Highlights", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    items(4) { index ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(CircleShape)
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                                    .padding(4.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("Vibe ${index + 1}", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Sticky Tabs ──────────────────────────────────────────────────
                    item(span = { GridItemSpan(3) }) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp).offset(y = (-48).dp)) // account for negative offset
                            TabRow(
                                selectedTabIndex = selectedTab,
                                containerColor = MaterialTheme.colorScheme.background,
                                modifier = Modifier.offset(y = (-48).dp),
                                divider = { HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant) }
                            ) {
                                Tab(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = { Icon(Icons.Default.GridOn, contentDescription = "Posts") }
                                )
                                Tab(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = { Icon(Icons.Default.PlayCircleOutline, contentDescription = "Clips") }
                                )
                                if (state.isOwnProfile) {
                                    Tab(
                                        selected = selectedTab == 2,
                                        onClick = { selectedTab = 2 },
                                        icon = { Icon(Icons.Default.BookmarkBorder, contentDescription = "Saved") }
                                    )
                                }
                            }
                        }
                    }

                    // ── Post Grid ────────────────────────────────────────────────────
                    val displayPosts = state.posts // In a real app, filter based on selectedTab
                    items(displayPosts) { post ->
                        var isPressed by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "pressScale")

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .offset(y = (-48).dp)
                                .scale(scale)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isPressed = true
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            tryAwaitRelease()
                                            isPressed = false
                                        },
                                        onLongPress = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            // Feature: Haptic Peek overlay trigger goes here
                                        }
                                    )
                                }
                        ) {
                            if (post.imageUrl != null) {
                                AsyncImage(
                                    model = post.imageUrl,
                                    contentDescription = "Post thumbnail",
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
                                        text = post.caption.take(30),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Custom Glassmorphism Top Bar ───────────────────────────────────────────
        val topBarBgColor by animateColorAsState(
            targetValue = if (topBarAlpha > 0.5f) MaterialTheme.colorScheme.background else Color.Transparent,
            label = "TopBarColor"
        )
        val contentColor by animateColorAsState(
            targetValue = if (topBarAlpha > 0.5f) MaterialTheme.colorScheme.onBackground else Color.White,
            label = "TopBarContentColor"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(topBarBgColor)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = contentColor)
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            Text(
                text = if (uiState is ProfileUiState.Success) {
                    val state = uiState as ProfileUiState.Success
                    if (topBarAlpha > 0.5f) state.user.username else ""
                } else "",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = contentColor,
                modifier = Modifier.weight(1f).padding(start = 16.dp)
            )

            // QR Code / Share icon
            IconButton(onClick = { showBottomSheet = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = contentColor)
            }
        }
        
        if (showBottomSheet) {
            val scope = rememberCoroutineScope()
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, top = 8.dp)
                ) {
                    if (uiState is ProfileUiState.Success) {
                        val state = uiState as ProfileUiState.Success
                        if (state.isOwnProfile) {
                            BottomSheetItem(icon = Icons.Default.Settings, title = "Settings & Privacy")
                            BottomSheetItem(icon = Icons.Default.QrCode, title = "Share Profile / QR Code")
                            BottomSheetItem(icon = Icons.Default.Archive, title = "Archive & Saved")
                            BottomSheetItem(icon = Icons.Default.Analytics, title = "Analytics / Insights")
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            BottomSheetItem(
                                icon = Icons.Default.ExitToApp,
                                title = "Log Out",
                                tint = MaterialTheme.colorScheme.error,
                                onClick = {
                                    showBottomSheet = false
                                    onLogoutClick()
                                }
                            )
                        } else {
                            BottomSheetItem(icon = Icons.Default.Share, title = "Share this Profile")
                            BottomSheetItem(icon = Icons.Default.VolumeOff, title = "Mute")
                            BottomSheetItem(icon = Icons.Default.Block, title = "Block", tint = MaterialTheme.colorScheme.error)
                            BottomSheetItem(icon = Icons.Default.Flag, title = "Report", tint = MaterialTheme.colorScheme.error)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            BottomSheetItem(icon = Icons.Default.Info, title = "About this Account")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomSheetItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = tint)
    }
}

@Composable
fun ProfileStat(value: String, label: String) {
    Column {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M"
    count >= 1_000     -> "${count / 1_000}K"
    else               -> count.toString()
}
