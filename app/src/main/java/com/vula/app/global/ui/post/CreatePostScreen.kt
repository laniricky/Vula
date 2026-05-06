package com.vula.app.global.ui.post

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import java.io.File
import java.util.concurrent.Executors

// ─── Main Screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onPostCreated: () -> Unit,
    viewModel: CreatePostViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()

    // State
    var hasPermissions by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isRecording by remember { mutableStateOf(false) }
    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    var capturedMediaType by remember { mutableStateOf("image") }
    var contentType by remember { mutableIntStateOf(0) } // 0=Post 1=Story 2=Clip
    var flashEnabled by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var caption by remember { mutableStateOf("") }
    var audienceIndex by remember { mutableIntStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // CameraX
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val videoCapture = remember { VideoCapture.withOutput(Recorder.Builder().build()) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var recording by remember { mutableStateOf<Recording?>(null) }

    // Gallery (real MediaStore)
    val galleryPhotos = remember { loadRecentGalleryUris(context, 20) }

    // Permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasPermissions = perms[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(Unit) {
        val cam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (cam == PackageManager.PERMISSION_GRANTED) hasPermissions = true
        else permissionLauncher.launch(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        )
    }

    // Bind CameraX when permissions ready or lens changes
    LaunchedEffect(hasPermissions, lensFacing) {
        if (!hasPermissions) return@LaunchedEffect
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            try {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture, videoCapture)
            } catch (e: Exception) {
                Log.e("CameraX", "Bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(uiState) {
        if (uiState is CreatePostUiState.Success) onPostCreated()
    }

    // Gallery launcher fallback
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            capturedUri = uri
            capturedMediaType = "image"
            showBottomSheet = true
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        if (!hasPermissions) {
            // Permission gate
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("Camera access needed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text("Allow camera to capture moments", color = Color.White.copy(alpha = 0.6f))
                Spacer(Modifier.height(24.dp))
                Button(onClick = {
                    permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                }) { Text("Allow Camera") }
            }
        } else {
            // ── Live Viewfinder (fills top ~65%) ──────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.72f)
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                )

                // Gradient at bottom of viewfinder for legibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = (-220).dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                            )
                        )
                )

                // ── Top Bar ────────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPostCreated) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    // Flash toggle
                    IconButton(onClick = { flashEnabled = !flashEnabled }) {
                        Icon(
                            if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            null, tint = Color.White
                        )
                    }
                }

                // ── Content Type Switcher (overlaid on viewfinder top) ─────────
                ContentTypeBar(
                    selected = contentType,
                    onSelect = {
                        contentType = it
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 72.dp)
                        .statusBarsPadding()
                )

                // ── Capture Controls (inside viewfinder at bottom edge) ────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = (-230).dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flip camera
                    IconButton(onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                            CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
                    }) {
                        Icon(Icons.Default.Cameraswitch, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    // Capture Button: tap = photo, hold = video
                    val captureRingScale by animateFloatAsState(
                        targetValue = if (isRecording) 1.2f else 1f,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy),
                        label = "ring"
                    )
                    Box(
                        modifier = Modifier
                            .size((80 * captureRingScale).dp)
                            .border(
                                width = if (isRecording) 6.dp else 4.dp,
                                color = if (isRecording) Color.Red else Color.White,
                                shape = CircleShape
                            )
                            .padding(10.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) Color.Red else Color.White)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                                        val opts = ImageCapture.OutputFileOptions.Builder(file).build()
                                        imageCapture.takePicture(
                                            opts,
                                            ContextCompat.getMainExecutor(context),
                                            object : ImageCapture.OnImageSavedCallback {
                                                override fun onImageSaved(out: ImageCapture.OutputFileResults) {
                                                    capturedUri = Uri.fromFile(file)
                                                    capturedMediaType = "image"
                                                    showBottomSheet = true
                                                }
                                                override fun onError(e: ImageCaptureException) {
                                                    Log.e("Camera", "Capture failed", e)
                                                }
                                            }
                                        )
                                    },
                                    onPress = {
                                        val file = File(context.cacheDir, "video_${System.currentTimeMillis()}.mp4")
                                        val opts = FileOutputOptions.Builder(file).build()
                                        val rec = videoCapture.output
                                            .prepareRecording(context, opts)
                                            .withAudioEnabled()
                                            .start(ContextCompat.getMainExecutor(context)) { event ->
                                                when (event) {
                                                    is VideoRecordEvent.Start -> isRecording = true
                                                    is VideoRecordEvent.Finalize -> {
                                                        isRecording = false
                                                        if (!event.hasError()) {
                                                            capturedUri = Uri.fromFile(file)
                                                            capturedMediaType = "video"
                                                            showBottomSheet = true
                                                        }
                                                        recording?.close(); recording = null
                                                    }
                                                }
                                            }
                                        recording = rec
                                        tryAwaitRelease()
                                        recording?.stop(); recording = null
                                    }
                                )
                            }
                    )

                    // Gallery quick-pick
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                // ── Hint label ────────────────────────────────────────────────
                Text(
                    text = if (isRecording) "● Recording — release to stop" else "Tap photo  •  Hold for video",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-195).dp)
                )

                // ── Real Gallery Row ──────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black)
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recents", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Browse all",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { galleryLauncher.launch("image/*") }
                        )
                    }

                    if (galleryPhotos.isEmpty()) {
                        Text(
                            "No recent photos",
                            color = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(galleryPhotos) { _, uri ->
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (capturedUri == uri) 2.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            capturedUri = uri
                                            capturedMediaType = "image"
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            showBottomSheet = true
                                        }
                                ) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // ── Post Details Bottom Sheet ──────────────────────────────────────────
        if (showBottomSheet && capturedUri != null) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding()
                ) {
                    // Preview thumbnail + caption side by side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        AsyncImage(
                            model = capturedUri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        BasicTextField(
                            value = caption,
                            onValueChange = { if (it.length <= 2200) caption = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                            decorationBox = { inner ->
                                Box {
                                    if (caption.isEmpty()) Text(
                                        "Write a caption, #tag or @mention…",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                    inner()
                                }
                            }
                        )
                    }

                    // Char counter ring
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        val frac = caption.length / 2200f
                        CircularProgressIndicator(
                            progress = { frac },
                            modifier = Modifier.size(24.dp),
                            color = if (frac > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                            strokeWidth = 2.5.dp,
                            strokeCap = StrokeCap.Round
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Audience picker
                    Text("Who can see this?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    AudiencePicker(selected = audienceIndex, onSelect = { audienceIndex = it })

                    Spacer(Modifier.height(20.dp))

                    // Share button
                    val isUploading = uiState is CreatePostUiState.Loading
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.createPost(caption, capturedUri, capturedMediaType)
                        },
                        enabled = !isUploading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.background,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Uploading…", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Bold)
                        } else {
                            Text(
                                "Share ${listOf("Post", "Story", "Clip")[contentType]}",
                                color = MaterialTheme.colorScheme.background,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

// ─── Content Type Bar ─────────────────────────────────────────────────────────

@Composable
fun ContentTypeBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val types = listOf("POST", "STORY", "CLIP")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        types.forEachIndexed { idx, label ->
            val isSelected = idx == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(idx) }
            ) {
                Text(
                    label,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .width(20.dp)
                            .height(2.dp)
                            .background(Color.White, RoundedCornerShape(1.dp))
                    )
                }
            }
        }
    }
}

