package com.vula.app.contacts.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.vula.app.contacts.data.Contact
import com.vula.app.contacts.ui.components.DynamicContactRing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactsScreen(
    onBackClick: () -> Unit,
    onContactClick: (userId: String, richStatus: String?, contactName: String) -> Unit,
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showRadar by remember { mutableStateOf(false) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasPermission = isGranted
        if (isGranted) viewModel.loadContacts()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.loadContacts()
        else permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    val contacts by viewModel.processedContacts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    if (showRadar) {
        RadarScreen(onNavigateBack = { showRadar = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showRadar = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Radar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                !hasPermission -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Contact permission is required to find your friends.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }) {
                            Text("Grant Permission")
                        }
                    }
                }
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                contacts.isEmpty() -> Text(
                    text = "No contacts found.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
                else -> {
                    val onVula = contacts.filter { it.vulaUserId != null }
                    val notOnVula = contacts.filter { it.vulaUserId == null }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item {
                            RadarBanner(onClick = { showRadar = true })
                        }

                        if (onVula.isNotEmpty()) {
                            stickyHeader {
                                CategoryHeader("The Inner Circle")
                            }
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(onVula.take(5)) { contact ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable { onContactClick(contact.vulaUserId!!, contact.richStatus, contact.name) }
                                        ) {
                                            DynamicContactRing(
                                                name = contact.name,
                                                isOnline = contact.isOnline,
                                                hasUnseenStory = contact.lastStoryTimestamp > 0L,
                                                size = 64.dp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = contact.name.split(" ").firstOrNull() ?: contact.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            stickyHeader {
                                CategoryHeader("On Vula")
                            }
                            items(onVula) { contact ->
                                ContactItem(
                                    contact = contact,
                                    isOnVula = true,
                                    onClick = { onContactClick(contact.vulaUserId!!, contact.richStatus, contact.name) },
                                    onInviteClick = {}
                                )
                            }
                        }

                        if (notOnVula.isNotEmpty()) {
                            stickyHeader {
                                CategoryHeader("Invite to Vula")
                            }
                            items(notOnVula) { contact ->
                                ContactItem(
                                    contact = contact,
                                    isOnVula = false,
                                    onClick = { },
                                    onInviteClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Hey ${contact.name}! I'm using Vula. Join me: https://vulaapp.com/invite"
                                            )
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, "Invite ${contact.name} via...")
                                        context.startActivity(shareIntent)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryHeader(title: String) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun RadarBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Vula Radar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Discover nearby users via Wi-Fi", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun ContactItem(
    contact: Contact,
    isOnVula: Boolean,
    onClick: () -> Unit,
    onInviteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isOnVula) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isOnVula) {
            DynamicContactRing(
                name = contact.name,
                isOnline = contact.isOnline,
                hasUnseenStory = contact.lastStoryTimestamp > 0L
            )
        } else {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = contact.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isOnVula && contact.richStatus != null) {
                Text(
                    text = contact.richStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (!isOnVula) {
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!isOnVula) {
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onInviteClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Invite", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
