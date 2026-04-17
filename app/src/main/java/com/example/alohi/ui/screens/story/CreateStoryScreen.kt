package com.example.alohi.ui.screens.story

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicVideo
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.alohi.ui.viewmodel.StoryViewModel
import com.example.alohi.utils.FileUtils
import java.io.File
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import android.widget.Toast

@Composable
fun CreateStoryScreen(
    storyViewModel: StoryViewModel = viewModel(),
    onBack: () -> Unit
) {
    val uiState by storyViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMusicUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    var isVideo by remember { mutableStateOf(false) }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> 
        uri?.let { 
            selectedUri = it 
            val mimeType = context.contentResolver.getType(it)
            isVideo = mimeType?.startsWith("video") == true
        } 
    }

    val musicPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> 
        uri?.let { selectedMusicUri = it }
    }

    LaunchedEffect(Unit) {
        mediaPicker.launch("*/*")
    }

    LaunchedEffect(uiState.uploadSuccess, uiState.error) {
        if (uiState.uploadSuccess) {
            Toast.makeText(context, "Đăng tin thành công!", Toast.LENGTH_SHORT).show()
            storyViewModel.resetUploadState()
            onBack()
        }
        if (!uiState.error.isNullOrEmpty()) {
            Toast.makeText(context, "Lỗi: ${uiState.error}", Toast.LENGTH_LONG).show()
            storyViewModel.resetUploadState()
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, "Cancel", tint = Color.White)
                }
            }
        },
        floatingActionButton = {
            if (selectedUri != null) {
                FloatingActionButton(
                    onClick = {
                        val file = FileUtils.getFileFromUri(context, selectedUri!!)
                        if (file != null) {
                            val type = if (isVideo) "video" else "image"
                            val musicFile = selectedMusicUri?.let { FileUtils.getFileFromUri(context, it) }
                            storyViewModel.createStory(file, type, caption, musicFile)
                        } else {
                            Toast.makeText(context, "Không thể lấy tệp", Toast.LENGTH_SHORT).show()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    if (uiState.isUploading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Đăng", tint = Color.White)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedUri == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = { mediaPicker.launch("*/*") },
                        modifier = Modifier.size(64.dp).background(Color.DarkGray, CircleShape)
                    ) {
                        Icon(Icons.Default.Image, "Chọn tệp", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Nhấn để chọn ảnh hoặc video", color = Color.White)
                }
            } else {
                if (isVideo) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            android.widget.VideoView(ctx).apply {
                                setVideoURI(selectedUri)
                                setOnPreparedListener { mp ->
                                    mp.setVideoScalingMode(android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                                    mp.isLooping = true
                                    start()
                                }
                            }
                        }
                    )
                } else {
                    AsyncImage(
                        model = selectedUri,
                        contentDescription = "Story Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Music picker button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp, end = 16.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    IconButton(
                        onClick = { musicPicker.launch("audio/*") },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (selectedMusicUri != null) Icons.Default.MusicVideo else Icons.Default.LibraryMusic,
                            contentDescription = "Chọn nhạc",
                            tint = if (selectedMusicUri != null) Color.Green else Color.White
                        )
                    }
                }

                // Caption input overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        placeholder = { Text("Thêm chú thích...", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}
