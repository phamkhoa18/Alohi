package com.example.alohi.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alohi.ui.navigation.BottomNavItem
import com.example.alohi.ui.screens.chatlist.ChatListScreen
import com.example.alohi.ui.screens.contacts.ContactsScreen
import com.example.alohi.ui.screens.discover.DiscoverScreen
import com.example.alohi.ui.screens.profile.ProfileScreen
import com.example.alohi.ui.theme.AloHiTheme
import com.example.alohi.ui.viewmodel.MainViewModel

/**
 * AloHi Main Screen — Shell with Bottom Navigation
 * Connected to MainViewModel for real data from backend
 */
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onChatClick: (String, String) -> Unit,
    onNavigateToAddFriend: () -> Unit = {},
    onNavigateToCreateGroup: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToCreateStory: () -> Unit = {},
    onNavigateToStoryViewer: (String) -> Unit = {}
) {
    // API requests are already handled by MainViewModel's init block,
    // so we don't need a LaunchedEffect here to avoid duplicate fetches on back-navigation.

    val uiState by mainViewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = BottomNavItem.entries

    val unreadCount = uiState.conversations.sumOf { convo ->
        convo.participants?.firstOrNull { it.user?.id == uiState.currentUser?.id }?.unreadCount ?: 0
    }
    val requestCount = uiState.friendRequests.size

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AloHiBottomBar(
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                tabs = tabs,
                chatUnreadCount = unreadCount,
                contactBadgeCount = requestCount
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (selectedTab) {
                0 -> ChatListScreen(
                    conversations = uiState.conversations,
                    isLoading = uiState.conversationsLoading,
                    currentUserId = uiState.currentUser?.id,
                    onlineFriends = uiState.onlineFriends,
                    mainViewModel = mainViewModel,
                    onChatClick = onChatClick,
                    onCreateGroupClick = onNavigateToCreateGroup,
                    onRefresh = { mainViewModel.loadConversations() },
                    onNavigateToCreateStory = onNavigateToCreateStory,
                    onNavigateToStoryViewer = onNavigateToStoryViewer
                )
                1 -> ContactsScreen(
                    friends = uiState.friends,
                    friendRequests = uiState.friendRequests,
                    isLoading = uiState.friendsLoading,
                    mainViewModel = mainViewModel,
                    onContactClick = { userId, name ->
                        // Create/find conversation and navigate
                        mainViewModel.createConversation(userId) { convoId ->
                            onChatClick(convoId, name)
                        }
                    },
                    onAddFriendClick = onNavigateToAddFriend,
                    onCreateGroupClick = onNavigateToCreateGroup,
                    onRefresh = {
                        mainViewModel.loadFriends()
                        mainViewModel.loadFriendRequests()
                    }
                )
                2 -> DiscoverScreen(mainViewModel)
                3 -> {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    ProfileScreen(
                        currentUser = uiState.currentUser,
                        sessions = uiState.sessions,
                        onUpdateProfile = { updates -> mainViewModel.updateProfile(updates) },
                        onUpdateAvatar = { uri -> mainViewModel.updateAvatar(context, uri) },
                        onLoadSessions = { mainViewModel.loadSessions() },
                        onLogoutSession = { sessionId -> mainViewModel.logoutSession(sessionId) },
                        onLogout = onLogout,
                        onRefresh = { mainViewModel.loadProfile() }
                    )
                }
            }
        }
    }
}

@Composable
private fun AloHiBottomBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<BottomNavItem>,
    chatUnreadCount: Int = 0,
    contactBadgeCount: Int = 0,
) {
    val colors = AloHiTheme.extendedColors

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = selectedIndex == index

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                icon = {
                    BadgedBox(
                        badge = {
                            val badgeCount = when (index) {
                                0 -> chatUnreadCount
                                1 -> contactBadgeCount
                                else -> 0
                            }
                            if (badgeCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (badgeCount > 99) "99+" else "$badgeCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = colors.textTertiary,
                    unselectedTextColor = colors.textTertiary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                )
            )
        }
    }
}
