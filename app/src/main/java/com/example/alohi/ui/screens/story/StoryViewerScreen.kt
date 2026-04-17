package com.example.alohi.ui.screens.story

import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.alohi.data.model.StoryGroup
import com.example.alohi.data.model.StoryItem
import com.example.alohi.data.remote.ApiClient
import com.example.alohi.ui.components.AvatarImage
import com.example.alohi.ui.viewmodel.StoryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StoryViewerScreen(
    authorId: String,
    storyViewModel: StoryViewModel = viewModel(),
    currentUserId: String,
    onBack: () -> Unit
) {
    val uiState by storyViewModel.uiState.collectAsState()
    var currentStoryIndex by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }

    if (uiState.isLoading && uiState.feed == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val allGroups = (uiState.feed?.myStories ?: emptyList()) + (uiState.feed?.friendsStories ?: emptyList())
    val targetGroup = allGroups.find { it.author.id == authorId }

    if (targetGroup == null || targetGroup.stories.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val stories = targetGroup.stories
    val storyItem = stories.getOrNull(currentStoryIndex)

    if (storyItem == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    // Mark as viewed
    LaunchedEffect(storyItem.id) {
        storyViewModel.markAsViewed(storyItem.id)
    }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isVideoBuffering by remember { mutableStateOf(false) }

    // Handle Background Music
    DisposableEffect(storyItem.id) {
        if (!storyItem.music?.url.isNullOrEmpty()) {
            val rawMusicUrl = storyItem.music.url
            val finalMusicUrl = if (rawMusicUrl.startsWith("/")) {
                ApiClient.BASE_URL.replace("/api/", "") + rawMusicUrl
            } else rawMusicUrl

            mediaPlayer = MediaPlayer().apply {
                setDataSource(finalMusicUrl)
                prepareAsync()
                setOnPreparedListener {
                    if (!isPaused) start()
                }
            }
        }
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    LaunchedEffect(isPaused) {
        if (isPaused) mediaPlayer?.pause()
        else mediaPlayer?.start()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(currentStoryIndex, isPaused) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        try {
                            awaitRelease()
                        } finally {
                            isPaused = false
                        }
                    },
                    onTap = { offset ->
                        val isRightSide = offset.x > (size.width / 2)
                        if (isRightSide) {
                            if (currentStoryIndex < stories.size - 1) {
                                currentStoryIndex++
                            } else {
                                onBack() // Reached end
                            }
                        } else {
                            if (currentStoryIndex > 0) {
                                currentStoryIndex--
                            }
                        }
                    }
                )
            }
    ) {
        // Main Story Content
        if (storyItem.type == "text" && storyItem.content != null) {
            val bgColor = try { Color(android.graphics.Color.parseColor(storyItem.content.backgroundColor ?: "#FF6600")) } catch (e: Exception) { Color(0xFFFF6600) }
            val txtColor = try { Color(android.graphics.Color.parseColor(storyItem.content.textColor ?: "#FFFFFF")) } catch (e: Exception) { Color.White }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = storyItem.content.text ?: "",
                    color = txtColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            val rawUrl = storyItem.media?.url ?: storyItem.media?.thumbnailUrl ?: ""
            val finalUrl = if (rawUrl.startsWith("/")) {
                ApiClient.BASE_URL.replace("/api/", "") + rawUrl
            } else rawUrl

            if (storyItem.type == "video") {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.parse(finalUrl))
                            setOnPreparedListener { mp ->
                                isVideoBuffering = false
                                mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                                mp.isLooping = false
                                if (!isPaused) start()
                            }
                            setOnInfoListener { _, what, _ ->
                                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) isVideoBuffering = true
                                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) isVideoBuffering = false
                                true
                            }
                            // Start buffering
                            isVideoBuffering = true
                        }
                    },
                    update = { view ->
                        if (isPaused && view.isPlaying) {
                            view.pause()
                        } else if (!isPaused && !view.isPlaying && !isVideoBuffering) {
                            view.start()
                        }
                    }
                )
                if (isVideoBuffering) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            } else {
                AsyncImage(
                    model = finalUrl,
                    contentDescription = "Story",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Caption overlay
            if (!storyItem.caption.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = storyItem.caption,
                        color = Color.White,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.medium)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Progress indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            stories.forEachIndexed { index, _ ->
                StoryProgressBar(
                    modifier = Modifier.weight(1f),
                    isActive = index == currentStoryIndex,
                    isCompleted = index < currentStoryIndex,
                    isPaused = isPaused,
                    onComplete = {
                        if (currentStoryIndex < stories.size - 1) {
                            currentStoryIndex++
                        } else {
                            onBack()
                        }
                    }
                )
            }
        }

        // Header (Avatar + Name + Close)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 64.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarImage(imageUrl = targetGroup.author.avatar?.url ?: "", name = targetGroup.author.displayName ?: "User", size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = targetGroup.author.displayName ?: "User",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = storyItem.createdAt?.take(10) ?: "", // Simplify date
                color = Color.LightGray,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }
        }

        // Footer Actions (React / View Count / Delete if own)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (targetGroup.author.id == currentUserId) {
                Text(text = "👀 ${storyItem.viewCount ?: 0} lượt xem", color = Color.White)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    coroutineScope.launch {
                        // repository.deleteStory(storyItem.id) // Call repository
                        onBack()
                    }
                }) {
                    Icon(Icons.Default.Delete, "Delete Story", tint = Color.Red)
                }
            } else {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text("Gửi tin nhắn...", color = Color.LightGray) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.LightGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = CircleShape
                )
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = { /* Handle React */ }) {
                    Icon(Icons.Default.Favorite, "React", tint = Color.Red, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
fun StoryProgressBar(
    modifier: Modifier = Modifier,
    isActive: Boolean,
    isCompleted: Boolean,
    isPaused: Boolean,
    onComplete: () -> Unit
) {
    val progress = remember { Animatable(if (isCompleted) 1f else 0f) }

    LaunchedEffect(isActive, isPaused) {
        if (isActive) {
            if (!isPaused) {
                val remainingTime = (1f - progress.value) * 5000 // 5 seconds per story
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(remainingTime.toInt(), easing = LinearEasing)
                )
                onComplete()
            } else {
                progress.stop()
            }
        } else {
            progress.snapTo(if (isCompleted) 1f else 0f)
        }
    }

    LinearProgressIndicator(
        progress = { progress.value },
        modifier = modifier
            .height(2.dp)
            .clip(MaterialTheme.shapes.small),
        color = Color.White,
        trackColor = Color.White.copy(alpha = 0.3f),
        drawStopIndicator = {}
    )
}
