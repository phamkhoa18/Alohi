package com.example.alohi.data.remote

import com.example.alohi.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * AloHi User API Service — Full Integration
 * All endpoints for profile, search, friends, conversations, messages
 * Matches backend routes exactly
 */
interface UserApiService {

    // ═══════════════════════════════════════════════════════
    // PROFILE — /api/users
    // ═══════════════════════════════════════════════════════

    /** GET /api/users/me */
    @GET("users/me")
    suspend fun getProfile(): Response<ApiResponse<UserProfile>>

    /** POST /api/users/fcm-token */
    @POST("users/fcm-token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): Response<ApiResponse<Any>>

    /** PUT /api/users/me — Update display name, bio, gender, etc. */
    @PUT("users/me")
    suspend fun updateProfile(@Body body: Map<String, String>): Response<ApiResponse<UserProfile>>

    /** PUT /api/users/me/avatar */
    @Multipart
    @PUT("users/me/avatar")
    suspend fun updateAvatar(@Part file: okhttp3.MultipartBody.Part): Response<ApiResponse<UserProfile>>

    /** GET /api/users/:id — View other user profile */
    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: String): Response<ApiResponse<UserProfile>>

    /** GET /api/users/search?q=... */
    @GET("users/search")
    suspend fun searchUsers(@Query("q") query: String): Response<ApiResponse<List<UserProfile>>>

    /** GET /api/users/phone/:phone */
    @GET("users/phone/{phone}")
    suspend fun findByPhone(@Path("phone") phone: String): Response<ApiResponse<UserProfile>>

    /** POST /api/users/block/:userId */
    @POST("users/block/{userId}")
    suspend fun blockUser(@Path("userId") userId: String): Response<ApiResponse<Unit>>

    /** DELETE /api/users/block/:userId */
    @DELETE("users/block/{userId}")
    suspend fun unblockUser(@Path("userId") userId: String): Response<ApiResponse<Unit>>

    // ═══════════════════════════════════════════════════════
    // FRIENDS — /api/friends
    // ═══════════════════════════════════════════════════════

    /** GET /api/friends */
    @GET("friends")
    suspend fun getFriends(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
    ): Response<ApiResponse<FriendsResponse>>

    /** GET /api/friends/online */
    @GET("friends/online")
    suspend fun getOnlineFriends(): Response<ApiResponse<List<FriendItem>>>

    /** GET /api/friends/count */
    @GET("friends/count")
    suspend fun getFriendCount(): Response<ApiResponse<FriendCountResponse>>

    /** POST /api/friends/request/:userId */
    @POST("friends/request/{userId}")
    suspend fun sendFriendRequest(@Path("userId") userId: String): Response<ApiResponse<Unit>>

    /** GET /api/friends/requests/received */
    @GET("friends/requests/received")
    suspend fun getReceivedRequests(): Response<ApiResponse<FriendRequestsResponse>>

    /** GET /api/friends/requests/sent */
    @GET("friends/requests/sent")
    suspend fun getSentRequests(): Response<ApiResponse<FriendRequestsResponse>>

    /** GET /api/friends/requests/count */
    @GET("friends/requests/count")
    suspend fun getRequestCount(): Response<ApiResponse<FriendCountResponse>>

    /** PUT /api/friends/request/:requestId/accept */
    @PUT("friends/request/{requestId}/accept")
    suspend fun acceptRequest(@Path("requestId") requestId: String): Response<ApiResponse<Unit>>
    
    @PUT("friends/request/user/{userId}/accept")
    suspend fun acceptRequestByUserId(@Path("userId") userId: String): Response<ApiResponse<Unit>>

    /** PUT /api/friends/request/:requestId/reject */
    @PUT("friends/request/{requestId}/reject")
    suspend fun rejectRequest(@Path("requestId") requestId: String): Response<ApiResponse<Unit>>

    /** DELETE /api/friends/request/:requestId — cancel sent request */
    @DELETE("friends/request/{requestId}")
    suspend fun cancelRequest(@Path("requestId") requestId: String): Response<ApiResponse<Unit>>

    /** DELETE /api/friends/request/user/:userId — cancel sent request by user ID */
    @DELETE("friends/request/user/{userId}")
    suspend fun cancelRequestByUserId(@Path("userId") userId: String): Response<ApiResponse<Unit>>

