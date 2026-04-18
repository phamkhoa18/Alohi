package com.example.alohi.ui.screens.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.alohi.data.remote.ApiClient
import com.example.alohi.ui.components.AvatarImage
import com.example.alohi.ui.components.ImageViewerDialog
import com.example.alohi.ui.theme.AloHiTheme
import com.example.alohi.ui.viewmodel.MainViewModel
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.PersonRemove
import com.example.alohi.ui.screens.group.AddMembersDialog

/**
 * Chat Detail / Info screen — Zalo/Messenger style
 *
 * Features:
 * - Partner profile header (avatar, name, online status)
 * - Media tabs: Ảnh | Video | File
 * - In-chat message search
 * - Quick actions: Mute, Pin, Block, Delete
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailScreen(
    conversationId: String,
    partnerName: String,
    mainViewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val messages = uiState.messages
    val serverUrl = ApiClient.BASE_URL.removeSuffix("api/")

    // ── Filter media from messages ──
    val imageMessages = remember(messages) {
        messages.filter { it.type == "image" }
    }
    val videoMessages = remember(messages) {
        messages.filter { it.type == "video" }
    }
    val fileMessages = remember(messages) {
        messages.filter { it.type == "file" }
    }

    // All image URLs for gallery viewer
    val allImageUrls = remember(imageMessages) {
        imageMessages.map { msg ->
            val content = msg.content ?: msg.preview ?: ""
            val isRemoteRelative = !content.startsWith("http") && !content.startsWith("content://") && !content.startsWith("file://")
            if (isRemoteRelative) {
                if (content.startsWith("/")) serverUrl + content.removePrefix("/") else serverUrl + content
            } else {
                content
            }
        }
    }

    // ── Tab state ──
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Ảnh (${imageMessages.size})", "Video (${videoMessages.size})", "File (${fileMessages.size})")

    // ── Search & Dialog state ──
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var showAddMembersDialog by remember { mutableStateOf(false) }
    val searchResults = remember(searchQuery, messages) {
        if (searchQuery.isBlank()) emptyList()
        else messages.filter {
            val text = it.content ?: it.preview ?: ""
            text.contains(searchQuery, ignoreCase = true)
        }
    }

    // ── Image & Video viewer state ──
    var viewerImageIndex by remember { mutableIntStateOf(-1) }
    var viewingVideoUrl by remember { mutableStateOf<String?>(null) }

    // ── Partner / Group info ──
    val currentConversation = uiState.conversations.firstOrNull { it.id == conversationId }
    val isGroupChat = currentConversation?.type == "group"
    val participants = currentConversation?.participants ?: emptyList()
    val currentUserId = uiState.currentUser?.id
    val partnerParticipant = currentConversation?.participants?.firstOrNull {
        it.user?.id != currentUserId
    }
    val isPartnerOnline = partnerParticipant?.user?.isOnline == true
    val partnerAvatar = partnerParticipant?.user?.avatar?.url
    val groupName = currentConversation?.group?.name ?: partnerName
    val groupAvatar = currentConversation?.group?.avatar?.url
    // Check if current user is admin/owner
    val myParticipant = participants.firstOrNull { it.user?.id == currentUserId }
    val isAdmin = myParticipant?.role == "admin" || myParticipant?.role == "owner"

    // Image viewer dialog
    if (viewerImageIndex >= 0 && allImageUrls.isNotEmpty()) {
        ImageViewerDialog(
            imageUrls = allImageUrls,
            initialIndex = viewerImageIndex,
            onDismiss = { viewerImageIndex = -1 }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết hội thoại") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { isSearching = !isSearching }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Tìm kiếm",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF7F7F8))
        ) {
            // ═══════════════════════════════════════
            // PROFILE HEADER
            // ═══════════════════════════════════════
            item(key = "profile_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isGroupChat) {
                        // Group avatar
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (groupAvatar != null) {
                                AvatarImage(
                                    name = groupName,
                                    imageUrl = groupAvatar,
                                    size = 80.dp,
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp),
                                )
                            }
                        }
                    } else {
                        AvatarImage(
                            name = partnerName,
                            imageUrl = partnerAvatar,
                            size = 80.dp,
                            showOnlineIndicator = true,
                            isOnline = isPartnerOnline,
                            modifier = Modifier.clickable { 
                                partnerParticipant?.user?.id?.let { onNavigateToProfile(it) }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isGroupChat) groupName else partnerName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isGroupChat) "${participants.size} thành viên"
                              else if (isPartnerOnline) "Đang hoạt động" else "Offline",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (!isGroupChat && isPartnerOnline) Color(0xFF34C759) else Color(0xFF8E8E93)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // ═══════════════════════════════════════
            // SEARCH BAR (expandable)
            // ═══════════════════════════════════════
            if (isSearching) {
                item(key = "search_bar") {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Tìm tin nhắn...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                    )
                }

                // Search results
                if (searchQuery.isNotBlank()) {
                    if (searchResults.isEmpty()) {
                        item(key = "no_results") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Không tìm thấy kết quả",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                        }
                    } else {
                        items(searchResults, key = { it.id ?: it.messageId ?: it.hashCode() }) { msg ->
                            val time = msg.createdAt?.let {
                                try { it.substringAfter("T").take(5) } catch (e: Exception) { "" }
                            } ?: ""
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = msg.content ?: msg.preview ?: "",
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = "${msg.sender?.displayName ?: "?"} • $time",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF8E8E93)
                                    )
                                },
                                modifier = Modifier.background(Color.White)
                            )
                            HorizontalDivider(color = Color(0xFFF0F0F0))
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // ═══════════════════════════════════════
            // GROUP MEMBERS (only for group chats)
            // ═══════════════════════════════════════
            if (isGroupChat) {
                item(key = "members_header") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Thành viên (${participants.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            if (isAdmin) {
                                Icon(
                                    Icons.Default.GroupAdd,
                                    contentDescription = "Thêm thành viên",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { showAddMembersDialog = true }
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                    }
                }

                items(participants, key = { it.user?.id ?: it.hashCode().toString() }) { participant ->
                    val user = participant.user
                    val role = participant.role
                    val isMe = user?.id == currentUserId
                    ListItem(
                        headlineContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isMe) "${user?.displayName ?: ""} (Bạn)" else (user?.displayName ?: ""),
                                    fontWeight = if (isMe) FontWeight.SemiBold else FontWeight.Normal,
                                )
                                if (role == "admin" || role == "owner") {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (role == "owner") "Trưởng nhóm" else "Admin",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .background(
                                                color = if (role == "owner") Color(0xFFFF9500) else MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        supportingContent = {
                            Text(
                                text = if (user?.isOnline == true) "Đang hoạt động" else "Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (user?.isOnline == true) Color(0xFF34C759) else Color(0xFF8E8E93)
                            )
                        },
                        leadingContent = {
                            AvatarImage(
                                name = user?.displayName ?: "",
                                imageUrl = user?.avatar?.url,
                                size = 40.dp,
                                showOnlineIndicator = true,
                                isOnline = user?.isOnline == true,
                            )
                        },
                        trailingContent = {
                            if (isAdmin && !isMe) {
                                Icon(
                                    Icons.Default.PersonRemove,
                                    contentDescription = "Xóa",
                                    tint = Color(0xFFFF3B30),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable {
                                            user?.id?.let { uid ->
                                                mainViewModel.removeGroupMember(conversationId, uid)
                                            }
                                        }
                                )
                            }
                        },
                        modifier = Modifier.background(Color.White),
                    )
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(start = 72.dp))
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // ═══════════════════════════════════════
            // QUICK ACTIONS
            // ═══════════════════════════════════════
            item(key = "quick_actions") {
                val isMuted = myParticipant?.isMuted == true
                val isBlocked = partnerParticipant?.user?.id?.let { uid ->
                    uiState.currentUser?.blockedUsers?.contains(uid) == true
                } ?: false

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                ) {
                    ListItem(
                        headlineContent = { Text(if (isMuted) "Bật thông báo" else "Tắt thông báo") },
                        leadingContent = {
                            Icon(if (isMuted) Icons.Default.Notifications else Icons.Default.NotificationsOff, contentDescription = null, tint = Color(0xFF8E8E93))
                        },
                        modifier = Modifier.clickable { 
                            mainViewModel.muteConversation(conversationId, isMuted)
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(start = 56.dp))

                    if (!isGroupChat) {
                        ListItem(
                            headlineContent = { Text("Xem trang cá nhân") },
                            leadingContent = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF8E8E93))
                            },
                            modifier = Modifier.clickable { 
                                partnerParticipant?.user?.id?.let { onNavigateToProfile(it) }
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(start = 56.dp))
                    }



                    if (isGroupChat) {
                        // Leave group
                        ListItem(
                            headlineContent = { Text("Rời nhóm", color = Color(0xFFFF9500)) },
                            leadingContent = {
                                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFFF9500))
                            },
                            modifier = Modifier.clickable {
                                mainViewModel.leaveGroup(conversationId) {
                                    onNavigateBack()
                                }
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(start = 56.dp))

                        if (isAdmin) {
                            // Dissolve group (admin only)
                            ListItem(
                                headlineContent = { Text("Giải tán nhóm", color = Color(0xFFFF3B30)) },
                                leadingContent = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF3B30))
                                },
                                modifier = Modifier.clickable {
                                    mainViewModel.dissolveGroup(conversationId) {
                                        onNavigateBack()
                                    }
                                }
                            )
                        }
                    } else {
                        ListItem(
                            headlineContent = { Text(if (isBlocked) "Bỏ chặn" else "Chặn người dùng", color = Color(0xFFFF3B30)) },
                            leadingContent = {
                                Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFFF3B30))
                            },
                            modifier = Modifier.clickable { 
                                partnerParticipant?.user?.id?.let { userId ->
                                    mainViewModel.blockUser(userId, isBlocked)
                                }
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(start = 56.dp))

                        ListItem(
                            headlineContent = { Text("Xóa hội thoại", color = Color(0xFFFF3B30)) },
                            leadingContent = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF3B30))
                            },
                            modifier = Modifier.clickable { 
                                mainViewModel.deleteConversation(conversationId) {
                                    onNavigateBack()
                                }
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // ═══════════════════════════════════════
            // MEDIA TABS
            // ═══════════════════════════════════════
            item(key = "media_tabs") {
                Column(modifier = Modifier.background(Color.White)) {
                    Text(
                        text = "Ảnh, Video & File",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // ── Tab content ──
            when (selectedTab) {
                0 -> {
                    // Images grid
                    if (imageMessages.isEmpty()) {
                        item(key = "no_images") {
                            EmptyMediaState(icon = Icons.Default.Image, text = "Chưa có ảnh nào")
                        }
                    } else {
                        // Render images in chunks of 3 for inline grid
                        val chunked = imageMessages.chunked(3)
                        items(chunked.size, key = { "img_row_$it" }) { rowIndex ->
                            val row = chunked[rowIndex]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(horizontal = 1.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                row.forEachIndexed { colIndex, msg ->
                                    val content = msg.content ?: msg.preview ?: ""
                                    val isRemoteRelative = !content.startsWith("http") && !content.startsWith("content://") && !content.startsWith("file://")
                                    val imageUrl = if (isRemoteRelative) {
                                        if (content.startsWith("/")) serverUrl + content.removePrefix("/") else serverUrl + content
                                    } else {
                                        content
                                    }
                                    val globalIndex = rowIndex * 3 + colIndex

                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Ảnh",
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(2.dp))
                                            .clickable { viewerImageIndex = globalIndex },
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                                // Fill remaining space if row is not full
                                repeat(3 - row.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }

                1 -> {
                    // Videos list
                    if (videoMessages.isEmpty()) {
                        item(key = "no_videos") {
                            EmptyMediaState(icon = Icons.Default.Videocam, text = "Chưa có video nào")
                        }
                    } else {
                        items(videoMessages, key = { it.id ?: it.messageId ?: it.hashCode() }) { msg ->
                            val content = msg.content ?: msg.preview ?: ""
                            val isRemoteRelative = !content.startsWith("http") && !content.startsWith("content://") && !content.startsWith("file://")
                            val videoUrl = if (isRemoteRelative) {
                                if (content.startsWith("/")) serverUrl + content.removePrefix("/") else serverUrl + content
                            } else {
                                content
                            }
                            val time = msg.createdAt?.let {
                                try { it.substringAfter("T").take(5) } catch (e: Exception) { "" }
                            } ?: ""

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .clickable { viewingVideoUrl = videoUrl }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = videoUrl,
                                        contentDescription = "Video",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Icon(
                                        Icons.Default.PlayCircleFilled,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Video",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${msg.sender?.displayName ?: ""} • $time",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF8E8E93)
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF0F0F0))
                        }
                    }
                }

                2 -> {
                    // Files list
                    if (fileMessages.isEmpty()) {
                        item(key = "no_files") {
                            EmptyMediaState(icon = Icons.Default.InsertDriveFile, text = "Chưa có file nào")
                        }
                    } else {
                        items(fileMessages, key = { it.id ?: it.messageId ?: it.hashCode() }) { msg ->
                            val content = msg.content ?: msg.preview ?: ""
                            val fileName = content.substringAfterLast("/").take(40)
                            val time = msg.createdAt?.let {
                                try { it.substringAfter("T").take(5) } catch (e: Exception) { "" }
                            } ?: ""

                            ListItem(
                                headlineContent = {
                                    Text(fileName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                supportingContent = {
                                    Text(
                                        "${msg.sender?.displayName ?: ""} • $time",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF8E8E93)
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                },
                                modifier = Modifier
                                    .background(Color.White)
                                    .clickable { /* TODO: open/download file */ }
                            )
                            HorizontalDivider(color = Color(0xFFF0F0F0))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    // ═══════════════════════════════════════════════════════
    // ADD MEMBERS DIALOG
    // ═══════════════════════════════════════════════════════
    if (showAddMembersDialog && isGroupChat) {
        val currentMemberIds = participants.mapNotNull { it.user?.id }
        AddMembersDialog(
            friends = uiState.friends,
            currentMemberIds = currentMemberIds,
            onDismiss = { showAddMembersDialog = false },
            onAddMembers = { newMemberIds ->
                mainViewModel.addGroupMembers(conversationId, newMemberIds)
                showAddMembersDialog = false
            }
        )
    }

    // ═══════════════════════════════════════════════════════
    // VIDEO VIEWER DIALOG
    // ═══════════════════════════════════════════════════════
    if (viewingVideoUrl != null) {
        VideoViewerDialog(
            videoUrl = viewingVideoUrl!!,
            onDismiss = { viewingVideoUrl = null }
        )
    }

}

@Composable
private fun EmptyMediaState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF8E8E93)
            )
        }
    }
}
