package com.example.alohi.ui.screens.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.alohi.data.model.FriendItem
import com.example.alohi.data.model.FriendRequest
import com.example.alohi.ui.components.AvatarImage
import com.example.alohi.ui.theme.AloHiTheme
import com.example.alohi.ui.viewmodel.MainViewModel

/**
 * AloHi Contacts Screen (Tab 2) — REAL API DATA
 * Features:
 * - Friend requests section with accept/reject
 * - All friends grouped alphabetically
 * - Friend count from API
 * - Add friend / Create group actions
 * - Empty state
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    friends: List<FriendItem> = emptyList(),
    friendRequests: List<FriendRequest> = emptyList(),
    isLoading: Boolean = false,
    mainViewModel: MainViewModel,
    onContactClick: (String, String) -> Unit = { _, _ -> },
    onAddFriendClick: () -> Unit = {},
    onCreateGroupClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    val colors = AloHiTheme.extendedColors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Gradient Top Bar ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(colors.gradientStart, colors.gradientEnd)
                    )
                )
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = "Danh bạ",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Search, "Tìm kiếm", tint = Color.White)
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }

        // ── Content ──
        if (isLoading && friends.isEmpty()) {
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
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Quick actions
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 8.dp)
                    ) {
                        QuickActionItem(
                            icon = Icons.Default.PersonAdd,
                            title = "Thêm bạn",
                            subtitle = "Tìm bạn bè qua số điện thoại hoặc QR",
                            onClick = onAddFriendClick
                        )
                        QuickActionItem(
                            icon = Icons.Default.GroupAdd,
                            title = "Tạo nhóm",
                            subtitle = "Tạo nhóm chat mới",
                            onClick = onCreateGroupClick
                        )
                    }
                    HorizontalDivider(color = colors.divider, thickness = 6.dp)
                }

                // ── Friend Requests Section ──
                if (friendRequests.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lời mời kết bạn",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = "${friendRequests.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    items(friendRequests) { request ->
                        FriendRequestItem(
                            request = request,
                            onAccept = { mainViewModel.acceptFriendRequest(request.id) },
                            onReject = { mainViewModel.rejectFriendRequest(request.id) }
                        )
                    }

                    item {
                        HorizontalDivider(color = colors.divider, thickness = 6.dp)
                    }
                }

                // Friend count
                item {
                    Text(
                        text = "Bạn bè (${friends.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                }

                // ── Friends List ──
                if (friends.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Color(0xFFD1D1D6),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Chưa có bạn bè",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Bắt đầu kết bạn bằng cách\ntìm kiếm hoặc quét mã QR",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textTertiary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Group alphabetically
                    val grouped = friends.groupBy {
                        it.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                    }.toSortedMap()

                    grouped.forEach { (letter, friendsInGroup) ->
                        item {
                            Text(
                                text = letter,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textTertiary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }

                        items(friendsInGroup) { friend ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onContactClick(friend.id, friend.displayName) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AvatarImage(
                                    name = friend.displayName,
                                    size = 44.dp,
                                    showOnlineIndicator = true,
                                    isOnline = friend.isOnline == true
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = friend.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (friend.isOnline == true) {
                                        Text(
                                            text = "Đang hoạt động",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.online
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun FriendRequestItem(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(
            name = request.from.displayName,
            size = 50.dp,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = request.from.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Muốn kết bạn với bạn",
                style = MaterialTheme.typography.bodySmall,
                color = AloHiTheme.extendedColors.textSecondary
            )
        }

        // Accept button
        IconButton(
            onClick = onAccept,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Chấp nhận",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Reject button
        IconButton(
            onClick = onReject,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF2F2F7))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Từ chối",
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AloHiTheme.extendedColors.textSecondary
            )
        }
    }
}
