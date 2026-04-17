package com.example.alohi.ui.screens.chatlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.alohi.data.model.ConversationItem
import com.example.alohi.data.model.FriendItem
import com.example.alohi.data.model.UserProfile
import com.example.alohi.ui.components.ChatListItem
import com.example.alohi.ui.components.StoryCircle
import com.example.alohi.ui.theme.AloHiTheme
import com.example.alohi.ui.viewmodel.MainViewModel

/**
 * AloHi Chat List Screen (Tab 1) — REAL API DATA ONLY
 * Features:
 * - Gradient header with search bar
 * - Online friends story row
 * - Real conversations from API
 * - Empty state when no conversations
 * - Search overlay connected to real search API
 */

@Composable
fun ChatListScreen(
    conversations: List<ConversationItem> = emptyList(),
    isLoading: Boolean = false,
    currentUserId: String? = null,
    onlineFriends: List<FriendItem> = emptyList(),
    mainViewModel: MainViewModel,
    onChatClick: (String, String) -> Unit,
    onCreateGroupClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    val colors = AloHiTheme.extendedColors
    var showSearch by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // ═══════════════════════════════════════════
            // GRADIENT HEADER
            // ═══════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(colors.gradientStart, colors.gradientEnd)
                        )
                    )
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AloHi",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Quét QR",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = { onCreateGroupClick() }) {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = "Tạo nhóm",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Search bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { showSearch = true },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Tìm kiếm",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tìm kiếm",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════
            // CONTENT
            // ═══════════════════════════════════════════
            if (isLoading && conversations.isEmpty()) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    // ── Online Friends Story Row ──
                    if (onlineFriends.isNotEmpty()) {
                        item {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(vertical = 14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(onlineFriends) { friend ->
                                    StoryCircle(
                                        name = friend.displayName,
                                        isAddStory = false,
                                        hasSeen = false,
                                        storyCount = 1,
                                    )
                                }
                            }
                        }

                        item {
                            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 6.dp)
                        }
                    }

                    // ── Conversation List ──
                    if (conversations.isEmpty()) {
                        // Empty state
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 80.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Create,
                                    contentDescription = null,
                                    tint = Color(0xFFD1D1D6),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Chưa có cuộc trò chuyện",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Bắt đầu trò chuyện bằng cách\ntìm kiếm bạn bè ở trên",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textTertiary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(conversations) { convo ->
                            val myParticipant = convo.participants?.firstOrNull {
                                it.user?.id == currentUserId
                            }
                            val otherUser = convo.participants?.firstOrNull {
                                it.user?.id != currentUserId
                            }?.user
                            val displayName = if (convo.type == "group") {
                                convo.group?.name ?: "Nhóm"
                            } else {
                                otherUser?.displayName ?: "Người dùng"
                            }
                            val lastMsgSenderId = convo.lastMessage?.sender?.id
                            val isMyLastMsg = lastMsgSenderId == currentUserId
                            val senderPrefix = when {
                                isMyLastMsg -> "Bạn: "
                                convo.type == "group" && convo.lastMessage?.sender != null ->
                                    "${convo.lastMessage.sender.displayName}: "
                                else -> null
                            }
                            val rawPreview = convo.lastMessage?.preview ?: ""
                            val lastMsg = if (convo.lastMessage?.type == "call") {
                                val isVideo = rawPreview.contains("\"type\": \"video\"") || rawPreview.contains("\"type\":\"video\"")
                                val isMissed = rawPreview.contains("\"isMissed\": true") || rawPreview.contains("\"isMissed\":true")
                                if (isVideo) {
                                    if (isMissed) "Cuộc gọi video nhỡ" else "Cuộc gọi video"
                                } else {
                                    if (isMissed) "Cuộc gọi thoại nhỡ" else "Cuộc gọi thoại"
                                }
                            } else {
                                rawPreview.takeIf { it.isNotBlank() }
                                    ?: when (convo.lastMessage?.type) {
                                        "image" -> "Hình ảnh"
                                        "video" -> "Video"
                                        "file" -> "Tệp đính kèm"
                                        "sticker" -> "Sticker"
                                        "audio" -> "Tin nhắn thoại"
                                        else -> "Tin nhắn mới"
                                    }
                            }
                            
                            val lastMsgIcon = when (convo.lastMessage?.type) {
                                "call" -> {
                                    val isVideo = rawPreview.contains("\"type\": \"video\"") || rawPreview.contains("\"type\":\"video\"")
                                    if (isVideo) Icons.Default.Videocam else Icons.Default.Call
                                }
                                "image" -> Icons.Default.Photo
                                "video" -> Icons.Default.SmartDisplay
                                "file" -> Icons.Default.AttachFile
                                "audio" -> Icons.Default.Mic
                                "sticker" -> Icons.Default.EmojiEmotions
                                else -> null
                            }
                            
                            val timestamp = formatTimestamp(convo.lastMessage?.timestamp ?: convo.updatedAt)
                            val unread = myParticipant?.unreadCount ?: 0
                            val avatarUrl = if (convo.type == "group") {
                                convo.group?.avatar?.url
                            } else {
                                otherUser?.avatar?.url
                            }

                            ChatListItem(
                                name = displayName,
                                lastMessage = lastMsg,
                                timestamp = timestamp,
                                onClick = { onChatClick(convo.id, displayName) },
                                avatarUrl = avatarUrl,
                                isOnline = otherUser?.isOnline == true,
                                unreadCount = unread,
                                isGroup = convo.type == "group",
                                senderPrefix = senderPrefix,
                                lastMessageIcon = lastMsgIcon,
                                modifier = Modifier.background(Color.White)
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 84.dp),
                                color = Color(0xFFF0F0F0),
                                thickness = 0.5.dp
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }

        // ═══════════════════════════════════════════
        // SEARCH OVERLAY — connected to real API
        // ═══════════════════════════════════════════
        AnimatedVisibility(
            visible = showSearch,
            enter = fadeIn() + slideInVertically { -it / 3 },
            exit = fadeOut() + slideOutVertically { -it / 3 }
        ) {
            SearchOverlay(
                mainViewModel = mainViewModel,
                onDismiss = { showSearch = false },
                onResultClick = { userId, name ->
                    showSearch = false
                    // Must create/find conversation first — userId is NOT a conversationId
                    mainViewModel.createConversation(userId) { conversationId ->
                        onChatClick(conversationId, name)
                    }
                }
            )
        }
    }
}

