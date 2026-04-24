package com.vula.app.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vula.app.core.model.ChatRoom
import com.vula.app.core.model.MessageRequest
import com.vula.app.core.ui.components.UserAvatar
import com.vula.app.core.ui.components.VulaTopBar
import com.vula.app.core.util.TimeAgo

@Composable
fun ChatListScreen(
    onChatClick: (String) -> Unit,
    onUserClick: (String) -> Unit = {},
    onNavigateToContacts: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val rooms by viewModel.roomsState.collectAsState()
    val requests by viewModel.incomingRequests.collectAsState()
    val currentUserId = viewModel.currentUserId

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToContacts,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Person, contentDescription = "Contacts")
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
            VulaTopBar(title = "Chats")

            if (rooms.isEmpty() && requests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💬", style = MaterialTheme.typography.displayMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No chats yet.\nGo to a user's profile to start one!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // ── Pending message requests ─────────────────────────────
                    if (requests.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Message Requests",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Badge { Text("${requests.size}") }
                            }
                        }
                        items(requests, key = { "req_${it.id}" }) { request ->
                            MessageRequestItem(
                                request = request,
                                onAccept = { roomId ->
                                    viewModel.acceptRequest(request.id, request.fromUserId) { id ->
                                        id?.let { onChatClick(it) }
                                    }
                                },
                                onDecline = { viewModel.declineRequest(request.id) },
                                onUserClick = { onUserClick(request.fromUserId) }
                            )
                            HorizontalDivider()
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    // ── Chat rooms ───────────────────────────────────────────
                    if (rooms.isNotEmpty()) {
                        item {
                            if (requests.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        "Messages",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        items(rooms, key = { it.id }) { room ->
                            ChatRoomItem(
                                room = room,
                                currentUserId = currentUserId,
                                hasUnread = currentUserId != null && room.unreadFor.contains(currentUserId),
                                onClick = { onChatClick(room.id) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun ChatRoomItem(
    room: ChatRoom,
    currentUserId: String?,
    hasUnread: Boolean,
    onClick: () -> Unit
) {
    val otherUserId = room.participants.firstOrNull { it != currentUserId } ?: "Unknown"
    val roomName = room.name ?: "Chat"
    val time = if (room.lastMessageAt > 0) TimeAgo.format(room.lastMessageAt) else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            UserAvatar(imageUrl = null, username = roomName, size = 48.dp)
            if (hasUnread) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .align(Alignment.TopEnd)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    roomName,
                    fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    color = if (hasUnread) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Text(time, style = MaterialTheme.typography.bodySmall,
                    color = if (hasUnread) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            val isOtherTyping = currentUserId != null && room.typingUsers.any { it != currentUserId }
            Text(
                text = if (isOtherTyping) "Typing..." else (room.lastMessage ?: "No messages yet"),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = if (isOtherTyping) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isOtherTyping) MaterialTheme.colorScheme.primary else if (hasUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MessageRequestItem(
    request: MessageRequest,
    onAccept: (String) -> Unit,
    onDecline: () -> Unit,
    onUserClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUserClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            imageUrl = request.fromProfileImageUrl,
            username = request.fromUsername,
            size = 44.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(request.fromUsername, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge)
            Text("Wants to send you a message",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilledIconButton(
                onClick = { onAccept(request.id) },
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = "Accept", modifier = Modifier.size(18.dp))
            }
            FilledIconButton(
                onClick = onDecline,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = "Decline", modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}
