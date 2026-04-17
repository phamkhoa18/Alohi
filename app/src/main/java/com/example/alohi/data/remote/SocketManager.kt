package com.example.alohi.data.remote

import android.util.Log
import com.example.alohi.data.local.TokenManager
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URI

/**
 * AloHi Socket Manager — Full Realtime Event Handling
 *
 * Client → Server:
 *   message:send, message:ack, message:read, typing:start, typing:stop
 *
 * Server → Client:
 *   message:sent, message:receive, message:delivered,
 *   message:read_receipt, message:recalled, typing:update,
 *   friend:request_received, friend:request_accepted,
 *   friend:online, friend:offline
 */
object SocketManager {
    private const val TAG = "SocketManager"
    private var socket: Socket? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null
    private var socketRetryCount = 0
    private const val MAX_SOCKET_RETRIES = 3

    // The exact same backend IP used for Retrofit ApiClient
    private const val SOCKET_SERVER_URL = "http://172.16.1.76:3000"

    // ═══════════════════════════════════════════════════════
    // SharedFlows — broadcast realtime events to ViewModels
    // ═══════════════════════════════════════════════════════

    // Friend events
    private val _friendRequestReceived = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val friendRequestReceived = _friendRequestReceived.asSharedFlow()

    private val _friendRequestAccepted = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val friendRequestAccepted = _friendRequestAccepted.asSharedFlow()

    // Message events
    private val _messageReceived = MutableSharedFlow<JSONObject>(extraBufferCapacity = 20)
    val messageReceived = _messageReceived.asSharedFlow()

    private val _messageSentAck = MutableSharedFlow<JSONObject>(extraBufferCapacity = 10)
    val messageSentAck = _messageSentAck.asSharedFlow()

    private val _messageDelivered = MutableSharedFlow<JSONObject>(extraBufferCapacity = 10)
    val messageDelivered = _messageDelivered.asSharedFlow()

    private val _messageReadReceipt = MutableSharedFlow<JSONObject>(extraBufferCapacity = 10)
    val messageReadReceipt = _messageReadReceipt.asSharedFlow()

    private val _messageRecalled = MutableSharedFlow<JSONObject>(extraBufferCapacity = 5)
    val messageRecalled = _messageRecalled.asSharedFlow()

    // Typing indicator
    private val _typingUpdate = MutableSharedFlow<JSONObject>(extraBufferCapacity = 10)
    val typingUpdate = _typingUpdate.asSharedFlow()

    // Presence
    private val _friendOnline = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val friendOnline = _friendOnline.asSharedFlow()

    private val _friendOffline = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val friendOffline = _friendOffline.asSharedFlow()

    // ── Call Events (WebRTC) ──
    private val _callIncoming = MutableSharedFlow<JSONObject>(extraBufferCapacity = 5)
    val callIncoming = _callIncoming.asSharedFlow()

    private val _callAccepted = MutableSharedFlow<JSONObject>(extraBufferCapacity = 5)
    val callAccepted = _callAccepted.asSharedFlow()

    private val _callRejected = MutableSharedFlow<JSONObject>(extraBufferCapacity = 5)
    val callRejected = _callRejected.asSharedFlow()

    private val _callEnded = MutableSharedFlow<JSONObject>(extraBufferCapacity = 5)
    val callEnded = _callEnded.asSharedFlow()

    private val _callTimeout = MutableSharedFlow<JSONObject>(extraBufferCapacity = 5)
    val callTimeout = _callTimeout.asSharedFlow()

    private val _callCancelled = MutableSharedFlow<JSONObject>(extraBufferCapacity = 5)
    val callCancelled = _callCancelled.asSharedFlow()

    private val _callIceCandidate = MutableSharedFlow<JSONObject>(extraBufferCapacity = 20)
    val callIceCandidate = _callIceCandidate.asSharedFlow()

    // ═══════════════════════════════════════════════════════
    // Init & Connect
    // ═══════════════════════════════════════════════════════

    private var currentTokenManager: TokenManager? = null

    fun init(tokenManager: TokenManager) {
        currentTokenManager = tokenManager
        connectWithToken(tokenManager)
    }

