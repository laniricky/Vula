package com.vula.app.global.ui.story

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vula.app.global.ui.components.VideoPlayer
import com.vula.app.core.model.Story
import com.vula.app.core.ui.components.UserAvatar
import com.vula.app.core.util.TimeAgo
import kotlinx.coroutines.delay

@Composable
fun StoryViewerScreen(
    stories: List<Story>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    onReplyToStory: ((authorUserId: String, message: String) -> Unit)? = null
) {
    if (stories.isEmpty()) {
        onDismiss()
        return
    }

    var currentIndex by remember { mutableStateOf(initialIndex) }
    var progress by remember { mutableStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    val currentStory = stories.getOrNull(currentIndex)

    // Auto-advance logic (5 seconds per story)
    LaunchedEffect(currentIndex, isPaused) {
        progress = 0f
        if (!isPaused) {
            val duration = 5000L
            val step = 50L
            val increment = step.toFloat() / duration.toFloat()

            while (progress < 1f) {
                delay(step)
                progress += increment
            }

            if (currentIndex < stories.lastIndex) {
                currentIndex++
            } else {
                onDismiss()
            }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 50, easing = LinearEasing),
        label = "story_progress"
    )

    if (currentStory != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            try { awaitRelease() } finally { isPaused = false }
                        },
                        onTap = { offset ->
                            val width = size.width
                            if (offset.x < width / 3) {
                                if (currentIndex > 0) { currentIndex--; progress = 0f }
                            } else {
                                if (currentIndex < stories.lastIndex) { currentIndex++; progress = 0f }
                                else onDismiss()
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {}
                    ) { change, dragAmount ->
                        change.consume()
                        if (dragAmount.y > 50) onDismiss()
                    }
                }
        ) {
            // Story Media
            if (currentStory.mediaType == "video") {
                VideoPlayer(
                    videoUrl = currentStory.imageUrl,
                    modifier = Modifier.fillMaxSize(),
                    playWhenReady = !isPaused
                )
            } else {
                AsyncImage(
                    model = currentStory.imageUrl,
                    contentDescription = "Story",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Gradient overlay at bottom for readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )

            // Overlay Top Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp)
            ) {
                // Progress Bars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.forEachIndexed { index, _ ->
                        val barProgress = when {
                            index < currentIndex -> 1f
                            index == currentIndex -> animatedProgress
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { barProgress },
                            modifier = Modifier.weight(1f).height(2.dp),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // User Info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(
                        imageUrl = currentStory.authorProfileImageUrl,
                        username = currentStory.authorUsername,
                        size = 36.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = currentStory.authorUsername,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = TimeAgo.format(currentStory.createdAt),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // ── Story Reply Bar ──────────────────────────────────────────────
            if (onReplyToStory != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = {
                            Text(
                                "Reply to ${currentStory.authorUsername}...",
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White.copy(alpha = 0.6f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = Color.White.copy(alpha = 0.15f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                onReplyToStory(currentStory.authorUserId, replyText.trim())
                                replyText = ""
                            }
                        },
                        enabled = replyText.isNotBlank(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send reply",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

