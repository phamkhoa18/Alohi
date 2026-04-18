package com.example.alohi.ui.screens.group

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alohi.data.model.FriendItem
import com.example.alohi.ui.components.AvatarImage
import com.example.alohi.ui.theme.AloHiTheme
import com.example.alohi.ui.viewmodel.MainViewModel

/**
 * AloHi Create Group Screen — Zalo-inspired design
 * Features:
 * - Group name input with icon
 * - Multi-select friend picker with search
 * - Selected members shown as chips
 * - Animated create button
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateGroupScreen(
    mainViewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onGroupCreated: (conversationId: String, groupName: String) -> Unit,
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val colors = AloHiTheme.extendedColors
    val friends = uiState.friends

    var groupName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateListOf<String>() }

    val filteredFriends = if (searchQuery.isBlank()) friends
    else friends.filter {
        it.displayName.contains(searchQuery, ignoreCase = true) ||
                (it.phone?.contains(searchQuery) == true)
    }

    val canCreate = groupName.isNotBlank() && selectedIds.size >= 1

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { _ ->
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Gradient Header ──
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
                            "Tạo nhóm mới",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = Color.White)
                        }
                    },
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            }

            // ── Group Name Input ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Group,
                        "Nhóm",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    placeholder = {
                        Text(
                            "Đặt tên nhóm...",
                            color = colors.textTertiary,
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = colors.divider,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                )
            }

            HorizontalDivider(color = colors.divider, thickness = 0.5.dp)

            // ── Selected Members Chips ──
            AnimatedVisibility(
                visible = selectedIds.isNotEmpty(),
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        "Đã chọn ${selectedIds.size} thành viên",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        selectedIds.forEach { id ->
                            val friend = friends.find { it.id == id }
                            if (friend != null) {
                                SelectedMemberChip(
                                    friend = friend,
                                    onRemove = { selectedIds.remove(id) }
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = colors.divider, thickness = 0.5.dp)

            // ── Search Bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Search,
                    "Tìm kiếm",
                    tint = colors.textTertiary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Tìm bạn bè...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.textTertiary,
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider(color = colors.divider, thickness = 6.dp)

            // ── Friends List ──
            Box(modifier = Modifier.weight(1f)) {
                if (friends.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Group,
                                "Bạn bè",
                                modifier = Modifier.size(64.dp),
                                tint = colors.textTertiary,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Chưa có bạn bè",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textSecondary,
                            )
                            Text(
                                "Thêm bạn bè trước để tạo nhóm",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textTertiary,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 100.dp),
                    ) {
                        item {
                            Text(
                                "Bạn bè (${friends.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                        }
                        items(filteredFriends, key = { it.id }) { friend ->
                            val isSelected = selectedIds.contains(friend.id)
                            FriendSelectRow(
                                friend = friend,
                                isSelected = isSelected,
                                onToggle = {
                                    if (isSelected) selectedIds.remove(friend.id)
                                    else selectedIds.add(friend.id)
                                }
                            )
                        }
                    }
                }
            }

            // ── Create Button ──
            AnimatedVisibility(
                visible = canCreate,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            mainViewModel.createGroup(groupName, selectedIds.toList()) { convoId ->
                                onGroupCreated(convoId, groupName)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Default.Group, "Tạo nhóm", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Tạo nhóm (${selectedIds.size} thành viên)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendSelectRow(
    friend: FriendItem,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    val colors = AloHiTheme.extendedColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Checkbox circle
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else Color.Transparent
                )
                .border(
                    width = if (isSelected) 0.dp else 2.dp,
                    color = if (isSelected) Color.Transparent else colors.textTertiary,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    "Đã chọn",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        AvatarImage(
            name = friend.displayName,
            imageUrl = friend.avatar?.url,
            size = 44.dp,
            showOnlineIndicator = true,
            isOnline = friend.isOnline ?: false,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (friend.phone != null) {
                Text(
                    text = friend.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun SelectedMemberChip(
    friend: FriendItem,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .clickable(onClick = onRemove)
            .padding(start = 4.dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarImage(
            name = friend.displayName,
            imageUrl = friend.avatar?.url,
            size = 26.dp,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = friend.displayName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            Icons.Default.Close,
            "Xóa",
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        )
    }
}
