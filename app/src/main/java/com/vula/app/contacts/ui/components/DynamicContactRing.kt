package com.vula.app.contacts.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DynamicContactRing(
    name: String,
    isOnline: Boolean,
    hasUnseenStory: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    val initial = name.firstOrNull()?.uppercase() ?: "?"

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (hasUnseenStory) {
            Canvas(modifier = Modifier.size(size)) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(primaryColor, secondaryColor, tertiaryColor, primaryColor)
                    ),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(size - 8.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF10B981), // Emerald Green
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                    modifier = Modifier.matchParentSize()
                ) {}
            }
        }
    }
}
