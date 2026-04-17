package com.example.alohi.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity — cached conversation for offline-first architecture.
 * Stores conversation metadata + last message + partner info denormalized
 * so chat list loads instantly from Room without any API call.
 */
@Entity(
    tableName = "conversations",
    indices = [Index(value = ["updatedAt"])]
)
data class ConversationEntity(
    @PrimaryKey
    val id: String,                // Conversation ID from server

    val type: String = "private",  // private, group
    val isActive: Boolean = true,

    // For private chats — partner info (denormalized)
    val partnerId: String? = null,
    val partnerDisplayName: String? = null,
    val partnerAvatarUrl: String? = null,
    val partnerIsOnline: Boolean = false,
    val partnerLastSeen: String? = null,

    // For group chats
    val groupName: String? = null,
    val groupAvatarUrl: String? = null,
    val groupDescription: String? = null,

    // Last message preview
    val lastMessageId: String? = null,
    val lastMessagePreview: String? = null,
    val lastMessageType: String? = "text",
    val lastMessageSenderId: String? = null,
    val lastMessageSenderName: String? = null,
    val lastMessageTimestamp: String? = null,

    // Unread count for current user
    val unreadCount: Int = 0,

    // Participant flags
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,

    // Timestamps
    val updatedAt: String? = null,

    // Participants JSON — full list stored as JSON string for complex data
    val participantsJson: String? = null,
)
