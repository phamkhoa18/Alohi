package com.example.alohi.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.ui.graphics.vector.ImageVector

// ═══════════════════════════════════════════════════════
// Bottom Navigation Items
// 4-tab structure like Zalo: Chat, Contacts, Discover, Profile
// ═══════════════════════════════════════════════════════

enum class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
) {
    CHAT(
        route = "tab_chat",
        label = "Tin nhắn",
        selectedIcon = Icons.Filled.ChatBubble,
        unselectedIcon = Icons.Outlined.ChatBubbleOutline,
    ),
    CONTACTS(
        route = "tab_contacts",
        label = "Danh bạ",
        selectedIcon = Icons.Filled.Call,
        unselectedIcon = Icons.Outlined.Call,
    ),
    DISCOVER(
        route = "tab_discover",
        label = "Khám phá",
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
    ),
    PROFILE(
        route = "tab_profile",
        label = "Cá nhân",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.PersonOutline,
    );
}
