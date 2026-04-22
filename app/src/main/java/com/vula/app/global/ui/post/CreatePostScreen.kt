package com.vula.app.global.ui.post

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vula.app.core.ui.components.VulaTopBar

@Composable
fun CreatePostScreen(
    onPostCreated: () -> Unit,
    viewModel: CreatePostViewModel = hiltViewModel()
) {
    var caption by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    
    val uiState by viewModel.uiState.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

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
                    onClick = { viewModel.createPost(caption, imageUri) },
                    enabled = (caption.isNotBlank() || imageUri != null) && uiState !is CreatePostUiState.Loading
                ) {
                    Text("Share", color = MaterialTheme.colorScheme.primary)
                }
            }
        )

        if (uiState is CreatePostUiState.Loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                placeholder = { Text("Write a caption...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Selected image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Add Photo",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Tap to add photo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (uiState is CreatePostUiState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (uiState as CreatePostUiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
