package com.vula.app.global.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vula.app.core.ui.components.VulaTopBar

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        VulaTopBar(
            title = "Settings",
            navigationIcon = onBackClick
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Settings Options
            Column {
                Text("Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                ListItem(
                    headlineContent = { Text("Notifications") },
                    modifier = Modifier.fillMaxWidth()
                )
                ListItem(
                    headlineContent = { Text("Privacy") },
                    modifier = Modifier.fillMaxWidth()
                )
                ListItem(
                    headlineContent = { Text("Help & Support") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Logout at the bottom
            Button(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("Log Out")
            }
        }
    }
}
