package com.vula.app.global.ui.story

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vula.app.core.ui.camera.CameraCaptureScreen
import com.vula.app.core.ui.components.VulaTopBar
import com.vula.app.global.ui.components.VideoPlayer

@Composable
fun CreateStoryScreen(
    onStoryCreated: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: CreateStoryViewModel = hiltViewModel()
) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val uiState by viewModel.uiState.collectAsState()

    var mediaType by remember { mutableStateOf("image") }

    LaunchedEffect(uiState) {
        if (uiState is CreateStoryUiState.Success) {
            onStoryCreated()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        VulaTopBar(
            title = "New Story",
            navigationIcon = onBackClick,
            actions = {
                TextButton(
                    onClick = { imageUri?.let { viewModel.uploadStory(it, mediaType) } },
                    enabled = imageUri != null && uiState !is CreateStoryUiState.Loading
                ) {
                    Text("Post", color = MaterialTheme.colorScheme.primary)
                }
            }
        )

        if (uiState is CreateStoryUiState.Loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (imageUri == null) {
            CameraCaptureScreen(
                onMediaCaptured = { uri, type ->
                    imageUri = uri
                    mediaType = type
                },
                onClose = onBackClick
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f) // Story portrait aspect ratio
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (mediaType == "video") {
                        VideoPlayer(videoUrl = imageUri.toString(), modifier = Modifier.fillMaxSize())
                    } else {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Selected story image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    IconButton(
                        onClick = { imageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Remove image",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

            if (uiState is CreateStoryUiState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (uiState as CreateStoryUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
