package com.example.alohi.data.local.dao

import androidx.room.*
import com.example.alohi.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO — Message operations.
 * Provides reactive Flows for instant UI display from cache.
 */
@Dao
interface MessageDao {

    /**
     * Observe all messages in a conversation, ordered by creation time.
     * Flow emits automatically whenever messages table changes.
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    /**
     * Get all messages for a conversation (one-shot, not reactive).
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getMessages(conversationId: String): List<MessageEntity>

    /**
     * Get the newest message timestamp in a conversation.
     * Used for delta-sync: "give me messages newer than this".
     */
    @Query("SELECT MAX(createdAt) FROM messages WHERE conversationId = :conversationId AND isSynced = 1")
    suspend fun getNewestTimestamp(conversationId: String): String?

    /**
     * Get count of cached messages for a conversation.
     */
    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun getMessageCount(conversationId: String): Int

    /**
     * Insert a single message. REPLACE = upsert behavior (update if exists).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    /**
     * Insert a batch of messages (from API sync).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    /**
     * Update delivery status for a message.
     */
    @Query("UPDATE messages SET deliveryStatus = :status WHERE messageId = :messageId")
    suspend fun updateDeliveryStatus(messageId: String, status: String)

    /**
     * Replace optimistic message ID with server ID after ACK.
     * Uses clientMessageId to find the pending message.
     */
    @Query("""
        UPDATE messages 
        SET messageId = :serverMessageId, 
            createdAt = :timestamp,
            deliveryStatus = 'sent',
            isSynced = 1
        WHERE messageId = :clientMessageId
    """)
    suspend fun updateOptimisticMessage(clientMessageId: String, serverMessageId: String, timestamp: String)

    /**
     * Mark ALL messages from a sender in a conversation as READ.
     */
    @Query("""
        UPDATE messages 
        SET deliveryStatus = 'read' 
        WHERE conversationId = :conversationId AND senderId = :senderId
    """)
    suspend fun markAllAsRead(conversationId: String, senderId: String)

    /**
     * Delete a recalled/deleted message.
     */
    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteMessage(messageId: String)

    /**
     * Clear all messages for a conversation (when user deletes conversation).
     */
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun clearConversation(conversationId: String)

    /**
     * Check if a message already exists (dedup).
     */
    @Query("SELECT COUNT(*) FROM messages WHERE messageId = :messageId")
    suspend fun messageExists(messageId: String): Int

    /**
     * Clear all messages (logout).
     */
    @Query("DELETE FROM messages")
    suspend fun clearAll()
}
