package com.example.alohi.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import com.example.alohi.utils.AudioPlayerManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.alohi.ui.theme.AloHiTheme
import com.example.alohi.ui.theme.ReceiverBubbleShape
import com.example.alohi.ui.theme.ReceiverBubbleFirstShape
import com.example.alohi.ui.theme.ReceiverBubbleMiddleShape
import com.example.alohi.ui.theme.ReceiverBubbleLastShape
import com.example.alohi.ui.theme.SenderBubbleShape
import com.example.alohi.ui.theme.SenderBubbleFirstShape
import com.example.alohi.ui.theme.SenderBubbleMiddleShape
import com.example.alohi.ui.theme.SenderBubbleLastShape

/**
 * AloHi Chat Bubble — Premium Zalo/iMessage Style
 *
 * Images/videos: no colored background, timestamp overlay on image
 * Text/file/call: standard bubble with color
 */

enum class MessageStatus {
    SENDING, SENT, DELIVERED, READ
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    message: String,
    timestamp: String,
    isFromMe: Boolean,
    modifier: Modifier = Modifier,
    status: MessageStatus = MessageStatus.READ,
    senderName: String? = null,
    isFirstInGroup: Boolean = true,
    isLastInGroup: Boolean = true,
    messageType: String = "text",
    replyToSenderName: String? = null,
    replyToContent: String? = null,
    reactions: List<Pair<String, Int>>? = null, // emoji to count
    isForwarded: Boolean = false,
    onImageClick: ((String) -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
) {
    val colors = AloHiTheme.extendedColors
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxBubbleWidth = screenWidth * 0.75f

    val isMediaType = messageType == "image" || messageType == "video"

    // Images/videos: transparent bubble, no colored background
    val bubbleColor = when {
        isMediaType -> Color.Transparent
        isFromMe -> colors.bubbleSender
        else -> colors.bubbleReceiver
    }
    val textColor = if (isFromMe) colors.bubbleTextSender else colors.bubbleTextReceiver

    // 3-tier shape selection
    val bubbleShape = when {
        isFirstInGroup && isLastInGroup -> if (isFromMe) SenderBubbleShape else ReceiverBubbleShape
        isFirstInGroup -> if (isFromMe) SenderBubbleFirstShape else ReceiverBubbleFirstShape
        isLastInGroup -> if (isFromMe) SenderBubbleLastShape else ReceiverBubbleLastShape
        else -> if (isFromMe) SenderBubbleMiddleShape else ReceiverBubbleMiddleShape
    }

    val alignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val topPadding = if (isFirstInGroup) 6.dp else 1.5.dp
    val bottomPadding = if (isLastInGroup) 2.dp else 0.5.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (isFromMe) 56.dp else 12.dp,
                end = if (isFromMe) 12.dp else 56.dp,
                top = topPadding,
                bottom = bottomPadding
            ),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxBubbleWidth)
                .animateContentSize(animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                ))
                .then(
                    if (isMediaType) {
                        // No clip/background for media — image handles its own clipping
                        Modifier
                    } else {
                        Modifier
                            .clip(bubbleShape)
                            .background(bubbleColor)
                    }
                )
                .combinedClickable(
                    onClick = {},
                    onLongClick = { onLongPress?.invoke() }
                )
                .padding(
                    start = if (isMediaType) 0.dp else 12.dp,
                    end = if (isMediaType) 0.dp else 12.dp,
                    top = if (isMediaType) 0.dp
                    else if (senderName != null && !isFromMe && isFirstInGroup) 6.dp else 8.dp,
                    bottom = if (isMediaType) 0.dp else 6.dp
                )
        ) {
            // Sender name (for group chats)
            if (senderName != null && !isFromMe && isFirstInGroup) {
                Text(
                    text = senderName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = if (isMediaType) 4.dp else 0.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Forwarded label
            if (isForwarded) {
                Text(
                    text = "↪ Tin nhắn chuyển tiếp",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFromMe) Color.White.copy(alpha = 0.7f) else Color(0xFF8E8E93),
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            // Reply quote
            if (replyToSenderName != null && replyToContent != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isFromMe) Color.White.copy(alpha = 0.15f)
                            else Color.Black.copy(alpha = 0.05f)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(30.dp)
                            .background(
                                if (isFromMe) Color.White.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(2.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = replyToSenderName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isFromMe) Color.White.copy(alpha = 0.8f)
                                    else MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                        )
                        Text(
                            text = replyToContent,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isFromMe) Color.White.copy(alpha = 0.6f)
                                    else Color(0xFF8E8E93),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // ── Message content based on type ──
            when (messageType) {
                "image" -> {
                    // Image: no bubble, just the image with rounded corners + timestamp overlay
                    val serverUrl = com.example.alohi.data.remote.ApiClient.BASE_URL.removeSuffix("api/")
                    val isRemoteRelative = !message.startsWith("http") && !message.startsWith("content://") && !message.startsWith("file://")
                    val imageUrl = if (isRemoteRelative) {
                        if (message.startsWith("/")) serverUrl + message.removePrefix("/") else serverUrl + message
                    } else {
                        message
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Ảnh",
                            modifier = Modifier
                                .widthIn(max = maxBubbleWidth)
                                .heightIn(max = 300.dp, min = 80.dp)
                                .clickable { onImageClick?.invoke(imageUrl) },
                            contentScale = ContentScale.FillWidth,
                        )
                        // Loading overlay
                        if (status == MessageStatus.SENDING) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        // Timestamp overlay on image — bottom right pill
                        if (isLastInGroup) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .background(
                                        Color.Black.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = timestamp,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                                if (isFromMe) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    MessageStatusIcon(status = status, isFromMe = true)
                                }
                            }
                        }
                    }
                }

                "video" -> {
                    // Video: no bubble, just video thumbnail + play button + timestamp overlay
                    val serverUrl = com.example.alohi.data.remote.ApiClient.BASE_URL.removeSuffix("api/")
                    val isRemoteRelative = !message.startsWith("http") && !message.startsWith("content://") && !message.startsWith("file://")
                    val videoUrl = if (isRemoteRelative) {
                        if (message.startsWith("/")) serverUrl + message.removePrefix("/") else serverUrl + message
                    } else {
                        message
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = videoUrl,
                            contentDescription = "Video",
                            modifier = Modifier
                                .widthIn(max = maxBubbleWidth)
                                .heightIn(max = 300.dp, min = 80.dp),
                            contentScale = ContentScale.FillWidth,
                        )
                        // Loading or play button
                        if (status == MessageStatus.SENDING) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayCircleFilled,
                                contentDescription = "Phát video",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        // Timestamp overlay
                        if (isLastInGroup) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .background(
                                        Color.Black.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = timestamp,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                                if (isFromMe) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    MessageStatusIcon(status = status, isFromMe = true)
                                }
                            }
                    }
                }
            }

                "audio" -> {
                    // Audio Playback UI using AudioPlayerManager
                    val serverUrl = com.example.alohi.data.remote.ApiClient.BASE_URL.removeSuffix("api/")
                    val isRemoteRelative = !message.startsWith("http") && !message.startsWith("content://") && !message.startsWith("file://")
                    val audioUrl = if (isRemoteRelative) {
                        if (message.startsWith("/")) serverUrl + message.removePrefix("/") else serverUrl + message
                    } else {
                        message
                    }
                    
                    val playbackState by AudioPlayerManager.playbackState.collectAsState()
                    val isMyAudio = playbackState.url == audioUrl
                    val isPlaying = isMyAudio && playbackState.isPlaying
                    
                    val displaySecs = if (isMyAudio && playbackState.durationMs > 0) {
                        (playbackState.durationMs - playbackState.currentPositionMs) / 1000
                    } else {
                        0
                    }
                    val mm = (displaySecs / 60).toString().padStart(2, '0')
                    val ss = (displaySecs % 60).toString().padStart(2, '0')
                    val timeText = if (isMyAudio && playbackState.durationMs > 0) "$mm:$ss" else "Voice"

                    LaunchedEffect(isPlaying) {
                        while (isPlaying) {
                            AudioPlayerManager.updateProgressIfPlaying()
                            delay(50)
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                                AudioPlayerManager.togglePlayPause(audioUrl)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                            contentDescription = "Phát ghi âm",
                            tint = if (isFromMe) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Waveform progress
                        Row(modifier = Modifier.weight(1f, fill = false), verticalAlignment = Alignment.CenterVertically) {
                            repeat(8) { idx ->
                                val isActive = if (isMyAudio && playbackState.progress > 0) {
                                    val prog = playbackState.progress * 8
                                    idx <= prog
                                } else false
                                
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 1.dp)
                                        .width(3.dp)
                                        .height(if (idx % 2 == 0) 14.dp else 8.dp)
                                        .background(
                                            if (isActive) (if (isFromMe) Color.White else MaterialTheme.colorScheme.primary)
                                            else (if (isFromMe) Color.White.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.5f)),
                                            CircleShape
                                        )
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }
                }

                "file" -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = "File",
                            tint = if (isFromMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = message.substringAfterLast("/").take(30),
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                            )
                            Text(
                                text = "Tệp đính kèm",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isFromMe) Color.White.copy(alpha = 0.6f) else Color(0xFF8E8E93)
                            )
                        }
                    }
                }

                "call" -> {
                    val json = remember(message) {
                        try { org.json.JSONObject(message) } catch (e: Exception) { null }
                    }
                    val isMissed = json?.optBoolean("isMissed", false) ?: false
                    val callType = json?.optString("type", "audio") ?: "audio"
                    val durationStr = json?.optInt("duration", 0)?.let {
                        if (it > 0) "${it / 60} phút ${it % 60} giây" else ""
                    } ?: ""

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isMissed -> Icons.Default.PhoneMissed
                                callType == "video" -> Icons.Default.Videocam
                                else -> Icons.Default.Phone
                            },
                            contentDescription = "Call",
                            tint = if (isMissed) Color.Red
                            else if (isFromMe) Color.White.copy(alpha = 0.8f)
                            else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (callType == "video") "Cuộc gọi Video" else "Cuộc gọi Thoại",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isMissed) Color.Red else textColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            val statusText = when {
                                isMissed && isFromMe -> "Đã gọi (không bắt máy)"
                                isMissed && !isFromMe -> "Cuộc gọi nhỡ"
                                durationStr.isNotEmpty() -> durationStr
                                else -> "Kết nối thất bại"
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isFromMe) Color.White.copy(alpha = 0.7f) else Color(0xFF8E8E93)
                            )
                        }
                    }
                }

                else -> {
                    // Text message (default)
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                }
            }

            // Timestamp + Status row — ONLY for non-media types (media has overlay)
            if (isLastInGroup && !isMediaType) {
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFromMe) Color.White.copy(alpha = 0.7f) else Color(0xFF8E8E93)
                    )
                    if (isFromMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageStatusIcon(status = status, isFromMe = true)
                    }
                }
            }

            // Emoji reactions row
            if (!reactions.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(
                            color = Color(0xFFF0F0F0),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    reactions.forEach { (emoji, count) ->
                        Text(
                            text = if (count > 1) "$emoji $count" else emoji,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageStatusIcon(
    status: MessageStatus,
    isFromMe: Boolean,
    modifier: Modifier = Modifier
) {
    val icon = when (status) {
        MessageStatus.SENDING -> Icons.Default.Done
        MessageStatus.SENT -> Icons.Default.Done
        MessageStatus.DELIVERED -> Icons.Default.DoneAll
        MessageStatus.READ -> Icons.Default.DoneAll
    }
    val iconTint = when {
        status == MessageStatus.SENDING -> Color.White.copy(alpha = 0.35f)
        isFromMe && status == MessageStatus.READ -> Color.White
        isFromMe -> Color.White.copy(alpha = 0.6f)
        else -> Color(0xFF8E8E93)
    }
    Icon(
        imageVector = icon,
        contentDescription = status.name,
        modifier = modifier.size(15.dp),
        tint = iconTint
    )
}
