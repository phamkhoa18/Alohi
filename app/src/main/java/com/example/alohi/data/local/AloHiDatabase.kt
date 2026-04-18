package com.example.alohi.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.alohi.data.local.dao.ConversationDao
import com.example.alohi.data.local.dao.MessageDao
import com.example.alohi.data.local.entity.ConversationEntity
import com.example.alohi.data.local.entity.MessageEntity

/**
 * AloHi Room Database — Offline-First Local Cache
 *
 * Architecture giống Zalo:
 * - Messages & conversations cached locally
 * - UI reads from Room (instant)
 * - API sync happens in background
 * - Socket events write directly to Room
 */
@Database(
    entities = [
        MessageEntity::class,
        ConversationEntity::class,
    ],
    version = 2,
    exportSchema = false
)
abstract class AloHiDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao

    companion object {
        @Volatile
        private var INSTANCE: AloHiDatabase? = null

        fun getInstance(context: Context): AloHiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AloHiDatabase::class.java,
                    "alohi_chat.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Clear singleton (call on logout to free resources)
         */
        fun destroyInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
