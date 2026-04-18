package com.example.alohi.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alohi.data.local.AloHiDatabase
import com.example.alohi.data.local.TokenManager
import com.example.alohi.data.local.toEntity
import com.example.alohi.data.local.toModel
import com.example.alohi.data.local.entity.MessageEntity
import com.example.alohi.data.model.*
import com.example.alohi.data.remote.ApiClient
import com.example.alohi.data.remote.SocketManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.example.alohi.utils.ZegoConfig

/**
 * AloHi Main ViewModel — Offline-First Architecture (giống Zalo)
 *
 * Chiến lược caching:
 * 1. Mở app → Load conversations từ Room (tức thì) → Sync API background
 * 2. Vào conversation → Load messages từ Room (tức thì) → Fetch delta từ API
 * 3. Socket message → Save vào Room + update UI cùng lúc
 * 4. Thoát conversation → Messages vẫn nằm trong Room, lần sau load tức thì
 *
 * Room DB is the single source of truth — UI observes Room Flows.
 */

data class MainUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,

    // Connection state for UI Toasts
    val socketState: com.example.alohi.data.remote.SocketManager.SocketState = com.example.alohi.data.remote.SocketManager.SocketState.DISCONNECTED,

    // Profile
    val currentUser: UserProfile? = null,
    val sessions: List<DeviceSession> = emptyList(),
    val sessionsLoading: Boolean = false,

    // Friend Search
    val searchResultUser: UserProfile? = null,
    val isSearchingUser: Boolean = false,
    val searchUserError: String? = null,

    // Conversations (from Room cache)
    val conversations: List<ConversationItem> = emptyList(),
    val conversationsLoading: Boolean = false,

    // Friends
    val friends: List<FriendItem> = emptyList(),
    val friendsLoading: Boolean = false,
    val friendCount: Int = 0,
    val friendRequests: List<FriendRequest> = emptyList(),
    val sentRequests: List<FriendRequest> = emptyList(),
    val onlineFriends: List<FriendItem> = emptyList(),
    val friendSuggestions: List<UserProfile> = emptyList(),

    // Messages (from Room cache for current conversation)
    val messages: List<MessageItem> = emptyList(),
    val messagesLoading: Boolean = false,
    val currentConversationId: String? = null,

    // Search
    val searchResults: List<UserProfile> = emptyList(),
    val searchLoading: Boolean = false,

    // Profile lookup
    val viewedProfile: UserProfile? = null,

    // Phone lookup
    val foundUser: UserProfile? = null,
    val findByPhoneLoading: Boolean = false,

    // Typing indicator
    val typingUser: String? = null, // displayName of person typing in current conv

    // Reply-to state
    val replyingToMessage: MessageItem? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val api = ApiClient.userApi
    private val tokenManager = TokenManager(application)

    // ── Room Database ──
    private val db = AloHiDatabase.getInstance(application)
    private val messageDao = db.messageDao()
    private val conversationDao = db.conversationDao()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Typing debounce
    private var typingJob: Job? = null
    private var isTyping = false

    // Room observation jobs
    private var messagesObserverJob: Job? = null

    companion object {
        private const val TAG = "MainViewModel"
    }

    init {
        SocketManager.init(tokenManager)
        setupSocketListeners()

        // ── Step 1: Load from Room cache (instant) ──
        observeConversationsFromRoom()

        // ── Step 2: Load profile + sync from API (background) ──
        refreshAllData()

        // Retry socket if initial connection failed (token was refreshed by REST)
        viewModelScope.launch {
            kotlinx.coroutines.delay(5000)
            if (!SocketManager.isConnected()) {
                Log.d(TAG, "Socket not connected after init, attempting reconnect...")
                SocketManager.reconnect()
            }
        }
    }

    /**
     * Called on init and when user navigates to MainScreen after login
     * to ensure data is fetched with fresh tokens.
     */
    fun refreshAllData() {
        val cachedUserId = tokenManager.getUserIdSync()
        if (cachedUserId.isNullOrEmpty()) {
            return
        }
        
        val cachedName = tokenManager.getUserDisplayNameSync() ?: "Bạn"
        _uiState.value = _uiState.value.copy(
            currentUser = UserProfile(id = cachedUserId, displayName = cachedName)
        )

        loadProfile()
        syncConversationsFromApi()
        loadFriends()
        loadFriendRequests()
        loadSentRequests()
        loadOnlineFriends()
        loadFriendSuggestions()
    }

    // ═══════════════════════════════════════════════════════
    // ROOM OBSERVERS — UI reads from local cache
    // ═══════════════════════════════════════════════════════

    /**
     * Observe conversations from Room → UI updates instantly.
     * This is the Zalo-like behavior: chat list appears immediately.
     */
    private fun observeConversationsFromRoom() {
        viewModelScope.launch {
            conversationDao.observeConversations().collectLatest { entities ->
                val convos = entities.map { it.toModel() }
                _uiState.value = _uiState.value.copy(
                    conversations = convos,
                    conversationsLoading = false,
                )
            }
        }
    }

    /**
     * Start observing messages for a specific conversation from Room.
     * Called when user enters a conversation screen.
     */
    private fun observeMessagesFromRoom(conversationId: String) {
        messagesObserverJob?.cancel()
        messagesObserverJob = viewModelScope.launch {
            messageDao.observeMessages(conversationId).collectLatest { entities ->
                val msgs = entities.map { it.toModel() }
                val currentLoading = _uiState.value.messagesLoading
                _uiState.value = _uiState.value.copy(
                    messages = msgs,
                    // Hide loading immediately if we got data from cache.
                    // If empty, leave loading state alone (background sync will turn it off if needed)
                    messagesLoading = if (msgs.isNotEmpty()) false else currentLoading,
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    // SOCKET REALTIME LISTENERS
    // ═══════════════════════════════════════════════════════

    private fun setupSocketListeners() {
        // ── Track socket connection state ──
        viewModelScope.launch {
            SocketManager.socketState.collect { state ->
                kotlinx.coroutines.delay(300) // Debounce rapid state changes
                _uiState.value = _uiState.value.copy(socketState = state)
            }
        }

        // ── Friend request received ──
        viewModelScope.launch {
            SocketManager.friendRequestReceived.collect {
                loadFriendRequests()
                _uiState.value = _uiState.value.copy(
                    successMessage = "Bạn nhận được một lời mời kết bạn mới"
                )
            }
        }

        // ── Friend request accepted ──
        viewModelScope.launch {
            SocketManager.friendRequestAccepted.collect {
                loadFriends()
                loadSentRequests()
                loadFriendRequests()
                syncConversationsFromApi()
            }
        }

        // ── Friend request cancelled/rejected ──
        viewModelScope.launch {
            SocketManager.friendRequestCancelled.collect {
                loadFriendRequests()
                loadSentRequests()
            }
        }
        viewModelScope.launch {
            SocketManager.friendRequestRejected.collect {
                loadFriendRequests()
                loadSentRequests()
            }
        }

        // ── New incoming message (save to Room → UI auto-updates via Flow) ──
        viewModelScope.launch {
            SocketManager.messageReceived.collect { json ->
                handleIncomingMessage(json)
            }
        }

        // ── Our message was saved on server (✓ sent) ──
        viewModelScope.launch {
            SocketManager.messageSentAck.collect { json ->
                handleMessageSentAck(json)
            }
        }

        // ── Message delivered to recipient (✓✓) ──
        viewModelScope.launch {
            SocketManager.messageDelivered.collect { json ->
                handleMessageDelivered(json)
            }
        }

        // ── Message read by recipient (blue ✓✓) ──
        viewModelScope.launch {
            SocketManager.messageReadReceipt.collect { json ->
                handleMessageReadReceipt(json)
            }
        }

        // ── Message recalled ──
        viewModelScope.launch {
            SocketManager.messageRecalled.collect { json ->
                handleMessageRecalled(json)
            }
        }

        // ── Typing indicator ──
        viewModelScope.launch {
            SocketManager.typingUpdate.collect { json ->
                handleTypingUpdate(json)
            }
        }

        // ── Friend came online → update Room → green dot appears ──
        viewModelScope.launch {
            SocketManager.friendOnline.collect { userId ->
                Log.d(TAG, "🟢 Friend online: $userId")
                conversationDao.updatePartnerOnlineStatus(userId, true, null)
            }
        }

        // ── Friend went offline → update Room → green dot disappears ──
        viewModelScope.launch {
            SocketManager.friendOffline.collect { userId ->
                Log.d(TAG, "⚫ Friend offline: $userId")
                val lastSeen = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                    .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                    .format(java.util.Date())
                conversationDao.updatePartnerOnlineStatus(userId, false, lastSeen)
            }
        }
    }

    /**
     * Handle incoming message — save to Room DB → UI auto-updates via Flow.
     * No manual state mutation needed!
     */
    private fun handleIncomingMessage(json: JSONObject) {
        val conversationId = json.optString("conversationId")
        val messageId = json.optString("messageId")

        // Acknowledge delivery → sender gets ✓✓
        SocketManager.emitAck(messageId, conversationId)

        // Build entity from socket JSON and save to Room
        val senderJson = json.optJSONObject("sender")
        val entity = MessageEntity(
            messageId = messageId,
            conversationId = conversationId,
            senderId = senderJson?.optString("_id") ?: "",
            senderDisplayName = senderJson?.optString("displayName") ?: "",
            senderAvatarUrl = senderJson?.optJSONObject("avatar")?.optString("url"),
            senderAvatarThumbnailUrl = senderJson?.optJSONObject("avatar")?.optString("thumbnailUrl"),
            content = json.optString("content"),
            preview = json.optString("preview"),
            type = json.optString("type", "text"),
            createdAt = json.optString("timestamp"),
            deliveryStatus = "delivered",
            isSynced = true,
        )

        viewModelScope.launch {
            // Save to Room → Flow observer auto-updates UI
            messageDao.insertMessage(entity)

            // Update conversation's last message in Room
            val senderName = senderJson?.optString("displayName")
            val preview = json.optString("preview", json.optString("content"))
            val timestamp = json.optString("timestamp")
            conversationDao.updateLastMessage(
                conversationId = conversationId,
                preview = preview,
                timestamp = timestamp,
                senderId = entity.senderId,
                senderName = senderName
            )

            val currentConv = _uiState.value.currentConversationId
            if (currentConv == conversationId) {
                // User is viewing this conversation — mark as read
                SocketManager.emitMarkRead(conversationId, messageId)
                conversationDao.resetUnread(conversationId)
            } else {
                // User is NOT in this conversation — increment unread badge + play sound
                conversationDao.incrementUnread(conversationId)
                playNotificationSound()
            }
        }
    }

    /**
     * Play default notification sound for in-app message alerts.
     * Short beep when user gets message while in the app but NOT in that conversation.
     */
    private fun playNotificationSound() {
        try {
            val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = android.media.RingtoneManager.getRingtone(getApplication(), uri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e(TAG, "playNotificationSound error", e)
        }
    }

    /** Handle sent ACK: update optimistic message status in Room */
    private fun handleMessageSentAck(json: JSONObject) {
        val clientMessageId = json.optString("clientMessageId")
        val serverMessageId = json.optString("messageId")
        val timestamp = json.optString("timestamp")

        viewModelScope.launch {
            messageDao.updateOptimisticMessage(clientMessageId, serverMessageId, timestamp)
        }
    }

    /** Handle delivered status: ✓✓ — update in Room */
    private fun handleMessageDelivered(json: JSONObject) {
        val messageId = json.optString("messageId")
        viewModelScope.launch {
            messageDao.updateDeliveryStatus(messageId, "delivered")
        }
    }

    /** Handle read receipt: blue ✓✓ — mark all messages as read in Room */
    private fun handleMessageReadReceipt(json: JSONObject) {
        val conversationId = json.optString("conversationId")
        val currentUser = _uiState.value.currentUser
        if (conversationId == _uiState.value.currentConversationId && currentUser != null) {
            viewModelScope.launch {
                messageDao.markAllAsRead(conversationId, currentUser.id)
            }
        }
    }

    /** Handle message recalled — delete from Room */
    private fun handleMessageRecalled(json: JSONObject) {
        val messageId = json.optString("messageId")
        viewModelScope.launch {
            messageDao.deleteMessage(messageId)
        }
    }

    /** Handle typing indicator (memory only, not persisted) */
    private fun handleTypingUpdate(json: JSONObject) {
        val conversationId = json.optString("conversationId")
        val displayName = json.optString("displayName")
        val isTypingNow = json.optBoolean("isTyping", false)

        if (conversationId == _uiState.value.currentConversationId) {
            _uiState.value = _uiState.value.copy(
                typingUser = if (isTypingNow) displayName else null
            )
        }
    }

    // ═══════════════════════════════════════════════════════
    // PROFILE
    // ═══════════════════════════════════════════════════════

    fun loadProfile() {
        viewModelScope.launch {
            try {
                val response = api.getProfile()
                if (response.isSuccessful && response.body()?.success == true) {
                    val user = response.body()?.data
                    _uiState.value = _uiState.value.copy(currentUser = user)
                    user?.let {
                        tokenManager.saveUserInfo(it.id, it.displayName, it.phone ?: "")
                        initZegoCallKit(it.id, it.displayName)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadProfile error", e)
            }
        }
    }

    private fun initZegoCallKit(userId: String, userName: String) {
        if (ZegoConfig.APP_ID == 0L) {
            Log.e(TAG, "⚠️ Zegocloud CallKit is NOT configured. Please update ZegoConfig.kt")
            return
        }
        val application = getApplication<Application>()
        
        val callInvitationConfig = ZegoUIKitPrebuiltCallInvitationConfig()
        
        // --- 🔴 QUAN TRỌNG: Cấu hình đánh thức màn hình khi tắt App ---
        // Yêu cầu thư viện zego_uikit_signaling_plugin_android trong build.gradle.kts
        // Và khai báo Server Key Firebase trong Zego Console
        
        try {
            // Sử dụng setCustomEvent? Nhưng tốt nhất là gán cờ mặc định:
            val notifyField = callInvitationConfig.javaClass.getField("notifyWhenAppRunningInBackgroundOrQuit")
            notifyField.set(callInvitationConfig, true)
        } catch (e: Exception) {
            Log.d(TAG, "Dùng bản Zego đời cũ/mới nên cấu hình notifyWhenAppRunningInBackgroundOrQuit được tự động bật")
        }

        // Connect user to Zego WebRTC server for calling
        ZegoUIKitPrebuiltCallService.init(
            application,
            ZegoConfig.APP_ID,
            ZegoConfig.APP_SIGN,
            userId,
            userName,
            callInvitationConfig
        )
    }

    fun updateProfile(updates: Map<String, String>) {
        viewModelScope.launch {
            try {
                val response = api.updateProfile(updates)
                if (response.isSuccessful && response.body()?.success == true) {
                    val user = response.body()?.data
                    _uiState.value = _uiState.value.copy(
                        currentUser = user,
                        successMessage = "Cập nhật thành công"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(error = "Cập nhật thất bại")
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateProfile error", e)
                _uiState.value = _uiState.value.copy(error = "Lỗi kết nối")
            }
        }
    }

    fun updateAvatar(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: return@launch
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val mediaType = mimeType.toMediaTypeOrNull()
                val requestBody = bytes.toRequestBody(mediaType)
                val extension = when (mimeType) {
                    "image/png" -> ".png"
                    "image/webp" -> ".webp"
                    "image/gif" -> ".gif"
                    else -> ".jpg"
                }
                val part = okhttp3.MultipartBody.Part.createFormData("avatar", "avatar$extension", requestBody)

                val response = api.updateAvatar(part)
                if (response.isSuccessful && response.body()?.success == true) {
                    val user = response.body()?.data
                    _uiState.value = _uiState.value.copy(
                        currentUser = user,
                        successMessage = "Đã cập nhật ảnh đại diện"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(error = "Cập nhật ảnh đại diện thất bại")
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateAvatar error", e)
                _uiState.value = _uiState.value.copy(error = "Lỗi kết nối")
            }
        }
    }

    fun getUserById(userId: String) {
        viewModelScope.launch {
            try {
                val response = api.getUserById(userId)
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(viewedProfile = response.body()?.data)
                }
            } catch (e: Exception) {
                Log.e(TAG, "getUserById error", e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    // CONVERSATIONS — Room-first + API sync
    // ═══════════════════════════════════════════════════════

    /**
     * Sync conversations from API → save to Room.
     * Room Flow observer auto-updates UI.
     * Called on init + pull-to-refresh + after receiving messages for other convos.
     */
    fun syncConversationsFromApi() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(conversationsLoading = true)
            try {
                val response = api.getConversations()
                if (response.isSuccessful && response.body()?.success == true) {
                    val convos = response.body()?.data ?: emptyList()
                    val currentUserId = _uiState.value.currentUser?.id

                    // Save to Room — Flow observer will auto-update UI
                    val entities = convos.map { it.toEntity(currentUserId) }
                    conversationDao.insertConversations(entities)

                    Log.d(TAG, "✅ Synced ${entities.size} conversations to Room")
                } else {
                    _uiState.value = _uiState.value.copy(conversationsLoading = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "syncConversationsFromApi error", e)
                _uiState.value = _uiState.value.copy(conversationsLoading = false)
            }
        }
    }

    /** Legacy alias for backward compat */
    fun loadConversations() = syncConversationsFromApi()

    fun createConversation(participantId: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "createConversation: participantId=$participantId")
                val request = CreateConversationRequest(userId = participantId)
                val response = api.createConversation(request)
                Log.d(TAG, "createConversation response: code=${response.code()}, success=${response.body()?.success}")
                if (response.isSuccessful && response.body()?.success == true) {
                    val convo = response.body()?.data
                    if (convo != null) {
                        Log.d(TAG, "createConversation OK: convoId=${convo.id}")
                        // Save to Room immediately
                        val currentUserId = _uiState.value.currentUser?.id
                        conversationDao.insertConversation(convo.toEntity(currentUserId))
                        onCreated(convo.id)
                    } else {
                        Log.e(TAG, "createConversation: data is null")
                        _uiState.value = _uiState.value.copy(error = "Không thể tạo hội thoại")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "createConversation failed: code=${response.code()}, error=$errorBody")
                    _uiState.value = _uiState.value.copy(error = "Không thể tạo hội thoại (${response.code()})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "createConversation error", e)
                _uiState.value = _uiState.value.copy(error = "Lỗi kết nối: ${e.message}")
            }
        }
    }

    fun deleteConversation(conversationId: String, onDeleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                api.deleteConversation(conversationId)
                conversationDao.delete(conversationId)
                messageDao.clearConversation(conversationId)
                _uiState.value = _uiState.value.copy(successMessage = "Đã xóa hội thoại")
                onDeleted?.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "deleteConversation error", e)
                _uiState.value = _uiState.value.copy(error = "Không thể xóa hội thoại")
            }
        }
    }

    fun muteConversation(conversationId: String, isMuted: Boolean) {
        viewModelScope.launch {
            try {
                if (isMuted) api.unmuteConversation(conversationId) else api.muteConversation(conversationId)
                // Assuming Room or API sync will update the state
                syncConversationsFromApi() 
                _uiState.value = _uiState.value.copy(successMessage = if (isMuted) "Bật thông báo thành công" else "Tắt thông báo thành công")
            } catch (e: Exception) {
                Log.e(TAG, "muteConversation error", e)
            }
        }
    }

    fun pinConversation(conversationId: String, isPinned: Boolean) {
        viewModelScope.launch {
            try {
                if (isPinned) api.unpinConversation(conversationId) else api.pinConversation(conversationId)
                syncConversationsFromApi()
                _uiState.value = _uiState.value.copy(successMessage = if (isPinned) "Bỏ ghim thành công" else "Ghim hội thoại thành công")
            } catch (e: Exception) {
                Log.e(TAG, "pinConversation error", e)
            }
        }
    }

    fun blockUser(userId: String, isBlocked: Boolean) {
        viewModelScope.launch {
            try {
                if (isBlocked) api.unblockUser(userId) else api.blockUser(userId)
                _uiState.value = _uiState.value.copy(successMessage = if (isBlocked) "Bỏ chặn thành công" else "Chặn người dùng thành công")
                loadProfile() // Reload profile to update block list
            } catch (e: Exception) {
                Log.e(TAG, "blockUser error", e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    // FRIENDS & SEARCH
    // ═══════════════════════════════════════════════════════

    fun searchUserByPhone(phone: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearchingUser = true, searchUserError = null, searchResultUser = null)
            try {
                val response = api.findByPhone(phone)
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(searchResultUser = response.body()?.data)
                } else {
                    _uiState.value = _uiState.value.copy(searchUserError = "Không tìm thấy người dùng")
                }
            } catch (e: Exception) {
                Log.e(TAG, "searchUserByPhone error", e)
                _uiState.value = _uiState.value.copy(searchUserError = "Lỗi kết nối")
            } finally {
                _uiState.value = _uiState.value.copy(isSearchingUser = false)
            }
        }
    }

    fun clearSearchUser() {
        _uiState.value = _uiState.value.copy(searchResultUser = null, searchUserError = null)
    }

    fun loadFriends() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(friendsLoading = true)
            try {
                val response = api.getFriends()
                if (response.isSuccessful && response.body()?.success == true) {
                    val friends = response.body()?.data?.friends ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        friends = friends,
                        friendCount = friends.size,
                        friendsLoading = false,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(friendsLoading = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadFriends error", e)
                _uiState.value = _uiState.value.copy(friendsLoading = false)
            }
        }
    }

    fun loadOnlineFriends() {
        viewModelScope.launch {
            try {
                val response = api.getOnlineFriends()
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        onlineFriends = response.body()?.data ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadOnlineFriends error", e)
            }
        }
    }

    fun loadFriendRequests() {
        viewModelScope.launch {
            try {
                val response = api.getReceivedRequests()
                if (response.isSuccessful && response.body()?.success == true) {
                    val requests = response.body()?.data?.requests ?: emptyList()
                    _uiState.value = _uiState.value.copy(friendRequests = requests)
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadFriendRequests error", e)
            }
        }
    }

    fun loadSentRequests() {
        viewModelScope.launch {
            try {
                val response = api.getSentRequests()
                if (response.isSuccessful && response.body()?.success == true) {
                    val requests = response.body()?.data?.requests ?: emptyList()
                    _uiState.value = _uiState.value.copy(sentRequests = requests)
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadSentRequests error", e)
            }
        }
    }

    fun loadFriendSuggestions() {
        viewModelScope.launch {
            try {
                val response = api.getFriendSuggestions()
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        friendSuggestions = response.body()?.data ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadFriendSuggestions error", e)
            }
        }
    }

    fun syncContacts(phones: List<String>) {
        viewModelScope.launch {
            try {
                val response = api.syncContacts(SyncContactsRequest(phones))
                if (response.isSuccessful && response.body()?.success == true) {
                    val syncedUsers = response.body()?.data ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        friendSuggestions = syncedUsers + _uiState.value.friendSuggestions,
                        successMessage = "Đã đồng bộ ${syncedUsers.size} liên hệ bạn bè"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "syncContacts error", e)
            }
        }
    }

    fun sendFriendRequest(userId: String) {
        viewModelScope.launch {
            try {
                val response = api.sendFriendRequest(userId)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Đã gửi lời mời",
                        searchResultUser = if (_uiState.value.searchResultUser?.id == userId)
                            _uiState.value.searchResultUser?.copy(friendStatus = "sent")
                        else _uiState.value.searchResultUser,
                        viewedProfile = if (_uiState.value.viewedProfile?.id == userId)
                            _uiState.value.viewedProfile?.copy(friendStatus = "sent")
                        else _uiState.value.viewedProfile,
                        searchResults = _uiState.value.searchResults.map {
                            if (it.id == userId) it.copy(friendStatus = "sent") else it
                        }
                    )
                    loadSentRequests()
                } else {
                    val errorStr = response.errorBody()?.string() ?: ""
                    val errorMsg = if (response.code() == 403) "Không thể gửi lời mời (Bị chặn)"
                    else if (response.code() == 409) "Đã gửi lời mời trước đó hoặc đã là bạn bè"
                    else "Không thể gửi lời mời"
                    _uiState.value = _uiState.value.copy(error = errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "sendFriendRequest error", e)
                _uiState.value = _uiState.value.copy(error = "Lỗi gửi lời mời")
            }
        }
    }

    fun cancelFriendRequest(userId: String) {
        viewModelScope.launch {
            try {
                val response = api.cancelRequestByUserId(userId)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Đã hủy yêu cầu",
                        searchResultUser = if (_uiState.value.searchResultUser?.id == userId)
                            _uiState.value.searchResultUser?.copy(friendStatus = "none")
                        else _uiState.value.searchResultUser,
                        viewedProfile = if (_uiState.value.viewedProfile?.id == userId)
                            _uiState.value.viewedProfile?.copy(friendStatus = "none")
                        else _uiState.value.viewedProfile,
                        searchResults = _uiState.value.searchResults.map {
                            if (it.id == userId) it.copy(friendStatus = "none") else it
                        }
                    )
                    loadSentRequests()
                }
            } catch (e: Exception) {
                Log.e(TAG, "cancelFriendRequest error", e)
                _uiState.value = _uiState.value.copy(error = "Lỗi hủy yêu cầu")
            }
        }
    }

    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            try {
                api.acceptRequest(requestId)
                _uiState.value = _uiState.value.copy(successMessage = "Đã chấp nhận lời mời")
                loadFriendRequests()
                loadFriends()
            } catch (e: Exception) {
                Log.e(TAG, "acceptFriendRequest error", e)
            }
        }
    }

    fun acceptFriendRequestByUserId(userId: String) {
        viewModelScope.launch {
            try {
                api.acceptRequestByUserId(userId)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Đã chấp nhận lời mời",
                    searchResultUser = if (_uiState.value.searchResultUser?.id == userId)
                        _uiState.value.searchResultUser?.copy(friendStatus = "friend")
                    else _uiState.value.searchResultUser,
                    viewedProfile = if (_uiState.value.viewedProfile?.id == userId)
                        _uiState.value.viewedProfile?.copy(friendStatus = "friend")
                    else _uiState.value.viewedProfile,
                    searchResults = _uiState.value.searchResults.map {
                        if (it.id == userId) it.copy(friendStatus = "friend") else it
                    }
                )
                loadFriendRequests()
                loadFriends()
            } catch (e: Exception) {
                Log.e(TAG, "acceptFriendRequestByUserId error", e)
            }
        }
    }

    fun rejectFriendRequest(requestId: String) {
        viewModelScope.launch {
            try {
                api.rejectRequest(requestId)
                loadFriendRequests()
            } catch (e: Exception) {
                Log.e(TAG, "rejectFriendRequest error", e)
            }
        }
    }

    fun cancelSentRequest(requestId: String) {
        viewModelScope.launch {
            try {
                api.cancelRequest(requestId)
                loadSentRequests()
            } catch (e: Exception) {
                Log.e(TAG, "cancelSentRequest error", e)
            }
        }
    }

    fun unfriend(userId: String) {
        viewModelScope.launch {
            try {
                api.unfriend(userId)
                _uiState.value = _uiState.value.copy(successMessage = "Đã hủy kết bạn")
                loadFriends()
            } catch (e: Exception) {
                Log.e(TAG, "unfriend error", e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    // SEARCH
    // ═══════════════════════════════════════════════════════

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), searchLoading = false)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(searchLoading = true)
            try {
                val response = api.searchUsers(query)
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        searchResults = response.body()?.data ?: emptyList(),
                        searchLoading = false,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(searchLoading = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "searchUsers error", e)
                _uiState.value = _uiState.value.copy(searchLoading = false)
            }
        }
    }

    fun findByPhone(phone: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(findByPhoneLoading = true, foundUser = null)
            try {
                val response = api.findByPhone(phone)
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        foundUser = response.body()?.data,
                        findByPhoneLoading = false,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        findByPhoneLoading = false,
                        error = "Không tìm thấy người dùng"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "findByPhone error", e)
                _uiState.value = _uiState.value.copy(findByPhoneLoading = false)
            }
        }
    }

    fun clearSearchResults() {
        _uiState.value = _uiState.value.copy(searchResults = emptyList(), foundUser = null)
    }

    // ═══════════════════════════════════════════════════════
    // MESSAGES — Room-first + API delta sync
    // ═══════════════════════════════════════════════════════

    /**
     * Load messages for a conversation:
     * 1. Start observing Room (cached messages appear instantly)
     * 2. Sync from API in background (fetch only newer messages if cache exists)
     *
     * This is the key "Zalo-like" behavior.
     */
    fun loadMessages(conversationId: String) {
        // If we're re-entering the same conversation, no-op (messages already in state)
        val prevConvId = _uiState.value.currentConversationId
        val alreadyHasCachedMessages = prevConvId == conversationId && _uiState.value.messages.isNotEmpty()

        _uiState.value = _uiState.value.copy(
            currentConversationId = conversationId,
            typingUser = null,
            // Clear messages ONLY if we are switching to a new conversation
            messages = if (prevConvId == conversationId) _uiState.value.messages else emptyList(),
            // Ensure loading spinner shows up IF we switch until cache is loaded
            messagesLoading = if (prevConvId == conversationId) _uiState.value.messagesLoading else true
        )

        // Step 1: Observe from Room — UI gets cached messages immediately
        observeMessagesFromRoom(conversationId)

        // Step 2: Sync from API in background conditionally
        viewModelScope.launch {
            val cachedCount = messageDao.getMessageCount(conversationId)
            val currentConversation = _uiState.value.conversations.find { it.id == conversationId }
            val myParticipant = currentConversation?.participants?.find { it.user?.id == _uiState.value.currentUser?.id }
            val unreadCountRaw = myParticipant?.unreadCount ?: 0
            val hasUnread = unreadCountRaw > 0

            Log.d(TAG, "loadMessages check: cachedCount=$cachedCount, unreadCountRaw=$unreadCountRaw, hasUnread=$hasUnread, alreadyCached=$alreadyHasCachedMessages")

            if (cachedCount == 0 && !alreadyHasCachedMessages) {
                // No cache at all — show loading spinner, do full fetch
                Log.d(TAG, "loadMessages: Fetching API because cachedCount is 0")
                _uiState.value = _uiState.value.copy(messagesLoading = true)
                syncMessagesFromApi(conversationId)
            } else if (hasUnread) {
                // Has cache, but has unread messages we might be missing
                // DON'T show loading — user sees cached msgs while new ones sync silently
                Log.d(TAG, "loadMessages: Background sync for unread messages")
                syncMessagesFromApi(conversationId)
            } else {
                // Fully cached and read. Skip API fetch to save bandwidth & prevent UI jitter
                Log.d(TAG, "loadMessages: Skipping API sync. $cachedCount local msgs, 0 unread.")
            }
        }
    }

    /**
     * Sync messages from API → save to Room.
     * Room Flow observer auto-updates UI with merged data.
     */
    private suspend fun syncMessagesFromApi(conversationId: String) {
        try {
            Log.d(TAG, "syncMessagesFromApi: conversationId=$conversationId")
            val response = api.getMessages(conversationId)
            if (response.isSuccessful && response.body()?.success == true) {
                val msgs = response.body()?.data?.messages ?: emptyList()
                Log.d(TAG, "syncMessagesFromApi: ${msgs.size} messages from API")

                // Convert API models → Room entities and save
                val entities = msgs.map { it.toEntity(conversationId) }
                messageDao.insertMessages(entities) // REPLACE = upsert

                Log.d(TAG, "✅ Synced ${entities.size} messages to Room for conv=$conversationId")
                
                // Force loader to hide even if Room doesn't emit (e.g. empty list)
                _uiState.value = _uiState.value.copy(messagesLoading = false)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "syncMessagesFromApi failed: code=${response.code()}, error=$errorBody")
                if (messageDao.getMessageCount(conversationId) == 0) {
                    _uiState.value = _uiState.value.copy(
                        messagesLoading = false,
                        error = "Không thể tải tin nhắn (${response.code()})"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncMessagesFromApi error", e)
            if (messageDao.getMessageCount(conversationId) == 0) {
                _uiState.value = _uiState.value.copy(
                    messagesLoading = false,
                    error = "Lỗi tải tin nhắn"
                )
            }
            // If cache exists, silently fail — user still sees cached messages
        }
    }

    /**
     * Send message via Socket (primary) with optimistic UI.
     * Optimistic message saved to Room immediately with SENDING status.
     * Backend socket handler processes it and sends ACK back → Room updates to SENT.
     */
    fun sendMessage(conversationId: String, content: String, type: String = "text") {
        val clientMessageId = java.util.UUID.randomUUID().toString()
        val currentUser = _uiState.value.currentUser ?: return

        // Create optimistic entity and save to Room
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date())

        val optimisticEntity = MessageEntity(
            messageId = clientMessageId, // temporary ID, replaced on ACK
            conversationId = conversationId,
            senderId = currentUser.id,
            senderDisplayName = currentUser.displayName,
            senderAvatarUrl = currentUser.avatar?.url,
            senderAvatarThumbnailUrl = currentUser.avatar?.thumbnailUrl,
            content = content,
            type = type,
            createdAt = timestamp,
            deliveryStatus = "sending",
            clientMessageId = clientMessageId,
            isSynced = false,
        )

        viewModelScope.launch {
            // Save to Room → Flow observer auto-adds to UI
            messageDao.insertMessage(optimisticEntity)

            // Update conversation's last message in Room
            conversationDao.updateLastMessage(
                conversationId = conversationId,
                preview = content,
                timestamp = timestamp,
                senderId = currentUser.id,
                senderName = currentUser.displayName
            )
        }

        // Send via Socket (fast path)
        if (SocketManager.isConnected()) {
            SocketManager.emitSendMessage(clientMessageId, conversationId, content, type)
        } else {
            // Fallback to REST if socket is disconnected
            viewModelScope.launch {
                try {
                    val request = SendMessageRequest(
                        content = content,
                        type = type,
                        clientMessageId = clientMessageId
                    )
                    val response = api.sendMessage(conversationId, request)
                    if (response.isSuccessful && response.body()?.success == true) {
                        val serverMsg = response.body()?.data
                        if (serverMsg != null) {
                            messageDao.updateOptimisticMessage(
                                clientMessageId,
                                serverMsg.messageId ?: serverMsg.id ?: clientMessageId,
                                serverMsg.createdAt ?: timestamp
                            )
                        }
                    }
                    syncConversationsFromApi()
                } catch (e: Exception) {
                    Log.e(TAG, "sendMessage REST fallback error", e)
                    _uiState.value = _uiState.value.copy(error = "Gửi tin nhắn thất bại")
                }
            }
        }
    }

    /**
     * Mark conversation as read — emits socket event + resets unread badge in Room
     */
    fun markConversationAsRead(conversationId: String) {
        val lastMsg = _uiState.value.messages.lastOrNull()
        SocketManager.emitMarkRead(conversationId, lastMsg?.messageId)
        viewModelScope.launch {
            conversationDao.resetUnread(conversationId)
        }
    }

    /**
     * Clear current conversation state when leaving chat screen.
     * Key difference from old code: messages stay in Room!
     * Next time user opens this conversation, cached messages appear instantly.
     */
    fun leaveConversation() {
        // Stop typing if we were
        val convId = _uiState.value.currentConversationId
        if (isTyping && convId != null) {
            SocketManager.emitTypingStop(convId)
            isTyping = false
        }

        // Cancel Room observer for this conversation
        messagesObserverJob?.cancel()
        messagesObserverJob = null

        // KEY: Do NOT clear messages from state!
        // Messages remain in memory so if user quickly re-opens the same
        // conversation, they see cached messages immediately with zero delay.
        // Room data remains as source-of-truth for the next observeMessagesFromRoom.
        _uiState.value = _uiState.value.copy(
            currentConversationId = null,
            // messages kept in state for instant re-entry
            typingUser = null,
        )
    }

    /**
     * Handle text change in composer — emits typing events with debounce
     */
    fun onTyping(conversationId: String) {
        if (!isTyping) {
            isTyping = true
            SocketManager.emitTypingStart(conversationId)
        }
        // Reset the stop-typing timer
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(2000)
            isTyping = false
            SocketManager.emitTypingStop(conversationId)
        }
    }

    fun recallMessage(messageId: String) {
        viewModelScope.launch {
            try {
                api.recallMessage(messageId)
                // Delete from Room
                messageDao.deleteMessage(messageId)
            } catch (e: Exception) {
                Log.e(TAG, "recallMessage error", e)
            }
        }
    }

    /**
     * Send an image message: upload to /api/upload/image, then send via socket
     */
    fun sendImageMessage(context: android.content.Context, conversationId: String, uri: android.net.Uri) {
        val clientMessageId = java.util.UUID.randomUUID().toString()
        val currentUser = _uiState.value.currentUser ?: return

        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date())

        // Optimistic entity saved to Room
        val optimisticEntity = MessageEntity(
            messageId = clientMessageId,
            conversationId = conversationId,
            senderId = currentUser.id,
            senderDisplayName = currentUser.displayName,
            senderAvatarUrl = currentUser.avatar?.url,
            senderAvatarThumbnailUrl = currentUser.avatar?.thumbnailUrl,
            content = uri.toString(),
            type = "image",
            createdAt = timestamp,
            deliveryStatus = "sending",
            clientMessageId = clientMessageId,
            isSynced = false,
        )

        viewModelScope.launch {
            messageDao.insertMessage(optimisticEntity)

            // Update conversation's last message in Room
            conversationDao.updateLastMessage(
                conversationId = conversationId,
                preview = "\uD83D\uDCF7 Đang gửi ảnh...",
                timestamp = timestamp,
                senderId = currentUser.id,
                senderName = currentUser.displayName
            )

            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: return@launch
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val mediaType = mimeType.toMediaTypeOrNull()
                val requestBody = bytes.toRequestBody(mediaType)
                val ext = when (mimeType) {
                    "image/png" -> "image.png"
                    "image/webp" -> "image.webp"
                    "image/gif" -> "image.gif"
                    else -> "image.jpg"
                }
                val part = okhttp3.MultipartBody.Part.createFormData("image", ext, requestBody)

                val uploadResponse = api.uploadChatImage(part)
                if (uploadResponse.isSuccessful && uploadResponse.body()?.success == true) {
                    val result = uploadResponse.body()?.data
                    val imageUrl = result?.url ?: return@launch

                    val attachmentJson = org.json.JSONObject().apply {
                        put("url", imageUrl)
                        if (result.thumbnailUrl != null) put("thumbnailUrl", result.thumbnailUrl)
                        put("fileType", "image")
                        val dims = org.json.JSONObject().apply {
                            put("width", result.width)
                            put("height", result.height)
                        }
                        put("dimensions", dims)
                        put("fileSize", result.size)
                    }
                    val attachmentsArray = org.json.JSONArray().put(attachmentJson)

                    if (SocketManager.isConnected()) {
                        SocketManager.emitSendMessage(clientMessageId, conversationId, imageUrl, "image", null, attachmentsArray)
                    } else {
                        val attachment = AttachmentData(
                            url = imageUrl,
                            thumbnailUrl = result.thumbnailUrl,
                            fileType = "image",
                            dimensions = com.example.alohi.data.model.Dimensions(width = result.width, height = result.height),
                            fileSize = result.size,
                        )
                        val request = SendMessageRequest(content = imageUrl, type = "image", clientMessageId = clientMessageId, attachments = listOf(attachment))
                        val msgResponse = api.sendMessage(conversationId, request)
                        if (msgResponse.isSuccessful) {
                            val serverMsg = msgResponse.body()?.data
                            val serverId = serverMsg?.messageId ?: serverMsg?.id ?: clientMessageId
                            messageDao.updateOptimisticMessage(clientMessageId, serverId, timestamp)
                        } else {
                            messageDao.deleteMessage(clientMessageId)
                        }
                        syncConversationsFromApi()
                    }
                } else {
                    _uiState.value = _uiState.value.copy(error = "Upload file API lỗi: ${uploadResponse.code()}")
                    messageDao.deleteMessage(clientMessageId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "sendImageMessage error", e)
                _uiState.value = _uiState.value.copy(error = "Văng ngoại lệ: ${e.message}")
                messageDao.deleteMessage(clientMessageId)
            }
        }
    }

    /**
     * Send a video message: upload to /api/upload/video, then send via REST
     */
    fun sendVideoMessage(context: android.content.Context, conversationId: String, uri: android.net.Uri) {
        val clientMessageId = java.util.UUID.randomUUID().toString()
        val currentUser = _uiState.value.currentUser ?: return

        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date())

        val optimisticEntity = MessageEntity(
            messageId = clientMessageId,
            conversationId = conversationId,
            senderId = currentUser.id,
            senderDisplayName = currentUser.displayName,
            senderAvatarUrl = currentUser.avatar?.url,
            senderAvatarThumbnailUrl = currentUser.avatar?.thumbnailUrl,
            content = uri.toString(),
            type = "video",
            createdAt = timestamp,
            deliveryStatus = "sending",
            clientMessageId = clientMessageId,
            isSynced = false,
        )

        viewModelScope.launch {
            messageDao.insertMessage(optimisticEntity)

            // Update conversation's last message in Room
            conversationDao.updateLastMessage(
                conversationId = conversationId,
                preview = "\uD83C\uDFAC Đang gửi video...",
                timestamp = timestamp,
                senderId = currentUser.id,
                senderName = currentUser.displayName
            )

            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: return@launch
                val mimeType = context.contentResolver.getType(uri) ?: "video/mp4"
                val mediaType = mimeType.toMediaTypeOrNull()
                val requestBody = bytes.toRequestBody(mediaType)
                val ext = when (mimeType) {
                    "video/quicktime" -> "video.mov"
                    "video/x-matroska" -> "video.mkv"
                    else -> "video.mp4"
                }
                val part = okhttp3.MultipartBody.Part.createFormData("video", ext, requestBody)

                val uploadResponse = api.uploadChatVideo(part)
                if (uploadResponse.isSuccessful && uploadResponse.body()?.success == true) {
                    val result = uploadResponse.body()?.data
                    val videoUrl = result?.url ?: return@launch

                    val attachmentJson = org.json.JSONObject().apply {
                        put("url", videoUrl)
                        if (result.thumbnailUrl != null) put("thumbnailUrl", result.thumbnailUrl)
                        put("fileType", "video")
                        if (result.duration != null) put("duration", result.duration)
                        val dims = org.json.JSONObject().apply {
                            put("width", result.width)
                            put("height", result.height)
                        }
                        put("dimensions", dims)
                        put("fileSize", result.size)
                    }
                    val attachmentsArray = org.json.JSONArray().put(attachmentJson)

                    if (SocketManager.isConnected()) {
                        SocketManager.emitSendMessage(clientMessageId, conversationId, videoUrl, "video", null, attachmentsArray)
                    } else {
                        val attachment = AttachmentData(
                            url = videoUrl,
                            thumbnailUrl = result.thumbnailUrl,
                            fileType = "video",
                            duration = result.duration?.toInt(),
                            dimensions = com.example.alohi.data.model.Dimensions(width = result.width, height = result.height),
                            fileSize = result.size,
                        )
                        val request = SendMessageRequest(
                            content = videoUrl,
                            type = "video",
                            clientMessageId = clientMessageId,
                            attachments = listOf(attachment),
                        )
                        val msgResponse = api.sendMessage(conversationId, request)
                        if (msgResponse.isSuccessful) {
                            val serverMsg = msgResponse.body()?.data
                            val serverId = serverMsg?.messageId ?: serverMsg?.id ?: clientMessageId
                            messageDao.updateOptimisticMessage(clientMessageId, serverId, timestamp)
                        } else {
                            messageDao.deleteMessage(clientMessageId)
                        }
                        syncConversationsFromApi()
                    }
                    syncConversationsFromApi()
                } else {
                    _uiState.value = _uiState.value.copy(error = "Upload video thất bại")
                    messageDao.deleteMessage(clientMessageId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "sendVideoMessage error", e)
                _uiState.value = _uiState.value.copy(error = "Gửi video thất bại")
                messageDao.deleteMessage(clientMessageId)
            }
        }
    }

    /**
     * Send a file message: upload to /api/upload/file, then send via REST
     */
    fun sendFileMessage(context: android.content.Context, conversationId: String, uri: android.net.Uri) {
        val clientMessageId = java.util.UUID.randomUUID().toString()
        val currentUser = _uiState.value.currentUser ?: return

        val fileName = uri.lastPathSegment ?: "file"
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date())

        val optimisticEntity = MessageEntity(
            messageId = clientMessageId,
            conversationId = conversationId,
            senderId = currentUser.id,
            senderDisplayName = currentUser.displayName,
            senderAvatarUrl = currentUser.avatar?.url,
            senderAvatarThumbnailUrl = currentUser.avatar?.thumbnailUrl,
            content = fileName,
            type = "file",
            createdAt = timestamp,
            deliveryStatus = "sending",
            clientMessageId = clientMessageId,
            isSynced = false,
        )

        viewModelScope.launch {
            messageDao.insertMessage(optimisticEntity)

            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: return@launch
                val mediaType = (context.contentResolver.getType(uri) ?: "application/octet-stream").toMediaTypeOrNull()
                val requestBody = bytes.toRequestBody(mediaType)
                val part = okhttp3.MultipartBody.Part.createFormData("file", fileName, requestBody)

                val uploadResponse = api.uploadFile(part)
                if (uploadResponse.isSuccessful && uploadResponse.body()?.success == true) {
                    val result = uploadResponse.body()?.data
                    val fileUrl = result?.url ?: return@launch

                    val attachmentJson = org.json.JSONObject().apply {
                        put("url", fileUrl)
                        put("fileType", "file")
                        put("fileName", result.originalName ?: fileName)
                        put("fileSize", result.size)
                    }
                    val attachmentsArray = org.json.JSONArray().put(attachmentJson)

                    if (SocketManager.isConnected()) {
                        SocketManager.emitSendMessage(clientMessageId, conversationId, fileUrl, "file", null, attachmentsArray)
                    } else {
                        val attachment = AttachmentData(
                            url = fileUrl,
                            fileType = "file",
                            fileName = result.originalName ?: fileName,
                            fileSize = result.size,
                        )
                        val request = SendMessageRequest(
                            content = fileUrl,
                            type = "file",
                            clientMessageId = clientMessageId,
                            attachments = listOf(attachment),
                        )
                        val msgResponse = api.sendMessage(conversationId, request)
                        if (msgResponse.isSuccessful) {
                            val serverMsg = msgResponse.body()?.data
                            val serverId = serverMsg?.messageId ?: serverMsg?.id ?: clientMessageId
                            messageDao.updateOptimisticMessage(clientMessageId, serverId, timestamp)
                        } else {
                            messageDao.deleteMessage(clientMessageId)
                        }
                    }
                    syncConversationsFromApi()
                } else {
                    _uiState.value = _uiState.value.copy(error = "Upload file thất bại")
                    messageDao.deleteMessage(clientMessageId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "sendFileMessage error", e)
                _uiState.value = _uiState.value.copy(error = "Gửi file thất bại")
                messageDao.deleteMessage(clientMessageId)
            }
        }
    }

    /**
     * Send an audio/voice message: upload to /api/upload/audio, then send via REST
     */
    fun sendVoiceMessage(conversationId: String, file: java.io.File, durationMs: Long) {
        val clientMessageId = java.util.UUID.randomUUID().toString()
        val currentUser = _uiState.value.currentUser ?: return

        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date())

        val optimisticEntity = MessageEntity(
            messageId = clientMessageId,
            conversationId = conversationId,
            senderId = currentUser.id,
            senderDisplayName = currentUser.displayName,
            senderAvatarUrl = currentUser.avatar?.url,
            senderAvatarThumbnailUrl = currentUser.avatar?.thumbnailUrl,
            content = "Voice message",
            type = "audio",
            createdAt = timestamp,
            deliveryStatus = "sending",
            clientMessageId = clientMessageId,
            isSynced = false,
        )

        viewModelScope.launch {
            messageDao.insertMessage(optimisticEntity)

            try {
                val mediaType = "audio/mp4".toMediaTypeOrNull()
                val requestBody = file.asRequestBody(mediaType)
                val part = okhttp3.MultipartBody.Part.createFormData("audio", file.name, requestBody)

                val uploadResponse = api.uploadVoiceAudio(part)
                if (uploadResponse.isSuccessful && uploadResponse.body()?.success == true) {
                    val result = uploadResponse.body()?.data
                    val audioUrl = result?.url ?: return@launch

                    val attachment = AttachmentData(
                        url = audioUrl,
                        fileType = "audio",
                        fileSize = result.size,
                        duration = (durationMs / 1000).toInt()
                    )

                    val request = SendMessageRequest(
                        content = audioUrl,
                        type = "audio",
                        clientMessageId = clientMessageId,
                        attachments = listOf(attachment),
                    )

                    val msgResponse = api.sendMessage(conversationId, request)
                    if (msgResponse.isSuccessful) {
                        val serverMsg = msgResponse.body()?.data
                        val serverId = serverMsg?.messageId ?: serverMsg?.id ?: clientMessageId
                        messageDao.deleteMessage(clientMessageId)
                        messageDao.insertMessage(optimisticEntity.copy(
                            messageId = serverId,
                            content = audioUrl, // use URL as content
                            deliveryStatus = "sent",
                            isSynced = true,
                        ))
                    }
                    syncConversationsFromApi()
                } else {
                    _uiState.value = _uiState.value.copy(error = "Upload audio thất bại")
                    messageDao.deleteMessage(clientMessageId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "sendVoiceMessage error", e)
                _uiState.value = _uiState.value.copy(error = "Gửi audio thất bại")
                messageDao.deleteMessage(clientMessageId)
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    // MESSAGE ACTIONS (Recall / Delete)
    // ═══════════════════════════════════════════════════════

    /**
     * Recall (unsend) a message — removes it for everyone (within 24h window)
     */
    fun recallMessage(messageId: String, conversationId: String) {
        viewModelScope.launch {
            try {
                val response = api.recallMessage(messageId)
                if (response.isSuccessful) {
                    messageDao.deleteMessage(messageId)
                    syncConversationsFromApi()
                    Log.d(TAG, "✅ Message recalled: $messageId")
                } else {
                    val errorBody = response.errorBody()?.string()
                    _uiState.value = _uiState.value.copy(error = "Thu hồi thất bại: ${response.code()}")
                    Log.e(TAG, "recallMessage failed: $errorBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "recallMessage error", e)
                _uiState.value = _uiState.value.copy(error = "Thu hồi thất bại: ${e.message}")
            }
        }
    }

    /**
     * Delete a message for self only — other participants can still see it
     */
    fun deleteMessageForSelf(messageId: String) {
        viewModelScope.launch {
            try {
                val response = api.deleteMessage(messageId)
                if (response.isSuccessful) {
                    messageDao.deleteMessage(messageId)
                    Log.d(TAG, "✅ Message deleted for self: $messageId")
                } else {
                    _uiState.value = _uiState.value.copy(error = "Xóa thất bại")
                }
            } catch (e: Exception) {
                Log.e(TAG, "deleteMessage error", e)
                _uiState.value = _uiState.value.copy(error = "Xóa thất bại: ${e.message}")
            }
        }
    }

    /**
     * Copy message text to clipboard
     */
    fun copyMessageToClipboard(context: android.content.Context, text: String) {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("AloHi Message", text)
        clipboard.setPrimaryClip(clip)
    }

    // ═══════════════════════════════════════════════════════
    // SESSIONS / DEVICES
    // ═══════════════════════════════════════════════════════

    fun loadSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(sessionsLoading = true)
            try {
                val response = ApiClient.authApi.getSessions()
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(sessions = response.body()?.data ?: emptyList())
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadSessions error", e)
            } finally {
                _uiState.value = _uiState.value.copy(sessionsLoading = false)
            }
        }
    }

    fun logoutSession(sessionId: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.authApi.logoutSession(sessionId)
                if (response.isSuccessful) {
                    loadSessions() // Reload
                }
            } catch (e: Exception) {
                Log.e(TAG, "logoutSession error", e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun refresh() {
        loadProfile()
        syncConversationsFromApi()
        loadFriends()
        loadFriendRequests()
        loadOnlineFriends()
    }

    /**
     * Call on logout — clear all local cache and un-init CallKit.
     */
    fun clearLocalCache() {
        ZegoUIKitPrebuiltCallService.unInit()
        viewModelScope.launch {
            messageDao.clearAll()
            conversationDao.clearAll()
        }
    }
    // ═══════════════════════════════════════════════════════
    // MESSAGE REPLY
    // ═══════════════════════════════════════════════════════

    fun setReplyingTo(message: MessageItem?) {
        _uiState.value = _uiState.value.copy(replyingToMessage = message)
    }

    fun sendReplyMessage(conversationId: String, content: String) {
        val replyTo = _uiState.value.replyingToMessage ?: return
        val replyToId = replyTo.messageId ?: replyTo.id ?: return
        val replyToSenderName = replyTo.sender?.displayName ?: ""
        val replyToContent = replyTo.displayText()

        // Clear reply state immediately
        _uiState.value = _uiState.value.copy(replyingToMessage = null)

        // Send via socket with replyTo data
        val clientMsgId = java.util.UUID.randomUUID().toString()
        val currentUserId = _uiState.value.currentUser?.id ?: return
        val currentUserName = _uiState.value.currentUser?.displayName ?: ""

        // Save optimistic message to Room
        viewModelScope.launch {
            val entity = MessageEntity(
                messageId = clientMsgId,
                conversationId = conversationId,
                senderId = currentUserId,
                senderDisplayName = currentUserName,
                content = content,
                type = "text",
                deliveryStatus = "sending",
                createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                    .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                    .format(java.util.Date()),
                clientMessageId = clientMsgId,
                isSynced = false,
            )
            messageDao.insertMessage(entity)

            // Send via socket with reply info
            val replyJson = JSONObject().apply {
                put("messageId", replyToId)
                put("content", replyToContent)
                put("sender", JSONObject().apply {
                    put("displayName", replyToSenderName)
                })
            }
            SocketManager.emitSendMessage(
                clientMessageId = clientMsgId,
                conversationId = conversationId,
                content = content,
                type = "text",
                replyTo = replyJson,
            )
        }
    }

    // ═══════════════════════════════════════════════════════
    // EMOJI REACTIONS
    // ═══════════════════════════════════════════════════════

    fun addReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            try {
                val response = api.addReaction(messageId, ReactMessageRequest(emoji))
                if (response.isSuccessful) {
                    // Refresh messages to get updated reactions
                    val convoId = _uiState.value.currentConversationId
                    if (convoId != null) loadMessages(convoId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "addReaction error", e)
            }
        }
    }

    fun removeReaction(messageId: String) {
        viewModelScope.launch {
            try {
                val response = api.removeReaction(messageId)
                if (response.isSuccessful) {
                    val convoId = _uiState.value.currentConversationId
                    if (convoId != null) loadMessages(convoId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "removeReaction error", e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    // FORWARD MESSAGE
    // ═══════════════════════════════════════════════════════

    fun forwardMessage(messageId: String, targetConversationIds: List<String>) {
        viewModelScope.launch {
            try {
                val response = api.forwardMessage(messageId, ForwardMessageRequest(targetConversationIds))
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Đã chuyển tiếp đến ${targetConversationIds.size} hội thoại"
                    )
                    syncConversationsFromApi()
                } else {
                    _uiState.value = _uiState.value.copy(error = "Chuyển tiếp thất bại")
                }
            } catch (e: Exception) {
                Log.e(TAG, "forwardMessage error", e)
                _uiState.value = _uiState.value.copy(error = "Lỗi kết nối")
            }
        }
    }


    // ═══════════════════════════════════════════════════════
    // GROUP MANAGEMENT
    // ═══════════════════════════════════════════════════════

    fun createGroup(name: String, memberIds: List<String>, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val request = CreateGroupRequest(name = name, members = memberIds)
                val response = api.createGroup(request)
                if (response.isSuccessful && response.body()?.success == true) {
                    val convo = response.body()?.data
                    if (convo != null) {
                        val currentUserId = _uiState.value.currentUser?.id
                        conversationDao.insertConversation(convo.toEntity(currentUserId))
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "Đã tạo nhóm \"$name\""
                        )
                        onCreated(convo.id)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "createGroup failed: $errorBody")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Tạo nhóm thất bại (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "createGroup error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Lỗi kết nối: ${e.message}"
                )
            }
        }
    }

    fun addGroupMembers(groupId: String, memberIds: List<String>) {
        viewModelScope.launch {
            try {
                val response = api.addGroupMembers(groupId, AddMembersRequest(memberIds))
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Đã thêm ${memberIds.size} thành viên"
                    )
                    syncConversationsFromApi()
                } else {
                    _uiState.value = _uiState.value.copy(error = "Thêm thành viên thất bại")
                }
            } catch (e: Exception) {
                Log.e(TAG, "addGroupMembers error", e)
                _uiState.value = _uiState.value.copy(error = "Lỗi kết nối")
            }
        }
    }

    fun removeGroupMember(groupId: String, userId: String) {
        viewModelScope.launch {
            try {
                val response = api.removeGroupMember(groupId, userId)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(successMessage = "Đã xóa thành viên")
                    syncConversationsFromApi()
                } else {
                    _uiState.value = _uiState.value.copy(error = "Xóa thành viên thất bại")
                }
            } catch (e: Exception) {
                Log.e(TAG, "removeGroupMember error", e)
            }
        }
    }

    fun leaveGroup(groupId: String, onLeft: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.leaveGroup(groupId)
                if (response.isSuccessful) {
                    conversationDao.delete(groupId)
                    messageDao.clearConversation(groupId)
                    _uiState.value = _uiState.value.copy(successMessage = "Đã rời nhóm")
                    onLeft()
                } else {
                    _uiState.value = _uiState.value.copy(error = "Rời nhóm thất bại")
                }
            } catch (e: Exception) {
                Log.e(TAG, "leaveGroup error", e)
                _uiState.value = _uiState.value.copy(error = "Lỗi kết nối")
            }
        }
    }

    fun updateGroupInfo(groupId: String, name: String?, description: String?) {
        viewModelScope.launch {
            try {
                val response = api.updateGroup(groupId, UpdateGroupRequest(name, description))
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(successMessage = "Đã cập nhật nhóm")
                    syncConversationsFromApi()
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateGroupInfo error", e)
            }
        }
    }

    fun changeGroupRole(groupId: String, userId: String, role: String) {
        viewModelScope.launch {
            try {
                val response = api.changeGroupRole(groupId, userId, ChangeRoleRequest(role))
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(successMessage = "Đã thay đổi vai trò")
                    syncConversationsFromApi()
                }
            } catch (e: Exception) {
                Log.e(TAG, "changeGroupRole error", e)
            }
        }
    }

    fun dissolveGroup(groupId: String, onDissolved: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.dissolveGroup(groupId)
                if (response.isSuccessful) {
                    conversationDao.delete(groupId)
                    messageDao.clearConversation(groupId)
                    _uiState.value = _uiState.value.copy(successMessage = "Đã giải tán nhóm")
                    onDissolved()
                }
            } catch (e: Exception) {
                Log.e(TAG, "dissolveGroup error", e)
            }
        }
    }
}