    /** DELETE /api/friends/:userId — unfriend */
    @DELETE("friends/{userId}")
    suspend fun unfriend(@Path("userId") userId: String): Response<ApiResponse<Unit>>

    /** GET /api/friends/suggestions */
    @GET("friends/suggestions")
    suspend fun getFriendSuggestions(): Response<ApiResponse<List<UserProfile>>>

    /** GET /api/friends/mutual/:userId */
    @GET("friends/mutual/{userId}")
    suspend fun getMutualFriends(@Path("userId") userId: String): Response<ApiResponse<List<FriendItem>>>

    /** POST /api/friends/sync-contacts */
    @POST("friends/sync-contacts")
    suspend fun syncContacts(@Body request: SyncContactsRequest): Response<ApiResponse<List<UserProfile>>>

    // ═══════════════════════════════════════════════════════
    // CONVERSATIONS — /api/conversations
    // ═══════════════════════════════════════════════════════

    /** GET /api/conversations — paginated returns List directly */
    @GET("conversations")
    suspend fun getConversations(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30,
    ): Response<ApiResponse<List<ConversationItem>>>

    /** GET /api/conversations/:id */
    @GET("conversations/{id}")
    suspend fun getConversation(@Path("id") id: String): Response<ApiResponse<ConversationItem>>

    /** POST /api/conversations — create private/group conversation */
    @POST("conversations")
    suspend fun createConversation(@Body body: CreateConversationRequest): Response<ApiResponse<ConversationItem>>

    /** PUT /api/conversations/:id/pin */
    @PUT("conversations/{id}/pin")
    suspend fun pinConversation(@Path("id") id: String): Response<ApiResponse<Unit>>

    /** PUT /api/conversations/:id/unpin */
    @PUT("conversations/{id}/unpin")
    suspend fun unpinConversation(@Path("id") id: String): Response<ApiResponse<Unit>>

    /** PUT /api/conversations/:id/mute */
    @PUT("conversations/{id}/mute")
    suspend fun muteConversation(@Path("id") id: String): Response<ApiResponse<Unit>>

    /** PUT /api/conversations/:id/unmute */
    @PUT("conversations/{id}/unmute")
    suspend fun unmuteConversation(@Path("id") id: String): Response<ApiResponse<Unit>>

    /** DELETE /api/conversations/:id */
    @DELETE("conversations/{id}")
    suspend fun deleteConversation(@Path("id") id: String): Response<ApiResponse<Unit>>

    // ═══════════════════════════════════════════════════════
    // MESSAGES — /api/messages
    // ═══════════════════════════════════════════════════════

    /** GET /api/messages/:conversationId */
    @GET("messages/{conversationId}")
    suspend fun getMessages(
        @Path("conversationId") conversationId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): Response<ApiResponse<MessagesResponse>>

    /** POST /api/messages/:conversationId — send message */
    @POST("messages/{conversationId}")
    suspend fun sendMessage(
        @Path("conversationId") conversationId: String,
        @Body body: SendMessageRequest,
    ): Response<ApiResponse<MessageItem>>

    /** PUT /api/messages/:messageId/recall */
    @PUT("messages/{messageId}/recall")
    suspend fun recallMessage(@Path("messageId") messageId: String): Response<ApiResponse<Unit>>

    /** DELETE /api/messages/:messageId */
    @DELETE("messages/{messageId}")
    suspend fun deleteMessage(@Path("messageId") messageId: String): Response<ApiResponse<Unit>>

    // ═══════════════════════════════════════════════════════
    // UPLOAD — /api/upload
    // ═══════════════════════════════════════════════════════

    /** POST /api/upload/image — Upload chat image */
    @Multipart
    @POST("upload/image")
    suspend fun uploadChatImage(@Part file: okhttp3.MultipartBody.Part): Response<ApiResponse<UploadResult>>

    /** POST /api/upload/video — Upload chat video */
    @Multipart
    @POST("upload/video")
    suspend fun uploadChatVideo(@Part file: okhttp3.MultipartBody.Part): Response<ApiResponse<UploadResult>>

    /** POST /api/upload/audio — Upload voice message */
    @Multipart
    @POST("upload/audio")
    suspend fun uploadVoiceAudio(@Part file: okhttp3.MultipartBody.Part): Response<ApiResponse<UploadResult>>

