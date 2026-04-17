package com.example.alohi.ui.screens.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.alohi.ui.viewmodel.CallViewModel
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun CallScreen(
    callViewModel: CallViewModel,
    onCallEnded: () -> Unit
) {
    val callState by callViewModel.callState.collectAsState()
    val context = LocalContext.current

    val requiredPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    val hasPermissions = remember {
        requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (!granted) {
            onCallEnded() // Close call if permissions denied
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    // Observe when call effectively resets/ends
    LaunchedEffect(callState.callId) {
        if (callState.callId.isEmpty()) {
            onCallEnded()
        }
    }

    if (hasPermissions) {
        if (callState.isIncoming && callState.isRinging) {
            IncomingCallView(
                callerName = callState.callerName,
                onAccept = { callViewModel.answerCall("") }, // Technically we need SDP offer here
                onReject = { callViewModel.rejectCall() }
            )
        } else if (callState.isActive || (callState.isRinging && !callState.isIncoming)) {
            ActiveCallView(callViewModel)
        }
    }
}

@Composable
fun IncomingCallView(
    callerName: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Đang gọi đến...", color = Color.Gray, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = callerName, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FloatingActionButton(
                onClick = onReject,
                containerColor = Color.Red,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(36.dp))
            }

            FloatingActionButton(
                onClick = onAccept,
                containerColor = Color(0xFF4CAF50), // Green
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.Call, contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }
    }
}

@Composable
fun ActiveCallView(callViewModel: CallViewModel) {
    val callState by callViewModel.callState.collectAsState()
    val context = LocalContext.current
    val isVideoCall = callState.callType == "video"

    if (isVideoCall) {
        // ═══ VIDEO CALL: Camera feed ═══
        val localView = remember { SurfaceViewRenderer(context) }
        val remoteView = remember { SurfaceViewRenderer(context) }

        LaunchedEffect(Unit) {
            callViewModel.initLocalSurfaceView(localView)
            callViewModel.initRemoteSurfaceView(remoteView)
            callViewModel.startWebRtcSession()
        }

        LaunchedEffect(callState.remoteStream) {
            if (callState.remoteStream != null && callState.remoteStream!!.videoTracks.isNotEmpty()) {
                val videoTrack: VideoTrack = callState.remoteStream!!.videoTracks[0]
                videoTrack.addSink(remoteView)
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // Remote View
            AndroidView(
                factory = { remoteView },
                modifier = Modifier.fillMaxSize()
            )

            // Local View PIP
            Box(
                modifier = Modifier
                    .padding(top = 40.dp, end = 16.dp)
                    .size(width = 100.dp, height = 150.dp)
                    .align(Alignment.TopEnd)
            ) {
                AndroidView(
                    factory = { localView },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Call Controls
            CallControls(callViewModel, callState, isVideoCall = true)
        }
    } else {
        // ═══ VOICE CALL: Audio only, no camera ═══
        LaunchedEffect(Unit) {
            callViewModel.startLocalAudioOnly()
            callViewModel.startWebRtcSession()
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A2E)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Caller info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Avatar placeholder circle
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(Color(0xFF16213E), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = null,
                        tint = Color(0xFF0F3460),
                        modifier = Modifier.size(56.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = callState.callerName.ifEmpty { "Cuộc gọi thoại" },
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (callState.isActive) "Đang gọi..." else "Đang kết nối...",
                    color = Color(0xFF8E8E93),
                    fontSize = 16.sp
                )
            }

            // Call Controls
            CallControls(callViewModel, callState, isVideoCall = false)

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun CallControls(
    callViewModel: CallViewModel,
    callState: com.example.alohi.ui.viewmodel.CallState,
    isVideoCall: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mute
        IconButton(
            onClick = { callViewModel.toggleAudio() },
            modifier = Modifier.background(if (callState.isAudioEnabled) Color.DarkGray else Color.White, CircleShape).size(56.dp)
        ) {
            Icon(if (callState.isAudioEnabled) Icons.Default.Mic else Icons.Default.MicOff, contentDescription = "Mute", tint = if (callState.isAudioEnabled) Color.White else Color.Black)
        }

        // End Call
        FloatingActionButton(
            onClick = { callViewModel.endCall() },
            containerColor = Color.Red,
            shape = CircleShape,
            modifier = Modifier.size(72.dp)
        ) {
            Icon(Icons.Default.CallEnd, contentDescription = "End", tint = Color.White, modifier = Modifier.size(36.dp))
        }

        // Video Toggle (only for video calls)
        if (isVideoCall) {
            IconButton(
                onClick = { callViewModel.toggleVideo() },
                modifier = Modifier.background(if (callState.isVideoEnabled) Color.DarkGray else Color.White, CircleShape).size(56.dp)
            ) {
                Icon(if (callState.isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff, contentDescription = "Video", tint = if (callState.isVideoEnabled) Color.White else Color.Black)
            }
        } else {
            // Speaker toggle placeholder for voice calls
            Spacer(modifier = Modifier.size(56.dp))
        }
    }
}