    /**
     * Force reconnect with fresh token — call after login or token refresh
     */
    fun reconnect() {
        val tm = currentTokenManager ?: return
        Log.d(TAG, "🔄 Reconnecting socket with fresh token...")
        socketRetryCount = 0
        cleanupSocket()
        connectWithToken(tm)
    }

    private fun cleanupSocket() {
        stopHeartbeat()
        socket?.off()
        socket?.disconnect()
        socket = null
    }

    private fun connectWithToken(tokenManager: TokenManager) {
        if (socket?.connected() == true) return

        val token = tokenManager.getAccessTokenSync()
        if (token.isNullOrEmpty()) {
            Log.w(TAG, "⚠️ No access token for socket")
            return
        }

        try {
            val options = IO.Options.builder()
                .setAuth(mapOf("token" to token))
                .setReconnection(true)
                .setReconnectionAttempts(3)
                .setReconnectionDelay(2000)
                .build()

            socket = IO.socket(URI.create(SOCKET_SERVER_URL), options)

            socket?.apply {
                on(Socket.EVENT_CONNECT) {
                    Log.d(TAG, "🔌 Connected to socket server: ${socket?.id()}")
                    socketRetryCount = 0 // Reset retry counter on success
                    startHeartbeat()
                }

                on("connected") { args ->
                    Log.d(TAG, "✅ Server acknowledged: ${args.contentToString()}")
                }

                // ── Friend Events ──
                on("friend:request_received") { _ ->
                    Log.d(TAG, "🔔 Friend Request Received!")
                    _friendRequestReceived.tryEmit("new_request")
                }

                on("friend:request_accepted") { _ ->
                    Log.d(TAG, "🔔 Friend Request Accepted!")
                    _friendRequestAccepted.tryEmit("accepted")
                }

                // ── Message Events ──
                on("message:receive") { args ->
                    try {
                        val json = JSONObject(args[0].toString())
                        Log.d(TAG, "💌 Message received: ${json.optString("messageId")}")
                        _messageReceived.tryEmit(json)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message:receive", e)
                    }
                }

                on("message:sent") { args ->
                    try {
                        val json = JSONObject(args[0].toString())
                        Log.d(TAG, "✓ Message sent ACK: ${json.optString("messageId")}")
                        _messageSentAck.tryEmit(json)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message:sent", e)
                    }
                }

                on("message:delivered") { args ->
                    try {
                        val json = JSONObject(args[0].toString())
                        Log.d(TAG, "✓✓ Message delivered: ${json.optString("messageId")}")
                        _messageDelivered.tryEmit(json)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message:delivered", e)
                    }
                }

                on("message:read_receipt") { args ->
                    try {
                        val json = JSONObject(args[0].toString())
                        Log.d(TAG, "👁 Message read: ${json.optString("messageId")}")
                        _messageReadReceipt.tryEmit(json)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message:read_receipt", e)
                    }
                }

                on("message:recalled") { args ->
                    try {
                        val json = JSONObject(args[0].toString())
                        Log.d(TAG, "🗑 Message recalled: ${json.optString("messageId")}")
                        _messageRecalled.tryEmit(json)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message:recalled", e)
                    }
                }

                on("typing:update") { args ->
                    try {
                        val json = JSONObject(args[0].toString())
                        _typingUpdate.tryEmit(json)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing typing:update", e)
                    }
                }

                on("friend:online") { args ->
                    try {
                        val data = args[0]
                        if (data is JSONObject) {
                            _friendOnline.tryEmit(data.optString("userId"))
                        } else if (data is String) {
                            try {
                                val json = JSONObject(data)
                                _friendOnline.tryEmit(json.optString("userId", data))
                            } catch (e: Exception) {
                                _friendOnline.tryEmit(data)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing friend:online", e)
                    }
                }

                on("friend:offline") { args ->
                    try {
                        val data = args[0]
                        if (data is JSONObject) {
                            _friendOffline.tryEmit(data.optString("userId"))
                        } else if (data is String) {
                            try {
                                val json = JSONObject(data)
                                _friendOffline.tryEmit(json.optString("userId", data))
                            } catch (e: Exception) {
                                _friendOffline.tryEmit(data)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing friend:offline", e)
                    }
                }
                
                on("user:online") { args ->
                    try {
                        val data = args[0]
                        if (data is JSONObject) {
                            _friendOnline.tryEmit(data.optString("userId"))
                        } else if (data is String) {
                            try {
                                val json = JSONObject(data)
                                _friendOnline.tryEmit(json.optString("userId", data))
                            } catch (e: Exception) {
                                _friendOnline.tryEmit(data)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing user:online", e)
                    }
                }

                on("user:offline") { args ->
                    try {
                        val data = args[0]
                        if (data is JSONObject) {
                            _friendOffline.tryEmit(data.optString("userId"))
                        } else if (data is String) {
                            try {
                                val json = JSONObject(data)
                                _friendOffline.tryEmit(json.optString("userId", data))
                            } catch (e: Exception) {
                                _friendOffline.tryEmit(data)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing user:offline", e)
                    }
                }

                // ── Call Events (Signaling) ──
                on("call:incoming") { args ->
                    val json = JSONObject(args[0].toString())
                    Log.d(TAG, "Incoming call: ${json.optString("callId")}")
                    _callIncoming.tryEmit(json)
                }

                on("call:accepted") { args ->
                    val json = JSONObject(args[0].toString())
                    _callAccepted.tryEmit(json)
                }

                on("call:rejected") { args ->
                    val json = JSONObject(args[0].toString())
                    _callRejected.tryEmit(json)
                }

                on("call:ended") { args ->
                    val json = JSONObject(args[0].toString())
                    _callEnded.tryEmit(json)
                }

                on("call:timeout") { args ->
                    val json = JSONObject(args[0].toString())
                    _callTimeout.tryEmit(json)
                }

                on("call:cancelled") { args ->
                    val json = JSONObject(args[0].toString())
                    _callCancelled.tryEmit(json)
                }

                on("call:ice-candidate") { args ->
                    val json = JSONObject(args[0].toString())
                    _callIceCandidate.tryEmit(json)
                }

                on("message:error") { args ->
                    Log.e(TAG, "❌ Message error: ${args.contentToString()}")
                }

                // Disconnect handlers
                on(Socket.EVENT_DISCONNECT) {
                    Log.e(TAG, "❌ Disconnected from socket server")
                    stopHeartbeat()
                }

                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    val errorMsg = args.firstOrNull()?.toString() ?: "unknown"
                    Log.e(TAG, "Socket connect error: $errorMsg")

                    // Token might have been refreshed by REST 401 interceptor
                    // Always retry with fresh token from DataStore
                    if (socketRetryCount >= MAX_SOCKET_RETRIES) {
                        Log.w(TAG, "⚠️ Max socket retries ($MAX_SOCKET_RETRIES) reached, giving up")
                        return@on
                    }
                    socketRetryCount++
                    scope.launch {
                        delay(2000) // Wait for potential REST token refresh to complete
                        val freshToken = tokenManager.getAccessTokenSync()
                        if (!freshToken.isNullOrEmpty()) {
                            Log.d(TAG, "🔄 Retrying socket with fresh token...")
                            cleanupSocket()
                            connectWithToken(tokenManager)
                        } else {
                            Log.w(TAG, "⚠️ No token available for socket retry")
                        }
                    }
                }
            }

            socket?.connect()
            Log.d(TAG, "🔌 Socket connecting to $SOCKET_SERVER_URL...")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to init socket", e)
        }
    }

    // ═══════════════════════════════════════════════════════
    // Heartbeat
    // ═══════════════════════════════════════════════════════

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (isActive) {
                try {
                    socket?.emit("heartbeat")
                    delay(30000) // 30 seconds
                } catch (e: Exception) {
                    Log.e(TAG, "Heartbeat error", e)
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    // ═══════════════════════════════════════════════════════
    // Emit — Client → Server
    // ═══════════════════════════════════════════════════════

    /** Send a message via Socket (primary path, faster than REST) */
    fun emitSendMessage(
        clientMessageId: String,
        conversationId: String,
        content: String,
        type: String = "text",
        replyTo: JSONObject? = null,
        attachments: org.json.JSONArray? = null
    ) {
        try {
            val payload = JSONObject().apply {
                put("clientMessageId", clientMessageId)
                put("conversationId", conversationId)
                put("type", type)
                put("content", content)
                if (replyTo != null) put("replyTo", replyTo)
                if (attachments != null) put("attachments", attachments)
            }
            socket?.emit("message:send", payload)
            Log.d(TAG, "📤 Emitted message:send clientId=$clientMessageId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to emit message:send", e)
        }
    }

    /** Acknowledge that we received a message (triggers ✓✓ for sender) */
    fun emitAck(messageId: String, conversationId: String) {
        try {
            val payload = JSONObject().apply {
                put("messageId", messageId)
                put("conversationId", conversationId)
            }
            socket?.emit("message:ack", payload)
            Log.d(TAG, "📤 Emitted message:ack for $messageId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to emit message:ack", e)
        }
    }

    /** Mark conversation as read (triggers blue ✓✓ for sender) */
    fun emitMarkRead(conversationId: String, messageId: String?) {
        try {
            val payload = JSONObject().apply {
                put("conversationId", conversationId)
                if (messageId != null) put("messageId", messageId)
            }
            socket?.emit("message:read", payload)
            Log.d(TAG, "📤 Emitted message:read for conv=$conversationId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to emit message:read", e)
        }
    }

    /** Emit typing:start */
    fun emitTypingStart(conversationId: String) {
        try {
            val payload = JSONObject().apply {
                put("conversationId", conversationId)
            }
            socket?.emit("typing:start", payload)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to emit typing:start", e)
        }
    }

    /** Emit typing:stop */
    fun emitTypingStop(conversationId: String) {
        try {
            val payload = JSONObject().apply {
                put("conversationId", conversationId)
            }
            socket?.emit("typing:stop", payload)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to emit typing:stop", e)
        }
    }

    // ═══════════════════════════════════════════════════════
    // Connection Management
    // ═══════════════════════════════════════════════════════

    fun isConnected(): Boolean = socket?.connected() == true

    fun connect() {
        if (socket?.connected() != true) {
            // Try reconnect with fresh token if we have a token manager
            if (socket == null && currentTokenManager != null) {
                reconnect()
            } else {
                socket?.connect()
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    // Emitters for Call Events
    // ═══════════════════════════════════════════════════════

    fun initiateCall(receiverId: String, type: String, callId: String, sdpOffer: String) {
        val payload = JSONObject().apply {
            put("receiverId", receiverId)
            put("type", type)
            put("callId", callId)
            put("sdpOffer", sdpOffer)
        }
        socket?.emit("call:initiate", payload)
    }

    fun acceptCall(callId: String, sdpAnswer: String) {
        val payload = JSONObject().apply {
            put("callId", callId)
            put("sdpAnswer", sdpAnswer)
        }
        socket?.emit("call:accept", payload)
    }

    fun rejectCall(callId: String, reason: String = "busy") {
        val payload = JSONObject().apply {
            put("callId", callId)
            put("reason", reason)
        }
        socket?.emit("call:reject", payload)
    }

    fun cancelCall(callId: String) {
        val payload = JSONObject().apply { put("callId", callId) }
        socket?.emit("call:cancel", payload)
    }

    fun endCall(callId: String, duration: Int) {
        val payload = JSONObject().apply {
            put("callId", callId)
            put("duration", duration)
        }
        socket?.emit("call:end", payload)
    }

    fun sendIceCandidate(callId: String, candidate: org.webrtc.IceCandidate) {
        val jsonCandidate = JSONObject().apply {
            put("sdpMid", candidate.sdpMid)
            put("sdpMLineIndex", candidate.sdpMLineIndex)
            put("candidate", candidate.sdp)
        }
        val payload = JSONObject().apply {
            put("callId", callId)
            put("candidate", jsonCandidate)
        }
        socket?.emit("call:ice-candidate", payload)
    }

    fun disconnect() {
        cleanupSocket()
    }
}
