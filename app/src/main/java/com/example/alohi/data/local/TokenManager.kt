package com.example.alohi.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * AloHi Token Manager — DataStore-based
 * Persists JWT tokens and user session data securely.
 */

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "alohi_auth")

class TokenManager(private val context: Context) {

    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val USER_ID = stringPreferencesKey("user_id")
        private val DEVICE_ID = stringPreferencesKey("device_id")
        private val USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")
        private val USER_PHONE = stringPreferencesKey("user_phone")
    }

    // ── Save Tokens ──
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            prefs[REFRESH_TOKEN] = refreshToken
        }
    }

    // ── Save User Info ──
    suspend fun saveUserInfo(userId: String, displayName: String, phone: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID] = userId
            prefs[USER_DISPLAY_NAME] = displayName
            prefs[USER_PHONE] = phone
        }
    }

    // ── Save Device ID ──
    suspend fun saveDeviceId(deviceId: String) {
        context.dataStore.edit { prefs ->
            prefs[DEVICE_ID] = deviceId
        }
    }

    // ── Flows ──
    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }
    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID] }
    val deviceId: Flow<String?> = context.dataStore.data.map { it[DEVICE_ID] }
    val userDisplayName: Flow<String?> = context.dataStore.data.map { it[USER_DISPLAY_NAME] }

    /**
     * Sync accessor for OkHttp interceptor (runs on IO thread)
     */
    fun getAccessTokenSync(): String? = runBlocking {
        context.dataStore.data.first()[ACCESS_TOKEN]
    }

    fun getRefreshTokenSync(): String? = runBlocking {
        context.dataStore.data.first()[REFRESH_TOKEN]
    }

    fun getDeviceIdSync(): String? = runBlocking {
        context.dataStore.data.first()[DEVICE_ID]
    }

    fun getUserIdSync(): String? = runBlocking {
        context.dataStore.data.first()[USER_ID]
    }

    fun getUserDisplayNameSync(): String? = runBlocking {
        context.dataStore.data.first()[USER_DISPLAY_NAME]
    }

    /**
     * Check if user is logged in
     */
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map {
        !it[ACCESS_TOKEN].isNullOrEmpty()
    }

    /**
     * Clear all auth data (logout)
     */
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
