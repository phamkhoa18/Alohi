package com.example.alohi.ui.screens.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.ui.platform.LocalContext
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

@Composable
fun VoiceRecordPanel(
    modifier: Modifier = Modifier,
    onSendRecording: (File, Long) -> Unit = { _, _ -> },
    onSendAsText: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationMs by remember { mutableStateOf(0L) }
    var recorder by remember { mutableStateOf<com.example.alohi.utils.AloHiAudioRecorder?>(null) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // start right away
            if (recorder == null) recorder = com.example.alohi.utils.AloHiAudioRecorder(context)
            recordedFile = recorder?.startRecording()
            if (recordedFile != null) {
                isRecording = true
                recordingDurationMs = 0L
            }
        }
    }

    // Timer logic
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (true) {
                delay(100)
                recordingDurationMs += 100
            }
        }
    }

    // Ripple animation for mic
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ── Instruction / Timer text ──
        if (isRecording) {
            val seconds = (recordingDurationMs / 1000).toInt()
            val mm = (seconds / 60).toString().padStart(2, '0')
            val ss = (seconds % 60).toString().padStart(2, '0')
            Text(
                text = "Đang ghi âm... $mm:$ss",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = "Bấm giữ để ghi âm",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF636366),
                fontWeight = FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Large mic button with touch processing ──
        Box(
            modifier = Modifier
                .size(72.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            
                            if (hasPermission) {
                                if (recorder == null) recorder = com.example.alohi.utils.AloHiAudioRecorder(context)
                                recordedFile = recorder?.startRecording()
                                if (recordedFile != null) {
                                    isRecording = true
                                    recordingDurationMs = 0L
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }

                            tryAwaitRelease()

                            // Released
                            if (isRecording) {
                                isRecording = false
                                val finalDuration = recorder?.stopRecording() ?: 0L
                                if (finalDuration > 1000 && recordedFile != null) {
                                    // Send file
                                    onSendRecording(recordedFile!!, finalDuration)
                                } else {
                                    // Too short, cancel
                                    android.widget.Toast.makeText(context, "Ghi âm quá ngắn", android.widget.Toast.LENGTH_SHORT).show()
                                    recorder?.cancelRecording()
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = if (isRecording) 0.3f else 1f))
            )
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Ghi âm",
                tint = if (isRecording) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Toggle buttons: "Gửi bản ghi âm" | "Gửi dạng văn bản" ──
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    color = Color(0xFFD1D1D6),
                    shape = RoundedCornerShape(24.dp)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1C1C1E))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Gửi bản ghi âm",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(
                modifier = Modifier
                    .clickable(onClick = onSendAsText)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Gửi dạng văn bản",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF8E8E93),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
