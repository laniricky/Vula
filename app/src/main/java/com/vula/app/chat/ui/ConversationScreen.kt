package com.vula.app.chat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vula.app.core.model.Message

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    chatRoomId: String,
    onBackClick: () -> Unit,
    // Optional context for Icebreakers (e.g. rich status from contacts)
    replyContext: String? = null,
    // Optional: other user's name for the top bar
    contactName: String? = null,
    // Shared transition scope from NavHost
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messagesState.collectAsState()
    val currentRoom by viewModel.currentRoom.collectAsState()
    val roomNames by viewModel.roomNames.collectAsState()
    val currentUserId = viewModel.currentUserId
    var textInput by remember { mutableStateOf("") }
    var activeReplyContext by remember { mutableStateOf(replyContext) }

    val resolvedContactName = contactName ?: roomNames[chatRoomId] ?: currentRoom?.name ?: "Chat"

    val listState = rememberLazyListState()

    val otherParticipantsTyping = currentRoom?.typingUsers?.filter { it != currentUserId } ?: emptyList()
    val isOtherTyping = otherParticipantsTyping.isNotEmpty()

    LaunchedEffect(chatRoomId) {
        viewModel.loadMessages(chatRoomId)
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top App Bar ──────────────────────────────────────────────────
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Hero Avatar — shared element key matches ContactsScreen
                        val avatarInitial = resolvedContactName.firstOrNull()?.uppercase() ?: "?"
                        val avatarModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                            with(sharedTransitionScope) {
                                Modifier.sharedElement(
                                    state = rememberSharedContentState(key = "avatar_${chatRoomId}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }
                        } else Modifier

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = avatarModifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = avatarInitial,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = resolvedContactName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            // ── Message List ─────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                reverseLayout = true,
                state = listState
            ) {
                items(messages.reversed()) { msg ->
                    val isMe = msg.senderId == currentUserId
                    val isReadByOther = currentRoom?.participants?.filter { it != currentUserId }
                        ?.any { msg.readBy.contains(it) } ?: false
                    MessageBubble(message = msg, isMe = isMe, isRead = isReadByOther)
                }
            }

            // ── Typing Indicator ─────────────────────────────────────────────
            AnimatedVisibility(visible = isOtherTyping) {
                Text(
                    text = "Typing...",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )
            }

            // ── Icebreaker Reply Pill ────────────────────────────────────────
            AnimatedVisibility(
                visible = activeReplyContext != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                activeReplyContext?.let { ctx ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "💬 Replying to status",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = ctx,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { activeReplyContext = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // ── Input Row ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = {
                        textInput = it
                        if (it.isNotEmpty()) viewModel.setTyping(chatRoomId)
                    },
                    placeholder = { Text("Message...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                val canSend = textInput.isNotBlank()
                FilledIconButton(
                    onClick = {
                        val contextPrefix = activeReplyContext?.let { "> *${it}*\n" } ?: ""
                        viewModel.sendMessage(chatRoomId, "$contextPrefix${textInput.trim()}")
                        textInput = ""
                        activeReplyContext = null
                    },
                    enabled = canSend,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, isMe: Boolean, isRead: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isMe) 18.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 18.dp
                    )
                )
                .background(
                    if (isMe) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                color = if (isMe) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isMe) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isRead) "Read ✓✓" else "Sent ✓",
                fontSize = 10.sp,
                color = if (isRead) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.End,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}
