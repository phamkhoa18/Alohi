package com.example.alohi.ui.screens.conversation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.alohi.data.model.MessageItem
import com.example.alohi.data.remote.ApiClient
import com.example.alohi.ui.components.AvatarImage
import com.example.alohi.ui.components.ChatBubble
import com.example.alohi.ui.components.ImageViewerDialog
import com.example.alohi.ui.components.MessageComposer
import com.example.alohi.ui.components.MessageStatus
import com.example.alohi.ui.theme.AloHiTheme
import com.example.alohi.ui.viewmodel.MainViewModel
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * AloHi Conversation Screen — Premium Zalo/iMessage Quality
 *
 * Features:
 * - Instant load from Room cache (zero spinner for cached conversations)
 * - Message entrance animations (fade + slide-up like iMessage)
 * - Proper date grouping separators
 * - Sender grouping (consecutive messages from same user)
 * - Multi-image grid display (Zalo-style)
 * - Long-press context menu with Material 3 ModalBottomSheet
 * - Full-screen image viewer with pinch-to-zoom
 * - Recall/delete/copy message actions
 * - Typing indicator with animated bouncing dots
 * - Scroll-to-bottom FAB when scrolled away
 */

// ═══════════════════════════════════════════════════════
// DISPLAY ITEM — Supports single messages + image grids
// ═══════════════════════════════════════════════════════

private sealed class DisplayItem {
    abstract val key: String

    data class Single(val info: MessageGroupInfo, val index: Int) : DisplayItem() {
        override val key = info.message.id ?: info.message.messageId ?: "s_$index"
    }

    data class ImageGrid(
        val infos: List<MessageGroupInfo>,
        val isFromMe: Boolean,
        val firstIndex: Int,
    ) : DisplayItem() {
        override val key = "grid_${infos.first().message.id ?: infos.first().message.messageId ?: firstIndex}"
    }
}

