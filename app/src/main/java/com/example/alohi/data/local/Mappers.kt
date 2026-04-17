package com.example.alohi.data.local

import com.example.alohi.data.local.entity.ConversationEntity
import com.example.alohi.data.local.entity.MessageEntity
import com.example.alohi.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Mappers — Convert between API models ↔ Room entities.
 *
 * These keep Room entities decoupled from API models,
 * while providing seamless conversion for the offline-first pipeline.
 */

private val gson = Gson()

// ═══════════════════════════════════════════════════════
// MessageItem ↔ MessageEntity
// ═══════════════════════════════════════════════════════

fun MessageItem.toEntity(conversationId: String): MessageEntity {
    return MessageEntity(
        messageId = this.messageId ?: this.id ?: "",
        conversationId = conversationId,
        senderId = this.sender?.id ?: "",
        senderDisplayName = this.sender?.displayName ?: "",
        senderAvatarUrl = this.sender?.avatar?.url,
        senderAvatarThumbnailUrl = this.sender?.avatar?.thumbnailUrl,
        content = this.content,
        preview = this.preview,
        type = this.type ?: "text",
        isRecalled = this.isRecalled ?: false,
        deliveryStatus = this.deliveryStatus,
        createdAt = this.createdAt,
        clientMessageId = null,
        isSynced = true,
    )
}

fun MessageEntity.toModel(): MessageItem {
    return MessageItem(
        id = this.messageId,
        messageId = this.messageId,
        conversation = this.conversationId,
        sender = UserProfile(
            id = this.senderId,
            displayName = this.senderDisplayName,
            avatar = if (this.senderAvatarUrl != null) {
                AvatarInfo(
                    url = this.senderAvatarUrl,
                    thumbnailUrl = this.senderAvatarThumbnailUrl
                )
            } else null,
        ),
        content = this.content,
        preview = this.preview,
        type = this.type,
        isRecalled = this.isRecalled,
        createdAt = this.createdAt,
        deliveryStatus = this.deliveryStatus,
    )
}

// ═══════════════════════════════════════════════════════
// ConversationItem ↔ ConversationEntity
// ═══════════════════════════════════════════════════════

fun ConversationItem.toEntity(currentUserId: String?): ConversationEntity {
    val myParticipant = this.participants?.firstOrNull { it.user?.id == currentUserId }
    val otherParticipant = this.participants?.firstOrNull { it.user?.id != currentUserId }
    val otherUser = otherParticipant?.user

    return ConversationEntity(
        id = this.id,
        type = this.type ?: "private",
        isActive = this.isActive ?: true,
        // Private chat partner
        partnerId = otherUser?.id,
        partnerDisplayName = otherUser?.displayName,
        partnerAvatarUrl = otherUser?.avatar?.url,
        partnerIsOnline = otherUser?.isOnline ?: false,
        partnerLastSeen = otherUser?.lastSeen,
        // Group chat
        groupName = this.group?.name,
        groupAvatarUrl = this.group?.avatar?.url,
        groupDescription = this.group?.description,
        // Last message
        lastMessageId = this.lastMessage?.id,
        lastMessagePreview = this.lastMessage?.preview,
        lastMessageType = this.lastMessage?.type ?: "text",
        lastMessageSenderId = this.lastMessage?.sender?.id,
        lastMessageSenderName = this.lastMessage?.sender?.displayName,
        lastMessageTimestamp = this.lastMessage?.timestamp,
        // Unread
        unreadCount = myParticipant?.unreadCount ?: 0,
        isPinned = myParticipant?.isPinned ?: false,
        isMuted = myParticipant?.isMuted ?: false,
        // Meta
        updatedAt = this.updatedAt,
        participantsJson = gson.toJson(this.participants),
    )
}

fun ConversationEntity.toModel(): ConversationItem {
    val participants: List<Participant>? = try {
        if (this.participantsJson != null) {
            val type = object : TypeToken<List<Participant>>() {}.type
            gson.fromJson<List<Participant>>(this.participantsJson, type)
        } else null
    } catch (e: Exception) {
        null
    }

    // Override online status from Room's real-time field (updated via socket friend:online/offline)
    val updatedParticipants = participants?.map { p ->
        val user = p.user
        if (user != null && this.partnerId != null && user.id == this.partnerId) {
            p.copy(
                unreadCount = this.unreadCount,
                user = user.copy(
                    isOnline = this.partnerIsOnline,
                    lastSeen = this.partnerLastSeen ?: user.lastSeen
                )
            )
        } else {
            p.copy(unreadCount = this.unreadCount)
        }
    }

    return ConversationItem(
        id = this.id,
        type = this.type,
        participants = updatedParticipants,
        lastMessage = if (this.lastMessagePreview != null || this.lastMessageTimestamp != null) {
            LastMessage(
                preview = this.lastMessagePreview,
                type = this.lastMessageType,
                sender = if (this.lastMessageSenderId != null) {
                    UserProfile(
                        id = this.lastMessageSenderId,
                        displayName = this.lastMessageSenderName ?: ""
                    )
                } else null,
                timestamp = this.lastMessageTimestamp,
                id = this.lastMessageId,
            )
        } else null,
        updatedAt = this.updatedAt,
        group = if (this.type == "group") {
            GroupInfo(
                name = this.groupName,
                avatar = if (this.groupAvatarUrl != null) AvatarInfo(url = this.groupAvatarUrl) else null,
                description = this.groupDescription,
            )
        } else null,
        isActive = this.isActive,
    )
}
