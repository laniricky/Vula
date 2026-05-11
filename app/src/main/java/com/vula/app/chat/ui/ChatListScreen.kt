package com.vula.app.chat.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vula.app.core.ui.components.UserAvatar
import com.vula.app.core.ui.components.VulaTopBar

@Composable
fun ChatListScreen(
    onChatClick: (String) -> Unit,
    onUserClick: (String) -> Unit = {},
    onNavigateToContacts: () -> Unit = {},
    onMenuClick: (() -> Unit)? = null,
    viewModel: ChatViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("All", "Messages", "Mentions", "Likes", "Comments", "Follows")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            VulaTopBar(
                title = "Recent Activity",
                showStats = false,
                actions = {
                    IconButton(onClick = { /* Mark all as read */ }) {
                        Icon(Icons.Default.DoneAll, contentDescription = "Mark all read", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = { /* Open Notification Settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            )

            // ── Tabs ──────────────────────────────────────────────────────────
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tabs.size) { index ->
                    val isSelected = selectedTab == index
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        label = { Text(tabs[index], fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = {
                            if (tabs[index] == "Messages") Icon(Icons.Default.Message, null, modifier = Modifier.size(16.dp))
                            if (tabs[index] == "Mentions") Icon(Icons.Default.AlternateEmail, null, modifier = Modifier.size(16.dp))
                            if (tabs[index] == "Likes") Icon(Icons.Default.Favorite, null, modifier = Modifier.size(16.dp))
                            if (tabs[index] == "Comments") Icon(Icons.Default.ChatBubble, null, modifier = Modifier.size(16.dp))
                            if (tabs[index] == "Follows") Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                            selectedLabelColor = MaterialTheme.colorScheme.background,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.background
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            if (tabs[selectedTab] == "Messages") {
                val rooms by viewModel.roomsState.collectAsState()
                val roomNames by viewModel.roomNames.collectAsState()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(rooms.size) { index ->
                        val room = rooms[index]
                        val name = if (room.type == "group") room.name ?: "Group" else (roomNames[room.id] ?: "Unknown")
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChatClick(room.id) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(room.lastMessage ?: "Started a conversation", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, fontSize = 14.sp)
                            }
                            if (room.unreadFor.contains(viewModel.currentUserId)) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                    }
                    if (rooms.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("No messages yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                // ── Today Headers ───────────────────────────────────────────────
                item {
                    Text("New", fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 8.dp))
                }
                
                item {
                    ActivityItem(
                        groupedUsernames = listOf("alex.j", "maria.ss", "+14"),
                        action = "liked your post",
                        time = "2m ago",
                        icon = Icons.Default.Favorite,
                        iconColor = Color(0xFFE91E63),
                        thumbnailUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=200&q=80",
                        isUnread = true
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                }
                
                item {
                    ActivityItem(
                        groupedUsernames = listOf("jake.w"),
                        action = "commented on your post",
                        time = "1h ago",
                        icon = Icons.Default.ChatBubble,
                        iconColor = Color(0xFF4CAF50),
                        thumbnailUrl = "https://images.unsplash.com/photo-1497935586351-b67a49e012bf?auto=format&fit=crop&w=200&q=80",
                        isUnread = true,
                        commentPreview = "This is absolutely stunning! 🔥 Keep up the great work.",
                        onReplyClick = { /* Handle inline reply */ }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                }

                item {
                    ActivityItem(
                        groupedUsernames = listOf("saraah", "minavibes"),
                        action = "started following you",
                        time = "3h ago",
                        icon = Icons.Default.Person,
                        iconColor = MaterialTheme.colorScheme.primary,
                        isFollow = true,
                        isUnread = true
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                }

                item {
                    Text("This Week", fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp))
                }

                // ── Milestone Card ────────────────────────────────────────────
                item {
                    MilestoneWidget()
                }

                item {
                    ActivityItem(
                        groupedUsernames = listOf("ilove.travel"),
                        action = "mentioned you in a comment",
                        time = "1d ago",
                        icon = Icons.Default.AlternateEmail,
                        iconColor = Color(0xFF9C27B0),
                        thumbnailUrl = "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?auto=format&fit=crop&w=200&q=80",
                        commentPreview = "@you check this out! 🙌",
                        isUnread = false,
                        onReplyClick = { }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                }

                // ── This Week Stats ───────────────────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Dashboard",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(Icons.Default.Favorite, "128", "Likes")
                        StatItem(Icons.Default.ChatBubble, "36", "Comments")
                        StatItem(Icons.Default.Person, "24", "Follows")
                        StatItem(Icons.Default.Visibility, "1.2K", "Views")
                    }
                }
                } // end LazyColumn (else)
            } // end else
        }
    }
}

@Composable
fun MilestoneWidget() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 0.85f },
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    strokeCap = StrokeCap.Round
                )
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Only 45 more likes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("to hit your weekly creator goal! \uD83C\uDF89", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ActivityItem(
    groupedUsernames: List<String>,
    action: String,
    time: String,
    icon: ImageVector,
    iconColor: Color,
    thumbnailUrl: String? = null,
    isFollow: Boolean = false,
    isUnread: Boolean = false,
    commentPreview: String? = null,
    onReplyClick: (() -> Unit)? = null
) {
    val bgColor = if (isUnread) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Avatars cluster
        Box {
            Row(modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)) {
                val displayCount = minOf(3, groupedUsernames.size)
                for (i in 0 until displayCount) {
                    val name = groupedUsernames[i]
                    Box(
                        modifier = Modifier
                            .offset(x = (-8 * i).dp)
                            .size(42.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (name.startsWith("+")) {
                            Text(name, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text(name.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
            // Action badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(iconColor)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f).padding(top = 2.dp)) {
            val namesStr = if (groupedUsernames.size > 2 && groupedUsernames.last().startsWith("+")) {
                "${groupedUsernames[0]}, ${groupedUsernames[1]} and ${groupedUsernames.last().drop(1)} others"
            } else if (groupedUsernames.size > 1) {
                groupedUsernames.joinToString(" and ")
            } else {
                groupedUsernames.firstOrNull() ?: ""
            }

            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(namesStr) }
                    append(" ")
                    append(action)
                },
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (commentPreview != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                            .padding(end = 8.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = commentPreview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        if (onReplyClick != null) {
                            Text(
                                "Reply",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onReplyClick() }.padding(end = 8.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Trailing element
        if (isUnread) {
            Box(modifier = Modifier.padding(top = 8.dp, end = 4.dp).size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }

        if (isFollow) {
            var isFollowedBack by remember { mutableStateOf(false) }
            val buttonColor by animateColorAsState(if (isFollowedBack) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.onBackground, label = "btn")
            val textColor by animateColorAsState(if (isFollowedBack) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.background, label = "txt")
            
            Button(
                onClick = { 
                    isFollowedBack = !isFollowedBack
                    if (isFollowedBack) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                if (isFollowedBack) {
                    Icon(Icons.Default.Check, contentDescription = "Followed", tint = textColor, modifier = Modifier.size(16.dp))
                } else {
                    Text("Follow back", color = textColor, fontSize = 12.sp)
                }
            }
        } else if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun StatItem(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
