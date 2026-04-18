package com.example.alohi.data.model

import com.google.gson.annotations.SerializedName

// ═══════════════════════════════════════════════════════
// API Response Models — matches backend ApiResponse format
// ═══════════════════════════════════════════════════════

/**
 * Generic API response wrapper
 * Backend format: { success, statusCode, message, data, timestamp }
 */
data class ApiResponse<T>(
    val success: Boolean,
    val statusCode: Int,
    val message: String,
    val data: T? = null,
    val timestamp: String? = null,
)

/**
 * Error response from API
 * Backend format: { success, statusCode, message, error: { code, details } }
 */
data class ApiError(
    val success: Boolean,
    val statusCode: Int,
    val message: String,
    val error: ErrorDetail? = null,
)

data class ErrorDetail(
    val code: String? = null,
    val details: List<FieldError>? = null,
)

data class FieldError(
    val field: String? = null,
    val message: String? = null,
)

// ═══════════════════════════════════════════════════════
// Auth Request/Response Models
// ═══════════════════════════════════════════════════════

/** POST /api/auth/send-otp */
data class SendOtpRequest(
    val phone: String,
)

data class SendOtpResponse(
    val message: String,
    val expiresIn: Int,
)

/** POST /api/auth/verify-otp */
data class VerifyOtpRequest(
    val phone: String,
    val code: String,
)

data class ResetPasswordRequest(
    val phone: String,
    val otpCode: String,
    val newPassword: String
)

/** POST /api/auth/register */
data class RegisterRequest(
    val phone: String,
    val password: String,
    val displayName: String,
    val gender: String = "other",
    val dateOfBirth: String? = null,
    val otpCode: String? = null,
)

data class RegisterResponse(
    val userId: String,
)

/** POST /api/auth/login */
data class LoginRequest(
    val phone: String,
    val password: String,
    val deviceId: String,
    val deviceName: String? = null,
    val deviceModel: String? = null,
    val platform: String = "android",
    val osVersion: String? = null,
    val appVersion: String? = null,
    val fcmToken: String? = null,
)

data class LoginResponse(
    val user: UserProfile,
    val accessToken: String,
    val refreshToken: String,
)

/** POST /api/auth/refresh-token */
data class RefreshTokenRequest(
    val refreshToken: String,
)

data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
)

// ═══════════════════════════════════════════════════════
// User Profile Model
// ═══════════════════════════════════════════════════════

data class UserProfile(
    @SerializedName("_id")
    val id: String,
    val phone: String? = null,
    val displayName: String,
    val avatar: AvatarInfo? = null,
    val coverPhoto: CoverPhotoInfo? = null,
    val bio: String? = null,
    val gender: String? = null,
    val dateOfBirth: String? = null,
    val customStatusText: String? = null,
    val customStatusEmoji: String? = null,
    val friendCount: Int? = null,
    val isVerified: Boolean? = null,
    val isOnline: Boolean? = null,
    val lastSeen: String? = null,
    val createdAt: String? = null,
    val friendStatus: String? = null, // "none", "friend", "sent", "received"
    val blockedUsers: List<String>? = null,
    val hasBlockedMe: Boolean? = null,
)

data class DeviceSession(
    @SerializedName("_id")
    val id: String,
    val deviceName: String?,
    val deviceModel: String?,
    val platform: String?,
    val osVersion: String?,
    val appVersion: String?,
    val ipAddress: String?,
    val isActive: Boolean,
    val lastActiveAt: String?,
    val loginAt: String?,
    val deviceId: String
)

data class FcmTokenRequest(
    val token: String,
    val deviceId: String,
    val deviceType: String = "android",
)

data class SyncContactsRequest(
    val phoneNumbers: List<String>
)

data class AvatarInfo(
    val url: String? = null,
    val publicId: String? = null,
    val thumbnailUrl: String? = null,
)

data class CoverPhotoInfo(
    val url: String? = null,
    val publicId: String? = null,
)

// ═══════════════════════════════════════════════════════
// Friends Models
// ═══════════════════════════════════════════════════════

