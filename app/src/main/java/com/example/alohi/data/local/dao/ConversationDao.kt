package com.example.alohi.data.local.dao

import androidx.room.*
import com.example.alohi.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO — Conversation operations.
 * Chat list loads from Room (instant) then syncs from API in background.
 */
@Dao
interface ConversationDao {

    /**
     * Observe all conversations, ordered by most recent activity.
     * Flow emits automatically when data changes → chat list updates instantly.
     */
    @Query("SELECT * FROM conversations WHERE isActive = 1 ORDER BY updatedAt DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    /**
     * Get all conversations (one-shot).
     */
    @Query("SELECT * FROM conversations WHERE isActive = 1 ORDER BY updatedAt DESC")
    suspend fun getConversations(): List<ConversationEntity>

    /**
     * Get a single conversation by ID.
     */
    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversation(id: String): ConversationEntity?

    /**
     * Insert or update a single conversation.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    /**
     * Insert or update a batch of conversations (from API sync).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ConversationEntity>)

    /**
     * Update last message preview for a conversation (from socket event).
     */
    @Query("""
        UPDATE conversations 
        SET lastMessagePreview = :preview, 
            lastMessageTimestamp = :timestamp, 
            lastMessageSenderId = :senderId,
            lastMessageSenderName = :senderName,
            updatedAt = :timestamp
        WHERE id = :conversationId
    """)
    suspend fun updateLastMessage(
        conversationId: String,
        preview: String,
        timestamp: String,
        senderId: String?,
        senderName: String?
    )

    /**
     * Increment unread count by 1 (when receiving a message while not in that conversation).
     */
    @Query("UPDATE conversations SET unreadCount = unreadCount + 1 WHERE id = :conversationId")
    suspend fun incrementUnread(conversationId: String)

    /**
     * Reset unread count to 0 (when user opens the conversation).
     */
    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun resetUnread(conversationId: String)

    /**
     * Update partner's online status.
     */
    @Query("UPDATE conversations SET partnerIsOnline = :isOnline, partnerLastSeen = :lastSeen WHERE partnerId = :partnerId")
    suspend fun updatePartnerOnlineStatus(partnerId: String, isOnline: Boolean, lastSeen: String?)

    /**
     * Soft delete (mark inactive).
     */
    @Query("UPDATE conversations SET isActive = 0 WHERE id = :conversationId")
    suspend fun softDelete(conversationId: String)

    /**
     * Hard delete.
     */
    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun delete(conversationId: String)

    /**
     * Clear all conversations (logout).
     */
    @Query("DELETE FROM conversations")
    suspend fun clearAll()
}