/** Data class for message grouping info */
private data class MessageGroupInfo(
    val message: MessageItem,
    val isFirstInGroup: Boolean,
    val isLastInGroup: Boolean,
    val showDateSeparator: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ConversationScreen(
    conversationId: String,
    partnerName: String,
    mainViewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onCallClick: () -> Unit = {},
    onVideoCallClick: () -> Unit = {},
    onNavigateToDetail: () -> Unit = {},
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val colors = AloHiTheme.extendedColors
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ── Panel state ──
    var activePanel by remember { mutableStateOf(ChatPanel.NONE) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val uiState by mainViewModel.uiState.collectAsState()
    val messages = uiState.messages
    val typingUser = uiState.typingUser

    // ── Image viewer state ──
    var viewingImageUrl by remember { mutableStateOf<String?>(null) }

    // ── Context menu state ──
    var selectedMessage by remember { mutableStateOf<MessageItem?>(null) }
    var showMessageMenu by remember { mutableStateOf(false) }
    var showForwardDialog by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()

    // ── Track previous message count for animation direction ──
    var prevMessageCount by remember { mutableStateOf(messages.size) }

    // ── Scroll-to-bottom FAB state ──
    val isAtBottom by remember {
        derivedStateOf {
            val firstVisibleItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            firstVisibleItem != null && firstVisibleItem.index <= 2
        }
    }

    // ── Derive partner online status from conversations data ──
    val currentConversation = uiState.conversations.firstOrNull { it.id == conversationId }
    val isGroupChat = currentConversation?.type == "group"
    val partnerParticipant = currentConversation?.participants?.firstOrNull {
        it.user?.id != uiState.currentUser?.id
    }
    val isPartnerOnline = partnerParticipant?.user?.isOnline == true
    val partnerLastSeen = partnerParticipant?.user?.lastSeen
    val groupMemberCount = if (isGroupChat) currentConversation?.participants?.size ?: 0 else 0

    // ── Media pickers ──
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                mainViewModel.sendImageMessage(context, conversationId, uri)
            }
            activePanel = ChatPanel.NONE
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                mainViewModel.sendVideoMessage(context, conversationId, uri)
            }
            activePanel = ChatPanel.NONE
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                mainViewModel.sendFileMessage(context, conversationId, uri)
            }
            activePanel = ChatPanel.NONE
        }
    }

    // ── Load messages + mark as read on enter ──
    LaunchedEffect(conversationId) {
        mainViewModel.loadMessages(conversationId)
    }

    // Mark as read once messages are loaded
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            mainViewModel.markConversationAsRead(conversationId)
        }
    }

    // ── Clean up when leaving conversation ──
    DisposableEffect(conversationId) {
        onDispose {
            mainViewModel.leaveConversation()
        }
    }

    // ── Toggle panel helper ──
    fun togglePanel(panel: ChatPanel) {
        if (activePanel == panel) {
            activePanel = ChatPanel.NONE
        } else {
            keyboardController?.hide()
            focusManager.clearFocus()
            activePanel = panel
        }
    }

    // ── Group messages by date for date separator rendering ──
    val groupedMessages = remember(messages) {
        messages.mapIndexed { index, msg ->
            val prevMsg = messages.getOrNull(index - 1)
            val nextMsg = messages.getOrNull(index + 1)
            val isSameSenderAsPrev = prevMsg?.sender?.id == msg.sender?.id
            val isSameSenderAsNext = nextMsg?.sender?.id == msg.sender?.id
            val showDateSeparator = if (index == 0) {
                true
            } else {
                val curDate = msg.createdAt?.substringBefore("T") ?: ""
                val prevDate = prevMsg?.createdAt?.substringBefore("T") ?: ""
                curDate != prevDate
            }
            MessageGroupInfo(
                message = msg,
                isFirstInGroup = !isSameSenderAsPrev || showDateSeparator,
                isLastInGroup = !isSameSenderAsNext,
                showDateSeparator = showDateSeparator,
            )
        }
    }

    // ── Build display items: group consecutive images into grids ──
    // Only group images sent within 30 seconds of each other (same batch)
    val currentUserId = uiState.currentUser?.id
    val displayItems = remember(groupedMessages, currentUserId) {
        buildList {
            var i = 0
            while (i < groupedMessages.size) {
                val info = groupedMessages[i]
                if (info.message.type == "image") {
                    val senderId = info.message.sender?.id
                    val images = mutableListOf(info)
                    var j = i + 1
                    while (j < groupedMessages.size &&
                        groupedMessages[j].message.type == "image" &&
                        groupedMessages[j].message.sender?.id == senderId &&
                        !groupedMessages[j].showDateSeparator &&
                        isWithinTimeWindow(
                            groupedMessages[j - 1].message.createdAt,
                            groupedMessages[j].message.createdAt,
                            30_000L // 30 seconds — only batch-sent images group together
                        )
                    ) {
                        images.add(groupedMessages[j])
                        j++
                    }
                    if (images.size > 1) {
                        add(DisplayItem.ImageGrid(images, senderId == currentUserId, i))
                    } else {
                        add(DisplayItem.Single(info, i))
                    }
                    i = j
                } else {
                    add(DisplayItem.Single(info, i))
                    i++
                }
            }
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            if (messages.size > prevMessageCount && isAtBottom) {
                delay(50)
                listState.animateScrollToItem(0)
            }
            prevMessageCount = messages.size
        }
    }

    // ═══════════════════════════════════════════════════════
    // IMAGE VIEWER DIALOG
    // ═══════════════════════════════════════════════════════
    if (viewingImageUrl != null) {
        ImageViewerDialog(
            imageUrl = viewingImageUrl!!,
            onDismiss = { viewingImageUrl = null }
        )
    }

    // ═══════════════════════════════════════════════════════
    // MESSAGE CONTEXT MENU — Material 3 ModalBottomSheet
    // ═══════════════════════════════════════════════════════
    if (showMessageMenu && selectedMessage != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showMessageMenu = false
                selectedMessage = null
            },
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            val msg = selectedMessage!!
            val isMyMessage = msg.sender?.id == currentUserId

            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                // ── Quick Emoji Reaction Row ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val quickEmojis = listOf("❤️", "😂", "😮", "😢", "😡", "👍")
                    quickEmojis.forEach { emoji ->
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier
                                .clickable {
                                    val msgId = msg.messageId ?: msg.id
                                    if (msgId != null) {
                                        mainViewModel.addReaction(msgId, emoji)
                                    }
                                    showMessageMenu = false
                                    selectedMessage = null
                                }
                                .padding(4.dp)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))

                // Reply
                ListItem(
                    headlineContent = { Text("Trả lời") },
                    leadingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable {
                        mainViewModel.setReplyingTo(msg)
                        showMessageMenu = false
                        selectedMessage = null
                    }
                )

                // Forward
                ListItem(
                    headlineContent = { Text("Chuyển tiếp") },
                    leadingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.clickable {
                        showMessageMenu = false
                        showForwardDialog = true
                    }
                )

                // Copy (text messages only)
                if (msg.type == "text" || msg.type == null) {
                    ListItem(
                        headlineContent = { Text("Sao chép") },
                        leadingContent = {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.clickable {
                            mainViewModel.copyMessageToClipboard(context, msg.displayText())
                            showMessageMenu = false
                            selectedMessage = null
                        }
                    )
                }

                // Share
                ListItem(
                    headlineContent = { Text("Chia sẻ") },
                    leadingContent = {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.clickable {
                        showMessageMenu = false
                        selectedMessage = null
                    }
                )

                // Recall (only sender, within 24h)
                if (isMyMessage) {
                    ListItem(
                        headlineContent = {
                            Text("Thu hồi", color = Color(0xFFFF9500))
                        },
                        leadingContent = {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color(0xFFFF9500)
                            )
                        },
                        modifier = Modifier.clickable {
                            val msgId = msg.messageId ?: msg.id
                            if (msgId != null) {
                                mainViewModel.recallMessage(msgId, conversationId)
                            }
                            showMessageMenu = false
                            selectedMessage = null
                        }
                    )
                }

                // Delete for self
                ListItem(
                    headlineContent = {
                        Text("Xóa phía tôi", color = Color(0xFFFF3B30))
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFFF3B30)
                        )
                    },
                    modifier = Modifier.clickable {
                        val msgId = msg.messageId ?: msg.id
                        if (msgId != null) {
                            mainViewModel.deleteMessageForSelf(msgId)
                        }
                        showMessageMenu = false
                        selectedMessage = null
                    }
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    // FORWARD DIALOG — pick conversations to forward to
    // ═══════════════════════════════════════════════════════
    if (showForwardDialog && selectedMessage != null) {
        val forwardMsg = selectedMessage!!
        val convos = uiState.conversations
        val selectedConvoIds = remember { mutableStateListOf<String>() }

        AlertDialog(
            onDismissRequest = {
                showForwardDialog = false
                selectedMessage = null
            },
            title = { Text("Chuyển tiếp đến", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.height(350.dp)
                ) {
                    items(convos.filter { it.id != conversationId }) { convo ->
                        val convoName = if (convo.type == "group") {
                            convo.group?.name ?: "Nhóm"
                        } else {
                            convo.participants?.firstOrNull { it.user?.id != currentUserId }?.user?.displayName ?: "?"
                        }
                        val isSelected = selectedConvoIds.contains(convo.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) selectedConvoIds.remove(convo.id)
                                    else selectedConvoIds.add(convo.id)
                                }
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    if (isSelected) selectedConvoIds.remove(convo.id)
                                    else selectedConvoIds.add(convo.id)
                                },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = convoName,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val msgId = forwardMsg.messageId ?: forwardMsg.id
                        if (msgId != null && selectedConvoIds.isNotEmpty()) {
                            mainViewModel.forwardMessage(msgId, selectedConvoIds.toList())
                        }
                        showForwardDialog = false
                        selectedMessage = null
                    },
                    enabled = selectedConvoIds.isNotEmpty()
                ) {
                    Text("Gửi (${selectedConvoIds.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showForwardDialog = false
                    selectedMessage = null
                }) {
                    Text("Hủy")
                }
            }
        )
    }

    // ═══════════════════════════════════════════════════════
    // MAIN SCAFFOLD
    // ═══════════════════════════════════════════════════════
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarImage(
                                name = partnerName,
                                size = 38.dp,
                                showOnlineIndicator = true,
                                isOnline = isPartnerOnline
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = partnerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                // Typing indicator or online/group status
                                if (typingUser != null) {
                                    Text(
                                        text = if (isGroupChat) "$typingUser đang nhập..." else "đang nhập...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else if (isGroupChat) {
                                    Text(
                                        text = "$groupMemberCount thành viên",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF8E8E93)
                                    )
                                } else if (isPartnerOnline) {
                                    Text(
                                        text = "Đang hoạt động",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.online
                                    )
                                } else {
                                    val lastSeenText = partnerLastSeen?.let {
                                        try {
                                            val timePart = it.substringAfter("T").take(5)
                                            "Truy cập lúc $timePart"
                                        } catch (e: Exception) { "Offline" }
                                    } ?: "Offline"
                                    Text(
                                        text = lastSeenText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF8E8E93)
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay lại",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        val partnerId = partnerParticipant?.user?.id ?: ""
                        if (partnerId.isNotEmpty()) {
                            val voiceBtnRef = remember { java.util.concurrent.atomic.AtomicReference<android.view.View>() }
                            Box(contentAlignment = Alignment.Center) {
                                IconButton(onClick = {
                                    val payload = """{"isMissed": true, "type": "audio", "duration": 0}"""
                                    mainViewModel.sendMessage(conversationId, payload, "call")
                                    voiceBtnRef.get()?.performClick()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Gọi thoại",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                androidx.compose.ui.viewinterop.AndroidView<android.view.View>(
                                    modifier = Modifier.size(1.dp).alpha(0f),
                                    factory = { context ->
                                        com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton(context).apply {
                                            setIsVideoCall(false)
                                            resourceID = com.example.alohi.utils.ZegoConfig.RESOURCE_ID
                                            setInvitees(
                                                java.util.Collections.singletonList(
                                                    com.zegocloud.uikit.service.defines.ZegoUIKitUser(partnerId, partnerName)
                                                )
                                            )
                                            voiceBtnRef.set(this)
                                        }
                                    }
                                )
                            }

                            val videoBtnRef = remember { java.util.concurrent.atomic.AtomicReference<android.view.View>() }
                            Box(contentAlignment = Alignment.Center) {
                                IconButton(onClick = {
                                    val payload = """{"isMissed": true, "type": "video", "duration": 0}"""
                                    mainViewModel.sendMessage(conversationId, payload, "call")
                                    videoBtnRef.get()?.performClick()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "Gọi video",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                androidx.compose.ui.viewinterop.AndroidView<android.view.View>(
                                    modifier = Modifier.size(1.dp).alpha(0f),
                                    factory = { context ->
                                        com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton(context).apply {
                                            setIsVideoCall(true)
                                            resourceID = com.example.alohi.utils.ZegoConfig.RESOURCE_ID
                                            setInvitees(
                                                java.util.Collections.singletonList(
                                                    com.zegocloud.uikit.service.defines.ZegoUIKitUser(partnerId, partnerName)
                                                )
                                            )
                                            videoBtnRef.set(this)
                                        }
                                    }
                                )
                            }
                        }

                        IconButton(onClick = onNavigateToDetail) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Thêm",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
                HorizontalDivider(
                    color = Color(0xFFE5E5EA),
                    thickness = 0.5.dp
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            ) {
                HorizontalDivider(
                    color = Color(0xFFE5E5EA),
                    thickness = 0.5.dp
                )

                // REPLY BAR (if replying to a message)
                val replyingTo = uiState.replyingToMessage
                if (replyingTo != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0F4FF))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(36.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = replyingTo.sender?.displayName ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                            )
                            Text(
                                text = replyingTo.displayText(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8E8E93),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { mainViewModel.setReplyingTo(null) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Hủy trả lời",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF8E8E93)
                            )
                        }
                    }
                }

                // MESSAGE COMPOSER
                MessageComposer(
                    value = messageText,
                    onValueChange = { newText ->
                        messageText = newText
                        if (newText.isNotBlank()) {
                            mainViewModel.onTyping(conversationId)
                        }
                    },
                    onSendClick = {
                        if (messageText.isNotBlank()) {
                            if (uiState.replyingToMessage != null) {
                                mainViewModel.sendReplyMessage(conversationId, messageText.trim())
                            } else {
                                mainViewModel.sendMessage(conversationId, messageText.trim())
                            }
                            messageText = ""
                        }
                    },
                    activePanel = activePanel,
                    onStickerToggle = { togglePanel(ChatPanel.STICKER) },
                    onAttachToggle = { togglePanel(ChatPanel.ATTACHMENT) },
                    onMicToggle = { togglePanel(ChatPanel.VOICE) },
                    onGalleryToggle = {
                        imagePickerLauncher.launch("image/*")
                    },
                    onTextFieldFocused = {
                        activePanel = ChatPanel.NONE
                    }
                )

                // BOTTOM PANELS
                AnimatedVisibility(
                    visible = activePanel == ChatPanel.STICKER,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    StickerPanel(
                        onStickerClick = { sticker ->
                            messageText += sticker
                        }
                    )
                }

                AnimatedVisibility(
                    visible = activePanel == ChatPanel.ATTACHMENT,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    AttachmentPanel(
                        onOptionClick = { option ->
                            when (option) {
                                "photo" -> imagePickerLauncher.launch("image/*")
                                "video" -> videoPickerLauncher.launch("video/*")
                                "file" -> filePickerLauncher.launch("*/*")
                                else -> { }
                            }
                        }
                    )
                }

                AnimatedVisibility(
                    visible = activePanel == ChatPanel.GALLERY,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    GalleryPanel(
                        onCameraClick = { },
                        onPhotoClick = { imagePickerLauncher.launch("image/*") }
                    )
                }

                AnimatedVisibility(
                    visible = activePanel == ChatPanel.VOICE,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    VoiceRecordPanel(
                        onSendRecording = { file, durationMs ->
                            mainViewModel.sendVoiceMessage(conversationId, file, durationMs)
                            activePanel = ChatPanel.NONE
                        },
                        onSendAsText = { }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ═══════════════════════════════════════
            // MESSAGES LIST
            // ═══════════════════════════════════════
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF7F7F8)),
                state = listState,
                reverseLayout = true,
            ) {
                // Bottom spacer (in reverse layout, the first item in the DSL behaves as the bottom spacing)
                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // ── Typing indicator ──
                if (typingUser != null) {
                    item(key = "typing_indicator") {
                        TypingIndicator(userName = typingUser)
                    }
                }

                // ── Empty states ──
                if (displayItems.isEmpty() && !uiState.messagesLoading) {
                    item(key = "empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "👋",
                                    fontSize = 48.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Hãy bắt đầu cuộc trò chuyện!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFF8E8E93),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Gửi tin nhắn đầu tiên cho $partnerName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFAEAEB2)
                                )
                            }
                        }
                    }
                } else if (uiState.messagesLoading && displayItems.isEmpty()) {
                    items(8) { index ->
                        ShimmerMessageBubble(isFromMe = index % 3 != 0)
                    }
                } else {
                    // ── Actual messages with grouping + image grids ──
                    val reversedItems = displayItems.asReversed()
                    items(reversedItems, key = { it.key }) { item ->
                        when (item) {
                            is DisplayItem.Single -> {
                                val info = item.info
                                val msg = info.message
                                val isFromMe = msg.sender?.id == currentUserId

                                if (info.showDateSeparator) {
                                    DateSeparator(dateString = msg.createdAt)
                                }

                                AnimatedMessageBubble(
                                    msg = msg,
                                    isFromMe = isFromMe,
                                    isFirstInGroup = info.isFirstInGroup,
                                    isLastInGroup = info.isLastInGroup,
                                    index = item.index,
                                    totalCount = displayItems.size,
                                    currentUserId = currentUserId,
                                    senderName = if (isGroupChat && !isFromMe) msg.sender?.displayName else null,
                                    onImageClick = { url -> viewingImageUrl = url },
                                    onLongPress = {
                                        selectedMessage = msg
                                        showMessageMenu = true
                                    },
                                )
                            }

                            is DisplayItem.ImageGrid -> {
                                val firstInfo = item.infos.first()
                                if (firstInfo.showDateSeparator) {
                                    DateSeparator(dateString = firstInfo.message.createdAt)
                                }

                                ImageGridBubble(
                                    images = item.infos,
                                    isFromMe = item.isFromMe,
                                    onImageClick = { url -> viewingImageUrl = url },
                                    onLongPress = { msg ->
                                        selectedMessage = msg
                                        showMessageMenu = true
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // ═══════════════════════════════════════
            // SCROLL-TO-BOTTOM FAB
            // ═══════════════════════════════════════
            AnimatedVisibility(
                visible = !isAtBottom && messages.size > 5,
                enter = fadeIn(tween(200)) + scaleIn(tween(200)),
                exit = fadeOut(tween(150)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    containerColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Cuộn xuống",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// IMAGE GRID BUBBLE — Zalo-style multi-image layout
// ═══════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageGridBubble(
    images: List<MessageGroupInfo>,
    isFromMe: Boolean,
    onImageClick: (String) -> Unit,
    onLongPress: (MessageItem) -> Unit,
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxWidth = screenWidth * 0.75f
    val colors = AloHiTheme.extendedColors
    val serverUrl = ApiClient.BASE_URL.removeSuffix("api/")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isFromMe) 56.dp else 12.dp,
                end = if (isFromMe) 12.dp else 56.dp,
                top = 6.dp,
                bottom = 2.dp
            ),
        contentAlignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .clip(RoundedCornerShape(16.dp))
                .padding(0.dp)
        ) {
            // Grid layout: 2 columns
            val chunked = images.chunked(2)
            chunked.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    row.forEach { info ->
                        val content = info.message.displayText()
                        val isRemoteRelative = !content.startsWith("http") && !content.startsWith("content://") && !content.startsWith("file://")
                        val imageUrl = if (isRemoteRelative) {
                            if (content.startsWith("/")) serverUrl + content.removePrefix("/") else serverUrl + content
                        } else {
                            content
                        }

                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Ảnh",
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .combinedClickable(
                                    onClick = { onImageClick(imageUrl) },
                                    onLongClick = { onLongPress(info.message) }
                                ),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    // If odd number in last row, add spacer to maintain grid
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                if (rowIndex < chunked.lastIndex) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// HELPER COMPOSABLES
// ═══════════════════════════════════════════════════════

/** Animated message bubble with entrance animation */
@Composable
private fun AnimatedMessageBubble(
    msg: MessageItem,
    isFromMe: Boolean,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    index: Int,
    totalCount: Int,
    currentUserId: String?,
    senderName: String? = null,
    onImageClick: ((String) -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
) {
    val timestamp = msg.createdAt?.let {
        try { it.substringAfter("T").take(5) } catch (e: Exception) { "" }
    } ?: ""

    val status = when {
        msg.deliveryStatus == "sending" -> MessageStatus.SENDING
        msg.deliveryStatus == "read" -> MessageStatus.READ
        msg.deliveryStatus == "delivered" -> MessageStatus.DELIVERED
        msg.deliveryStatus == "sent" -> MessageStatus.SENT
        isFromMe && msg.readBy?.isNotEmpty() == true -> MessageStatus.READ
        isFromMe -> MessageStatus.SENT
        else -> MessageStatus.READ
    }

    // Entrance animation: only animate the last few messages
    val shouldAnimate = index >= totalCount - 3
    val animAlpha = remember { Animatable(if (shouldAnimate) 0f else 1f) }
    val animTranslateY = remember { Animatable(if (shouldAnimate) 20f else 0f) }

    LaunchedEffect(msg.id ?: msg.messageId) {
        if (shouldAnimate) {
            launch {
                animAlpha.animateTo(
                    1f,
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                )
            }
            launch {
                animTranslateY.animateTo(
                    0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
    }

    // Build reactions summary: emoji -> count
    val reactionsSummary = remember(msg.reactions) {
        msg.reactions?.groupBy { it.emoji ?: "" }
            ?.filter { it.key.isNotEmpty() }
            ?.map { (emoji, entries) -> emoji to entries.size }
    }

    ChatBubble(
        message = msg.displayText(),
        timestamp = timestamp,
        isFromMe = isFromMe,
        status = status,
        senderName = senderName,
        isFirstInGroup = isFirstInGroup,
        isLastInGroup = isLastInGroup,
        messageType = msg.type ?: "text",
        replyToSenderName = msg.replyTo?.sender?.displayName,
        replyToContent = msg.replyTo?.content,
        reactions = reactionsSummary,
        isForwarded = msg.forwardedFrom != null,
        onImageClick = onImageClick,
        onLongPress = onLongPress,
        modifier = Modifier.graphicsLayer {
            alpha = animAlpha.value
            translationY = animTranslateY.value
        }
    )
}

/** Date separator between message groups */
@Composable
private fun DateSeparator(dateString: String?) {
    val displayDate = remember(dateString) {
        formatDateForSeparator(dateString)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayDate,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF8E8E93),
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .background(
                    color = Color(0xFFE8E8ED),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

/** Typing indicator with animated bouncing dots — like iMessage */
@Composable
private fun TypingIndicator(userName: String?) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    val dot1 = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "dot1"
    )
    val dot2 = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing, delayMillis = 100),
            repeatMode = RepeatMode.Reverse
        ), label = "dot2"
    )
    val dot3 = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ), label = "dot3"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .background(
                    color = Color(0xFFE8E8ED),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(dot1, dot2, dot3).forEach { dotAnim ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .offset(y = dotAnim.value.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8E8E93))
                )
            }
        }
    }
}

/** Shimmer skeleton bubble for loading state */
@Composable
private fun ShimmerMessageBubble(isFromMe: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha = infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "shimmerAlpha"
    )

    val alignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleWidth = if (isFromMe) 0.55f else 0.65f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isFromMe) 80.dp else 12.dp,
                end = if (isFromMe) 12.dp else 80.dp,
                top = 4.dp,
                bottom = 4.dp
            ),
        contentAlignment = alignment
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth(bubbleWidth)
                    .height(if (isFromMe) 36.dp else 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE0E0E0).copy(alpha = shimmerAlpha.value))
            )
        }
    }
}