data class FriendsResponse(
    val friends: List<FriendItem>,
    val total: Int? = null,
    val page: Int? = null,
)

data class FriendItem(
    @SerializedName("_id")
    val id: String,
    val displayName: String,
    val avatar: AvatarInfo? = null,
    val isOnline: Boolean? = null,
    val lastSeen: String? = null,
    val phone: String? = null,
)

data class FriendCountResponse(
    val count: Int,
)

data class FriendRequest(
    @SerializedName("_id")
    val id: String,
    val from: UserProfile,
    val to: UserProfile? = null,
    val status: String? = null,
    val message: String? = null,
    val createdAt: String? = null,
)

data class FriendRequestsResponse(
    val requests: List<FriendRequest>? = null,
    val total: Int? = null,
)

// ═══════════════════════════════════════════════════════
// Conversation Models
// ═══════════════════════════════════════════════════════

// Note: Backend paginated() returns data as List<ConversationItem> directly
// ConversationsResponse is no longer needed — use ApiResponse<List<ConversationItem>>

data class ConversationItem(
    @SerializedName("_id")
    val id: String,
    val type: String? = "private", // private, group
    val participants: List<Participant>? = null,
    val lastMessage: LastMessage? = null,
    val updatedAt: String? = null,
    val group: GroupInfo? = null,
    val isActive: Boolean? = true,
)

data class Participant(
    val user: UserProfile? = null,
    val role: String? = "member",
    val unreadCount: Int? = 0,
    val isPinned: Boolean? = false,
    val isMuted: Boolean? = false,
)

data class GroupInfo(
    val name: String? = null,
    val avatar: AvatarInfo? = null,
    val description: String? = null,
)

data class LastMessage(
    val preview: String? = null,
    val type: String? = "text",
    val sender: UserProfile? = null,
    val timestamp: String? = null,
    @SerializedName("_id")
    val id: String? = null,
)

// ═══════════════════════════════════════════════════════
// Message Models
// ═══════════════════════════════════════════════════════

data class MessagesResponse(
    val messages: List<MessageItem>? = null,
    val total: Int? = null,
)

data class MessageItem(
    @SerializedName("_id")
    val id: String? = null,
    val messageId: String? = null,
    val conversation: String? = null,
    val sender: UserProfile? = null,
    val content: String? = null,     // from processMessage response
    val preview: String? = null,     // from MessageMetadata in DB
    val type: String? = "text",
    val isRecalled: Boolean? = false,
    val createdAt: String? = null,
    val readBy: List<ReadByEntry>? = null,
    // Reply
    val replyTo: ReplyInfo? = null,
    // Reactions
    val reactions: List<ReactionEntry>? = null,
    // Forward
    val forwardedFrom: ForwardInfo? = null,
    // Local-only field for delivery status tracking (not from server JSON)
    val deliveryStatus: String? = null, // "sending", "sent", "delivered", "read"
    // Attachments (for file/video sizes and info)
    val attachments: List<com.example.alohi.data.model.AttachmentData>? = null,
) {
    /** Get display text: prefer content (from send response) then preview (from DB) */
    fun displayText(): String = content ?: preview ?: ""
}

data class ReplyInfo(
    @SerializedName("_id")
    val id: String? = null,
    val messageId: String? = null,
    val sender: UserProfile? = null,
    val content: String? = null,
    val type: String? = "text",
)

data class ReactionEntry(
    val user: String? = null,
    val emoji: String? = null,
    val createdAt: String? = null,
)

data class ForwardInfo(
    val messageId: String? = null,
    val originalSender: UserProfile? = null,
)

data class ReadByEntry(
    val user: String? = null,
    val readAt: String? = null,
)

// ═══════════════════════════════════════════════════════
// Request Models — POST/PUT bodies
// ═══════════════════════════════════════════════════════

/** POST /api/messages/:conversationId */
data class SendMessageRequest(
    val content: String? = null,
    val type: String = "text",
    val clientMessageId: String = java.util.UUID.randomUUID().toString(),
    val attachments: List<AttachmentData>? = null,
)

