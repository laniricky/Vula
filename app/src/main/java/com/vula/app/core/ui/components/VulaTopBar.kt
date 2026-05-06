package com.vula.app.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VulaTopBar(
    title: String,
    followersCount: Int = 50,
    viewsCount: Int = 51,
    showStats: Boolean = true,
    navigationIcon: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Back arrow
        if (navigationIcon != null) {
            IconButton(onClick = navigationIcon) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }

        // Center: Title
        Text(
            text = title,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        )

        // Right: custom actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )

        // Default stats (followers + views)
        if (showStats) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Followers",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "$followersCount",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    Icons.Default.Visibility,
                    contentDescription = "Views",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "$viewsCount",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
