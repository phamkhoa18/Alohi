package com.example.alohi.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity — cached message for offline-first architecture.
 * Maps 1:1 with MessageItem from API, stored locally for instant loading.
 */
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId", "createdAt"]),
        Index(value = ["messageId"], unique = true)
    ]
)
data class MessageEntity(
    @PrimaryKey
    val messageId: String,           // Server message ID (or clientMessageId for pending)

    val conversationId: String,

    // Sender info (denormalized for fast loading without JOINs)
    val senderId: String,
    val senderDisplayName: String,
    val senderAvatarUrl: String? = null,
    val senderAvatarThumbnailUrl: String? = null,

    // Content
    val content: String? = null,
    val preview: String? = null,
    val type: String = "text",       // text, image, video, file, audio

    // Status
    val isRecalled: Boolean = false,
    val deliveryStatus: String? = null,  // sending, sent, delivered, read

    // Timestamps
    val createdAt: String? = null,

    // Client tracking
    val clientMessageId: String? = null, // for matching optimistic messages

    // Sync metadata
    val isSynced: Boolean = true,    // false = pending upload to server

    // Metadata
    val attachmentsJson: String? = null,
)
