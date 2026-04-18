package com.example.alohi.ui.screens.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.alohi.ui.theme.AloHiTheme
import com.example.alohi.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestsScreen(
    mainViewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val receivedRequests = uiState.friendRequests
    val sentRequests = uiState.sentRequests

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Đã nhận (${receivedRequests.size})", "Đã gửi (${sentRequests.size})")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trạng thái kết bạn", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Trở lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = { HorizontalDivider(color = AloHiTheme.extendedColors.divider) },
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (selectedTabIndex == 0) {
                    if (receivedRequests.isEmpty()) {
                        EmptyStateMessage("Bạn không có lời mời kết bạn nào")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(receivedRequests) { request ->
                                FriendRequestItem(
                                    request = request,
                                    onAccept = { mainViewModel.acceptFriendRequest(request.id) },
                                    onReject = { mainViewModel.rejectFriendRequest(request.id) }
                                )
                            }
                        }
                    }
                } else {
                    if (sentRequests.isEmpty()) {
                        EmptyStateMessage("Bạn chưa gửi lời mời kết bạn nào")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(sentRequests) { request ->
                                FriendRequestItem(
                                    request = request,
                                    isSentRequest = true,
                                    onCancel = { mainViewModel.cancelFriendRequest(request.to?.id ?: "") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color.Gray,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}