/** Formats ISO date string for display in date separator */
private fun formatDateForSeparator(iso: String?): String {
    if (iso == null) return "Hôm nay"
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = sdf.parse(iso.substringBefore("T")) ?: return "Hôm nay"
        val now = java.util.Date()

        val calNow = java.util.Calendar.getInstance().apply { time = now }
        val calDate = java.util.Calendar.getInstance().apply { time = date }

        when {
            calNow.get(java.util.Calendar.DAY_OF_YEAR) == calDate.get(java.util.Calendar.DAY_OF_YEAR) &&
                calNow.get(java.util.Calendar.YEAR) == calDate.get(java.util.Calendar.YEAR) -> "Hôm nay"
            calNow.get(java.util.Calendar.DAY_OF_YEAR) - calDate.get(java.util.Calendar.DAY_OF_YEAR) == 1 &&
                calNow.get(java.util.Calendar.YEAR) == calDate.get(java.util.Calendar.YEAR) -> "Hôm qua"
            else -> {
                val displaySdf = java.text.SimpleDateFormat("dd 'tháng' MM, yyyy", java.util.Locale("vi"))
                displaySdf.format(date)
            }
        }
    } catch (e: Exception) {
        "Hôm nay"
    }
}

/** Check if two ISO timestamps are within a time window (for batch image grouping) */
private fun isWithinTimeWindow(t1: String?, t2: String?, windowMs: Long): Boolean {
    if (t1 == null || t2 == null) return false
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val time1 = sdf.parse(t1.substringBefore(".").substringBefore("Z"))?.time ?: return false
        val time2 = sdf.parse(t2.substringBefore(".").substringBefore("Z"))?.time ?: return false
        kotlin.math.abs(time1 - time2) <= windowMs
    } catch (e: Exception) { false }
}