data class AttachmentData(
    val url: String,
    val thumbnailUrl: String? = null,
    val fileType: String = "image", // matches backend fileType
    val fileName: String? = null,
    val fileSize: Long? = null,
    val dimensions: Dimensions? = null,
    val duration: Int? = null, // seconds, for audio/video
)

data class Dimensions(
    val width: Int? = null,
    val height: Int? = null,
)

// Upload response from POST /api/upload endpoints
data class UploadResult(
    val url: String? = null,
    val thumbnailUrl: String? = null,
    val publicId: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val size: Long? = null,
    val format: String? = null,
    val duration: Double? = null,
    val originalName: String? = null,
)

/** POST /api/conversations — create private conversation (backend expects userId) */
data class CreateConversationRequest(
    val userId: String, // for private conversations
    val type: String = "private",
)

/** PUT /api/users/me — partial update */
data class UpdateProfileRequest(
    val displayName: String? = null,
    val bio: String? = null,
    val gender: String? = null,
    val dateOfBirth: String? = null,
    val customStatusText: String? = null,
    val customStatusEmoji: String? = null,
)

// ═══════════════════════════════════════════════════════
// Group Models
// ═══════════════════════════════════════════════════════

/** POST /api/groups — create group */
data class CreateGroupRequest(
    val name: String,
    val members: List<String>,
    val description: String? = null,
)

/** POST /api/groups/:id/members — add members */
data class AddMembersRequest(
    val members: List<String>,
)

/** PUT /api/groups/:id/members/:userId/role */
data class ChangeRoleRequest(
    val role: String, // "admin", "member"
)

/** PUT /api/groups/:id */
data class UpdateGroupRequest(
    val name: String? = null,
    val description: String? = null,
)

/** PUT /api/groups/:id/settings */
data class UpdateGroupSettingsRequest(
    val onlyAdminCanSend: Boolean? = null,
    val onlyAdminCanAddMember: Boolean? = null,
    val onlyAdminCanChangeInfo: Boolean? = null,
    val approvalRequired: Boolean? = null,
    val maxMembers: Int? = null,
)

data class InviteLinkResponse(
    val inviteLink: String,
)

/** POST /api/messages/:messageId/forward */
data class ForwardMessageRequest(
    val targetConversationIds: List<String>,
)

/** POST /api/messages/:messageId/react */
data class ReactMessageRequest(
    val emoji: String,
)

// ═══════════════════════════════════════════════════════
// Story Models
// ═══════════════════════════════════════════════════════

data class StoriesFeedResponse(
    val myStories: List<StoryGroup>? = null,
    val friendsStories: List<StoryGroup>? = null,
)

data class StoryGroup(
    val author: UserProfile,   // Use common UserProfile
    val stories: List<StoryItem>,
    val hasUnread: Boolean? = true,
)

data class StoryItem(
    @SerializedName("_id")
    val id: String,
    val author: UserProfile? = null,
    val type: String? = "image", // image, video, text
    val content: StoryContent? = null,
    val media: StoryMedia? = null,
    val caption: String? = null,
    val music: StoryMusic? = null,
    val privacy: String? = "friends",
    val isActive: Boolean? = true,
    val createdAt: String? = null,
    val expiresAt: String? = null,
    var viewers: List<StoryViewer>? = null,
    var viewCount: Int? = 0,
)

data class StoryContent(
    val text: String? = null,
    val backgroundColor: String? = null,
    val fontFamily: String? = null,
    val textColor: String? = null,
)

data class StoryMedia(
    val url: String? = null,
    val publicId: String? = null,
    val thumbnailUrl: String? = null,
    val duration: Long? = null,
)

data class StoryMusic(
    val name: String? = null,
    val artist: String? = null,
    val url: String? = null,
)

data class StoryViewer(
    val user: UserProfile? = null,
    val viewedAt: String? = null,
    val reaction: String? = null,
)
