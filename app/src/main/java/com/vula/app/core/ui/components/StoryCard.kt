package com.vula.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vula.app.core.model.Story

@Composable
fun StoryCard(
    story: Story,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientColors = if (story.isViewed) {
        listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
    } else {
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.primaryContainer
        )
    }

    Column(
        modifier = modifier
            .width(72.dp)
            .clickable { onClick() }
            .semantics(mergeDescendants = true) {
                contentDescription = if (story.isViewed) "Viewed story from ${story.authorUsername}" else "Unread story from ${story.authorUsername}"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar with gradient border
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(gradientColors))
                .padding(3.dp) // Border thickness
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface) // Inner padding
                .padding(2.dp) // Inner gap
                .clip(CircleShape)
        ) {
            if (story.authorProfileImageUrl != null) {
                AsyncImage(
                    model = story.authorProfileImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clearAndSetSemantics {},
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = story.authorUsername.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.clearAndSetSemantics {}
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Username
        Text(
            text = story.authorUsername,
            style = MaterialTheme.typography.labelSmall,
            color = if (story.isViewed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clearAndSetSemantics {}
        )
    }
}

@Composable
fun AddStoryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(72.dp)
            .clickable { onClick() }
            .semantics(mergeDescendants = true) {
                contentDescription = "Create a new story"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar with Add Icon
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Your Story",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
