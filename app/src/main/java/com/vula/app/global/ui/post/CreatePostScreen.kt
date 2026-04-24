package com.vula.app.global.ui.post

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import coil.compose.AsyncImage
import com.vula.app.core.ui.camera.CameraCaptureScreen
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

    Column(modifier = Modifier.fillMaxSize()) {
        VulaTopBar(
            title = "New Post",
            actions = {
                TextButton(
                    onClick = { viewModel.createPost(caption, imageUri, mediaType) },
                    enabled = (caption.isNotBlank() || imageUri != null) && uiState !is CreatePostUiState.Loading
                ) {
                    Text("Share", color = MaterialTheme.colorScheme.primary)
                }
            }
        )

        if (uiState is CreatePostUiState.Loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (imageUri == null && caption.isBlank()) {
            CameraCaptureScreen(
                onMediaCaptured = { uri, type ->
                    imageUri = uri
                    mediaType = type
                },
                onClose = { /* Optional: Navigate back if close is clicked here */ }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Modern Image Picker Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (mediaType == "video") {
                        VideoPlayer(videoUrl = imageUri.toString(), modifier = Modifier.fillMaxSize())
                    } else if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Selected image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (imageUri != null) {
                        // Clear image button
                        IconButton(
                            onClick = { imageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(
                                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
                                    RoundedCornerShape(50)
                                )
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Remove image",
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Add Photo",
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Tap to add photo", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

            Spacer(modifier = Modifier.height(24.dp))

            // Borderless Caption TextField
            BasicTextField(
                value = caption,
                onValueChange = { caption = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (caption.isEmpty()) {
                            Text(
                                "Write a caption...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (uiState is CreatePostUiState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (uiState as CreatePostUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
    }
}
