package com.vula.app.chat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vula.app.core.model.ChatRoom
import com.vula.app.core.ui.components.UserAvatar
import com.vula.app.core.ui.components.VulaTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatListScreen(
    onChatClick: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val rooms by viewModel.roomsState.collectAsState()
    val currentUserId = viewModel.currentUserId

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            VulaTopBar(title = "Chats")

            if (rooms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No chats yet. Go to a user's profile to start one!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(rooms) { room ->
                        ChatRoomItem(
                            room = room,
                            currentUserId = currentUserId,
                            onClick = { onChatClick(room.id) }
                        )
                        HorizontalDivider()
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
    onClick: () -> Unit
) {
    val otherUserId = room.participants.firstOrNull { it != currentUserId } ?: "Unknown"
    val roomName = room.name ?: "User: ${otherUserId.take(8)}..."
    
    val time = if (room.lastMessageAt > 0) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(room.lastMessageAt))
    } else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(imageUrl = null, username = roomName, size = 48.dp)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(roomName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = room.lastMessage ?: "No messages yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
