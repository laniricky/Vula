package com.vula.app.auth.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun DynamicBackground(content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()

    // We animate between subtle variants of the primary/surface colors
    val primaryColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
    val secondaryColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
    val tertiaryColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
    val surfaceColor = MaterialTheme.colorScheme.surface

    val color1 by infiniteTransition.animateColor(
        initialValue = primaryColor,
        targetValue = tertiaryColor,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val color2 by infiniteTransition.animateColor(
        initialValue = surfaceColor,
        targetValue = secondaryColor,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(color1, color2)
                )
            )
    ) {
        content()
    }
}
