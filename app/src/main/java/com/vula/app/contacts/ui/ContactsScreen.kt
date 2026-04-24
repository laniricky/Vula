package com.vula.app.contacts.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onBackClick: () -> Unit,
    onContactClick: (String) -> Unit, // Takes the Vula User ID
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
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

    val contacts by viewModel.contacts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    // Map of clean phone number -> Vula User ID
    val phoneToVulaIdMap by viewModel.phoneToVulaIdMap.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(contacts) { contact ->
                        val cleanPhone = viewModel.cleanPhoneNumber(contact.phoneNumber)
                        val vulaUserId = phoneToVulaIdMap[cleanPhone]
                        val isOnVula = vulaUserId != null
                        
                        ContactItem(
                            contact = contact,
                            isOnVula = isOnVula,
                            onClick = { if (vulaUserId != null) onContactClick(vulaUserId) },
                            onInviteClick = {
                                // Launch SMS with invite text
                                val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("smsto:${contact.phoneNumber}")
                                    putExtra("sms_body",
                                        "Hey ${contact.name}! I'm using Vula. Join me: https://vulaapp.com/download")
                                }
                                context.startActivity(smsIntent)
                            }
                        )
                    }
                }
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
    Card(
        modifier = Modifier.fillMaxWidth().then(
            if (isOnVula) Modifier.clickable { onClick() } else Modifier
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = if (isOnVula) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp).fillMaxSize(),
                    tint = if (isOnVula) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isOnVula) "On Vula ✓" else contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOnVula) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
}
