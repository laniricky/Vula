package com.vula.app.global.ui.post

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vula.app.core.ui.components.VulaTopBar
import com.vula.app.global.ui.components.VideoPlayer

@Composable
fun CreatePostScreen(
    onPostCreated: () -> Unit,
    viewModel: CreatePostViewModel = hiltViewModel()
) {
    var caption by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val uiState by viewModel.uiState.collectAsState()
    var mediaType by remember { mutableStateOf("image") }

    LaunchedEffect(uiState) {
        if (uiState is CreatePostUiState.Success) {
            onPostCreated()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            VulaTopBar(
                title = "Upload",
                onMenuClick = { /* Drawer or menu */ }
            )

            if (uiState is CreatePostUiState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // ── Upload Box ──────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { /* trigger file picker */ },
                    contentAlignment = Alignment.Center
                ) {
                    // Dashed border effect (simulated with standard border for simplicity)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Add Media",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Add photos or videos",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Drag & drop or tap to select",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Pills
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = false,
                                    onClick = { },
                                    label = { Text("Photos") },
                                    leadingIcon = { Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp)) }
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = { },
                                    label = { Text("Videos") },
                                    leadingIcon = { Icon(Icons.Default.Videocam, null, modifier = Modifier.size(16.dp)) }
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = { },
                                    label = { Text("✨ Mixed") }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Recents ─────────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recents",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "See all",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.clickable { }
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Mock recents row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(4) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            // Check circle indicator
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(20.dp)
                                    .border(1.5.dp, Color.White, CircleShape)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Post Details ────────────────────────────────────────────────
                Text(
                    text = "Post details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicTextField(
                                value = caption,
                                onValueChange = { caption = it },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (caption.isEmpty()) {
                                            Text(
                                                "Write a caption...",
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                        Text(
                            text = "${caption.length}/2,200",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.align(Alignment.End)
                        )
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                        
                        // Settings items
                        SettingsItem(Icons.Default.LocationOn, "Add location")
                        SettingsItem(Icons.Default.Person, "Tag people")
                        SettingsItem(Icons.Default.TextFormat, "Add alt text")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Additional Settings ─────────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Who can see this?", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text("Everyone", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Advanced settings", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Action Buttons ──────────────────────────────────────────────
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.createPost(caption, imageUri, mediaType) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
                    ) {
                        Text("Share", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier
                            .weight(0.4f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Schedule", color = MaterialTheme.colorScheme.onBackground)
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun SettingsItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
    }
}