// ─── Audience Picker ─────────────────────────────────────────────────────────

@Composable
fun AudiencePicker(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(
        Triple("🌍", "Everyone", MaterialTheme.colorScheme.primary),
        Triple("👥", "Contacts", MaterialTheme.colorScheme.tertiary),
        Triple("🔒", "Private", Color(0xFF9E9E9E))
    )
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { index, (emoji, label, accent) ->
            val isSelected = selected == index
            val border by animateColorAsState(
                if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant, label = "b"
            )
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(if (isSelected) 1.5.dp else 1.dp, border, RoundedCornerShape(12.dp))
                    .clickable { onSelect(index) },
                color = if (isSelected) accent.copy(alpha = 0.08f) else Color.Transparent
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(emoji, fontSize = 20.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        label, fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface.copy(0.5f)
                    )
                }
            }
        }
    }
}

// ─── Settings Row Item ────────────────────────────────────────────────────────

@Composable
fun SettingsItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable {}.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.6f))
        Spacer(Modifier.width(16.dp))
        Text(label, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.4f))
    }
}

// ─── MediaStore Gallery Loader ────────────────────────────────────────────────

fun loadRecentGalleryUris(context: Context, limit: Int): List<Uri> {
    val uris = mutableListOf<Uri>()
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
    try {
        context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            var count = 0
            while (cursor.moveToNext() && count < limit) {
                val id = cursor.getLong(idCol)
                uris.add(Uri.withAppendedPath(collection, id.toString()))
                count++
            }
        }
    } catch (e: Exception) {
        Log.e("Gallery", "Failed to load gallery", e)
    }
    return uris
}
