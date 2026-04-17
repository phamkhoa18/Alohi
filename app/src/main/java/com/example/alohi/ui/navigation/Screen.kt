package com.example.alohi.ui.navigation

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// ═══════════════════════════════════════════════════════
// AloHi Navigation Routes
// Type-safe sealed class navigation system
// Names are URL-encoded to handle Vietnamese characters
// ═══════════════════════════════════════════════════════

sealed class Screen(val route: String) {
    // ── Auth Flow ──
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Login : Screen("login")
    data object Otp : Screen("otp/{phone}") {
        fun createRoute(phone: String) = "otp/$phone"
    }
    data object Register : Screen("register/{phone}") {
        fun createRoute(phone: String) = "register/$phone"
    }
    data object SetupProfile : Screen("setup_profile")

    // ── Main Tabs ──
    data object Main : Screen("main")

    // ── Chat ──
    data object Conversation : Screen("conversation/{conversationId}/{name}") {
        fun createRoute(conversationId: String, name: String): String {
            val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
            return "conversation/$conversationId/$encodedName"
        }
        fun decodeName(encodedName: String): String {
            return URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString())
        }
    }
    
    data object ConversationDetail : Screen("conversation_detail/{conversationId}/{name}") {
        fun createRoute(conversationId: String, name: String): String {
            val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
            return "conversation_detail/$conversationId/$encodedName"
        }
        fun decodeName(encodedName: String): String {
            return URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString())
        }
    }

    // ── Contacts ──
    data object AddFriend : Screen("add_friend")
    data object FriendRequests : Screen("friend_requests")
    data object UserProfile : Screen("user_profile/{userId}") {
        fun createRoute(userId: String) = "user_profile/$userId"
    }

    // ── Profile/Settings ──
    data object EditProfile : Screen("edit_profile")
    data object Settings : Screen("settings")

    // ── Group ──
    data object CreateGroup : Screen("create_group")

    // ── Call ──
    data object VoiceCall : Screen("voice_call/{userId}/{name}") {
        fun createRoute(userId: String, name: String): String {
            val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
            return "voice_call/$userId/$encodedName"
        }
    }
    data object VideoCall : Screen("video_call/{userId}/{name}") {
        fun createRoute(userId: String, name: String): String {
            val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
            return "video_call/$userId/$encodedName"
        }
    }
}
