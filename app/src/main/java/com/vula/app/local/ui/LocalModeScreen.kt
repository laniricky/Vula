package com.vula.app.local.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vula.app.core.model.LocalPost
import com.vula.app.core.ui.components.FullScreenLoading
import com.vula.app.core.ui.components.VulaTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun LocalModeScreen(
    viewModel: LocalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var permissionsDenied by remember { mutableStateOf(false) }

    val requiredPermissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            permissionsDenied = false
            viewModel.enableLocalMode()
        } else {
            permissionsDenied = true
        }
    }

    fun hasPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        VulaTopBar(
            title = "Local Mode",
            actions = {
                if (uiState is LocalUiState.Active) {
                    TextButton(onClick = { viewModel.disableLocalMode() }) {
                        Text("Leave", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )

        when (val state = uiState) {
            is LocalUiState.Disabled -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Text(
                            "Discover who is around you.",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Connect to your current Wi-Fi network to chat anonymously with others in the same location.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                        if (permissionsDenied) {
                            Text(
                                "Local Mode requires Location and Nearby Devices permissions to find the Wi-Fi network.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                        
                        Button(
                            onClick = {
                                if (hasPermissions()) {
                                    viewModel.enableLocalMode()
                                } else {
                                    permissionLauncher.launch(requiredPermissions.toTypedArray())
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enable Local Mode")
                        }
                    }
                }
            }
            is LocalUiState.Loading -> {
                FullScreenLoading()
            }
            is LocalUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.enableLocalMode() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            is LocalUiState.Active -> {
                LocalActiveContent(state, viewModel)
            }
        }
    }
}

@Composable
fun LocalActiveContent(
    state: LocalUiState.Active,
    viewModel: LocalViewModel
) {
    var selectedTab by remember { mutableStateOf(0) }
    var textInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Connected Anonymously as", style = MaterialTheme.typography.bodySmall)
                Text(state.alias, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Badge {
                Text("${state.peopleHere.size} here")
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Local Feed", modifier = Modifier.padding(16.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("People Here", modifier = Modifier.padding(16.dp))
            }
        }

        if (selectedTab == 0) {
            Column(modifier = Modifier.fillMaxSize().weight(1f)) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(state.posts) { post ->
                        LocalPostCard(post)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 80.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Say something...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.postText(textInput)
                            textInput = ""
                        },
                        enabled = textInput.isNotBlank()
                    ) {
                        Text("Send")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(state.peopleHere) { alias ->
                    ListItem(
                        headlineContent = { Text(alias, fontWeight = FontWeight.Bold) },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(alias.take(1).uppercase())
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun LocalPostCard(post: LocalPost) {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val time = formatter.format(Date(post.createdAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(post.alias, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(time, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(post.text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
