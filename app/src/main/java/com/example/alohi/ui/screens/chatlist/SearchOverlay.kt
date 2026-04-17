package com.example.alohi.ui.screens.chatlist

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.alohi.ui.components.AvatarImage
import com.example.alohi.ui.theme.AloHiTheme
import com.example.alohi.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

/**
 * AloHi Search Overlay — REAL API SEARCH
 * Features:
 * - Auto-focused search input
 * - Debounced real-time search via API
 * - Online friends as suggestions
 * - Search results with "Add friend" action
 */
@Composable
fun SearchOverlay(
    mainViewModel: MainViewModel,
    onDismiss: () -> Unit,
    onResultClick: (String, String) -> Unit,
) {
    var searchText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val colors = AloHiTheme.extendedColors
    val uiState by mainViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Debounced search — triggers API after 400ms
    LaunchedEffect(searchText) {
        if (searchText.length >= 2) {
            delay(400)
            mainViewModel.searchUsers(searchText)
        } else {
            mainViewModel.clearSearchResults()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // ── Search Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                mainViewModel.clearSearchResults()
                onDismiss()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF2F2F7))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (searchText.isEmpty()) {
                            Text(
                                text = "Tìm bạn bè, nhóm, tin nhắn...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF8E8E93)
                            )
                        }
                        BasicTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { mainViewModel.searchUsers(searchText) }
                            )
                        )
                    }

                    if (searchText.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Xóa",
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    searchText = ""
                                    mainViewModel.clearSearchResults()
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
        }

        HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)

        // ── Content ──
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (searchText.isEmpty()) {
                // Suggestions: online friends
                if (uiState.onlineFriends.isNotEmpty()) {
                    item {
                        Text(
                            text = "Bạn bè đang online",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    items(uiState.onlineFriends) { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onResultClick(friend.id, friend.displayName) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarImage(
                                name = friend.displayName,
                                size = 44.dp,
                                showOnlineIndicator = true,
                                isOnline = true
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = friend.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Đang hoạt động",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.online
                                )
                            }
                        }
                    }
                }

                // All friends
                if (uiState.friends.isNotEmpty()) {
                    item {
                        HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 6.dp)
                        Text(
                            text = "Tất cả bạn bè (${uiState.friendCount})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    items(uiState.friends) { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onResultClick(friend.id, friend.displayName) }
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
                            Column {
                                Text(
                                    text = friend.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (friend.isOnline == true) "Đang hoạt động" else "Không hoạt động",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (friend.isOnline == true) colors.online else colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // If no friends either
                if (uiState.friends.isEmpty() && uiState.onlineFriends.isEmpty()) {
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
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Chưa có bạn bè",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textSecondary
                            )
                            Text(
                                text = "Tìm bạn bè qua số điện thoại hoặc tên",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textTertiary
                            )
                        }
                    }
                }
            } else {
                // ── Search Results ──
                item {
                    Text(
                        text = "Kết quả tìm kiếm",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                if (uiState.searchLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else if (uiState.searchResults.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFFD1D1D6),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Không tìm thấy kết quả",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textSecondary
                            )
                            Text(
                                text = "Thử tìm kiếm với từ khóa khác",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textTertiary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(uiState.searchResults) { user ->
                        val isFriend = uiState.friends.any { it.id == user.id }
                        val isMe = user.id == uiState.currentUser?.id

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!isMe) onResultClick(user.id, user.displayName)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarImage(
                                name = user.displayName,
                                size = 44.dp,
                                showOnlineIndicator = true,
                                isOnline = user.isOnline == true
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = when {
                                        isMe -> "Bạn"
                                        isFriend -> "Bạn bè"
                                        else -> user.phone ?: ""
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isFriend) colors.online else colors.textSecondary
                                )
                            }

                            // Add friend button if not friend and not me
                            if (!isFriend && !isMe) {
                                if (user.friendStatus == "sent") {
                                    TextButton(
                                        onClick = { mainViewModel.cancelFriendRequest(user.id) }
                                    ) {
                                        Text(
                                            text = "Thu hồi",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                } else {
                                    TextButton(
                                        onClick = { mainViewModel.sendFriendRequest(user.id) }
                                    ) {
                                        Text(
                                            text = "Kết bạn",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