/** Format ISO timestamp to relative display like Zalo */
private fun formatTimestamp(iso: String?): String {
    if (iso == null) return ""
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = sdf.parse(iso.substringBefore(".").substringBefore("Z")) ?: return ""
        val now = java.util.Date()
        val diffMs = now.time - date.time
        val diffMin = diffMs / 60000
        val diffHour = diffMin / 60

        val localSdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
        localSdf.timeZone = java.util.TimeZone.getDefault()

        val calNow = java.util.Calendar.getInstance().apply { time = now }
        val calDate = java.util.Calendar.getInstance().apply { time = date }

        when {
            diffMin < 1 -> "Vừa xong"
            diffMin < 60 -> "${diffMin} phút"
            calNow.get(java.util.Calendar.DAY_OF_YEAR) == calDate.get(java.util.Calendar.DAY_OF_YEAR) &&
                calNow.get(java.util.Calendar.YEAR) == calDate.get(java.util.Calendar.YEAR) -> localSdf.format(date)
            diffHour < 48 -> "Hôm qua"
            else -> {
                val dateSdf = java.text.SimpleDateFormat("dd/MM", java.util.Locale.US)
                dateSdf.timeZone = java.util.TimeZone.getDefault()
                dateSdf.format(date)
            }
        }
    } catch (e: Exception) {
        ""
    }
}
