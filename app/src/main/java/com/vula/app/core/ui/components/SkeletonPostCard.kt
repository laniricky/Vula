package com.vula.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun SkeletonPostCard() {
    val shimmer = shimmerBrush()
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        // Author row
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(shimmer)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Box(
                    modifier = Modifier
                        .height(12.dp)
                        .width(120.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmer)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height(10.dp)
                        .width(80.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(shimmer)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Image placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(shimmer)
        )
        Spacer(modifier = Modifier.height(10.dp))
        // Caption placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(shimmer)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(shimmer)
        )
    }
}
