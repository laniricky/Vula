package com.vula.app.auth.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Check
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ButtonState {
    Idle, Loading, Success, Error
}

@Composable
fun AnimatedSubmitButton(
    text: String,
    state: ButtonState,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isShaking by remember { mutableStateOf(false) }

    // Shake animation
    val shake by animateFloatAsState(
        targetValue = if (isShaking) 10f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessHigh
        )
    )

    LaunchedEffect(state) {
        if (state == ButtonState.Error) {
            isShaking = true
            delay(100)
            isShaking = false
        }
    }

    // Morph animation (width and shape)
    val widthPercent by animateFloatAsState(
        targetValue = if (state == ButtonState.Loading || state == ButtonState.Success) 0f else 1f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )
    val cornerRadius by animateIntAsState(
        targetValue = if (state == ButtonState.Loading || state == ButtonState.Success) 50 else 16,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer { translationX = if (isShaking) shake else -shake }
    ) {
        Button(
            onClick = onClick,
            enabled = enabled && state != ButtonState.Loading && state != ButtonState.Success,
            shape = RoundedCornerShape(cornerRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state == ButtonState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth(if (widthPercent > 0.15f) widthPercent else 0.15f) // Don't collapse completely, keep circle size
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (state == ButtonState.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else if (state == ButtonState.Success) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Check,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
