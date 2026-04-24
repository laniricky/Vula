package com.vula.app.core.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraCaptureScreen(
    onMediaCaptured: (Uri, String) -> Unit, // uri and mediaType ("image" or "video")
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermissions by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions[Manifest.permission.CAMERA] == true &&
                         permissions[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        val cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        val audioPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (cameraPermission == PackageManager.PERMISSION_GRANTED && audioPermission == PackageManager.PERMISSION_GRANTED) {
            hasPermissions = true
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    if (!hasPermissions) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("Camera and Audio permissions are required.", color = Color.White)
        }
        return
    }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val videoCapture = remember { VideoCapture.withOutput(Recorder.Builder().build()) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    var recording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val type = context.contentResolver.getType(uri) ?: ""
            val mediaType = if (type.startsWith("video")) "video" else "image"
            onMediaCaptured(uri, mediaType)
        }
    }

    LaunchedEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    videoCapture
                )
            } catch (e: Exception) {
                Log.e("CameraX", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 32.dp, end = 32.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery Picker
            IconButton(onClick = { galleryLauncher.launch("*/*") }) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
            }

            // Capture Button (Tap for photo, Hold for video)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(
                        width = if (isRecording) 8.dp else 4.dp,
                        color = if (isRecording) Color.Red else Color.White,
                        shape = CircleShape
                    )
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) Color.Red else Color.White)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                // Take Photo
                                val photoFile = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                imageCapture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            onMediaCaptured(Uri.fromFile(photoFile), "image")
                                        }
                                        override fun onError(exc: ImageCaptureException) {
                                            Log.e("CameraX", "Photo capture failed: ${exc.message}", exc)
                                        }
                                    }
                                )
                            },
                            onPress = {
                                // Start Video
                                val videoFile = File(context.cacheDir, "video_${System.currentTimeMillis()}.mp4")
                                val outputOptions = FileOutputOptions.Builder(videoFile).build()

                                val currentRecording = videoCapture.output
                                    .prepareRecording(context, outputOptions)
                                    .withAudioEnabled()
                                    .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                                        when (recordEvent) {
                                            is VideoRecordEvent.Start -> isRecording = true
                                            is VideoRecordEvent.Finalize -> {
                                                isRecording = false
                                                if (!recordEvent.hasError()) {
                                                    onMediaCaptured(Uri.fromFile(videoFile), "video")
                                                } else {
                                                    recording?.close()
                                                    recording = null
                                                }
                                            }
                                        }
                                    }
                                recording = currentRecording

                                tryAwaitRelease()

                                // Stop Video
                                recording?.stop()
                                recording = null
                            }
                        )
                    }
            )

            // Switch Camera
            IconButton(onClick = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }
            }) {
                Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White)
            }
        }
    }
}
