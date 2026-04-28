package com.vula.app.global.ui.components

import android.Manifest
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

/**
 * Bottom-sheet composable for recording a ≤10s voice note and sending it
 * as a DM reply. The caller provides [onSendVoiceNote] which receives
 * the Firebase Storage download URL as a message string.
 */
@Composable
fun VoiceNoteReaction(
    storyId: String,
    currentUserId: String,
    onSendVoiceNote: (url: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storage = remember { FirebaseStorage.getInstance() }

    var isRecording by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var secondsElapsed by remember { mutableIntStateOf(0) }
    var outputFile by remember { mutableStateOf<File?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var hasPermission by remember { mutableStateOf(false) }
    var amplitude by remember { mutableFloatStateOf(0f) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Timer & amplitude sampler while recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            secondsElapsed = 0
            while (isRecording && secondsElapsed < 10) {
                delay(200)
                amplitude = (recorder?.maxAmplitude ?: 0) / 32768f
                if (secondsElapsed % 5 == 0) secondsElapsed++ // tick every 5 iterations = ~1s
            }
            if (secondsElapsed >= 10) {
                // Auto-stop at 10s
                recorder?.stop()
                recorder?.release()
                recorder = null
                isRecording = false
            }
        }
    }

    fun startRecording() {
        val file = File(context.cacheDir, "voice_${UUID.randomUUID()}.m4a")
        outputFile = file
        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mr.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = mr
        isRecording = true
    }

    fun stopRecording() {
        recorder?.stop()
        recorder?.release()
        recorder = null
        isRecording = false
    }

    fun uploadAndSend() {
        val file = outputFile ?: return
        isUploading = true
        scope.launch(Dispatchers.IO) {
            try {
                val ref = storage.reference.child(
                    "voice_reactions/$storyId/${currentUserId}_${UUID.randomUUID()}.m4a"
                )
                ref.putFile(android.net.Uri.fromFile(file)).await()
                val url = ref.downloadUrl.await().toString()
                launch(Dispatchers.Main) {
                    isUploading = false
                    onSendVoiceNote("🎤 Voice note: $url")
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) { isUploading = false }
            }
        }
    }

    // Pulsing animation for mic button
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Handle
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isRecording) "Recording…" else "Hold to record a voice reaction",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${secondsElapsed}s / 10s",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Amplitude bar (5 bars)
            if (isRecording) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(40.dp)
                ) {
                    repeat(5) { i ->
                        val barHeight = ((amplitude * 40 * (0.4f + (i % 3) * 0.3f))
                            .coerceIn(4f, 40f)).dp
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(56.dp))
            }

            // Mic / Stop button
            if (!hasPermission) {
                Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text("Grant Microphone Permission")
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(if (isRecording) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) Color(0xFFE53935) else MaterialTheme.colorScheme.primary
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (isRecording) stopRecording()
                            else startRecording()
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Stop recording" else "Start recording",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Send / Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { uploadAndSend() },
                    enabled = outputFile != null && !isRecording && !isUploading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