    /** POST /api/upload/file — Upload general file */
    @Multipart
    @POST("upload/file")
    suspend fun uploadFile(@Part file: okhttp3.MultipartBody.Part): Response<ApiResponse<UploadResult>>

    // ═══════════════════════════════════════════════════════
    // FCM / DEVICE — /api/devices
    // ═══════════════════════════════════════════════════════

    /** PUT /api/devices/:deviceId/fcm-token */
    @PUT("devices/{deviceId}/fcm-token")
    suspend fun registerFcmToken(
        @Path("deviceId") deviceId: String,
        @Body request: com.example.alohi.data.model.FcmTokenRequest
    ): Response<ApiResponse<Unit>>

    // ═══════════════════════════════════════════════════════
    // MESSAGE ACTIONS — /api/messages
    // ═══════════════════════════════════════════════════════

    /** POST /api/messages/:messageId/react — Add reaction to message */
    @POST("messages/{messageId}/react")
    suspend fun addReaction(
        @Path("messageId") messageId: String,
        @Body body: ReactMessageRequest,
    ): Response<ApiResponse<Unit>>

    /** DELETE /api/messages/:messageId/react — Remove reaction */
    @DELETE("messages/{messageId}/react")
    suspend fun removeReaction(
        @Path("messageId") messageId: String,
    ): Response<ApiResponse<Unit>>

    /** POST /api/messages/:messageId/forward — Forward message */
    @POST("messages/{messageId}/forward")
    suspend fun forwardMessage(
        @Path("messageId") messageId: String,
        @Body body: ForwardMessageRequest,
    ): Response<ApiResponse<Unit>>

    // ═══════════════════════════════════════════════════════
    // GROUPS — /api/groups
    // ═══════════════════════════════════════════════════════

    /** POST /api/groups — create group */
    @POST("groups")
    suspend fun createGroup(@Body body: CreateGroupRequest): Response<ApiResponse<ConversationItem>>

    /** PUT /api/groups/:id — update group info */
    @PUT("groups/{id}")
    suspend fun updateGroup(
        @Path("id") id: String,
        @Body body: UpdateGroupRequest,
    ): Response<ApiResponse<ConversationItem>>

    /** DELETE /api/groups/:id — dissolve group */
    @DELETE("groups/{id}")
    suspend fun dissolveGroup(@Path("id") id: String): Response<ApiResponse<Unit>>

    /** POST /api/groups/:id/members — add members */
    @POST("groups/{id}/members")
    suspend fun addGroupMembers(
        @Path("id") id: String,
        @Body body: AddMembersRequest,
    ): Response<ApiResponse<Unit>>

    /** DELETE /api/groups/:id/members/:userId — remove member */
    @DELETE("groups/{id}/members/{userId}")
    suspend fun removeGroupMember(
        @Path("id") id: String,
        @Path("userId") userId: String,
    ): Response<ApiResponse<Unit>>

    /** PUT /api/groups/:id/members/:userId/role — change role */
    @PUT("groups/{id}/members/{userId}/role")
    suspend fun changeGroupRole(
        @Path("id") id: String,
        @Path("userId") userId: String,
        @Body body: ChangeRoleRequest,
    ): Response<ApiResponse<Unit>>

    /** POST /api/groups/:id/leave — leave group */
    @POST("groups/{id}/leave")
    suspend fun leaveGroup(@Path("id") id: String): Response<ApiResponse<Unit>>

    /** PUT /api/groups/:id/transfer-owner/:userId */
    @PUT("groups/{id}/transfer-owner/{userId}")
    suspend fun transferOwnership(
        @Path("id") id: String,
        @Path("userId") userId: String,
    ): Response<ApiResponse<Unit>>

    /** POST /api/groups/:id/invite-link — generate invite link */
    @POST("groups/{id}/invite-link")
    suspend fun generateInviteLink(@Path("id") id: String): Response<ApiResponse<InviteLinkResponse>>

    /** POST /api/groups/join/:inviteLink */
    @POST("groups/join/{inviteLink}")
    suspend fun joinByInviteLink(@Path("inviteLink") inviteLink: String): Response<ApiResponse<ConversationItem>>

    /** PUT /api/groups/:id/settings */
    @PUT("groups/{id}/settings")
    suspend fun updateGroupSettings(
        @Path("id") id: String,
        @Body body: UpdateGroupSettingsRequest,
    ): Response<ApiResponse<Unit>>
}
