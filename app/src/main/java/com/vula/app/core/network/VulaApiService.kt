package com.vula.app.core.network

import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

// ── Auth ─────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class RequestCodeBody(val phone: String)

@JsonClass(generateAdapter = true)
data class RequestCodeResponse(val message: String)

@JsonClass(generateAdapter = true)
data class VerifyCodeBody(val phone: String, val code: String)

@JsonClass(generateAdapter = true)
data class VerifyCodeResponse(val message: String, val token: String, val userId: String = "")

// ── Users ────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ApiUser(
    val id: String = "",
    val username: String = "",
    val phoneNumber: String = "",
    val displayName: String = "",
    val bio: String = "",
    val profileImageUrl: String? = null,
    val bannerUrl: String? = null,
    val richStatus: String? = null,
    val website: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val isPrivate: Boolean = false,
    val isOnline: Boolean = false,
    val createdAt: Long = 0L
)

@JsonClass(generateAdapter = true)
data class UpdateProfileBody(
    val displayName: String,
    val username: String,
    val bio: String,
    val richStatus: String,
    val website: String,
    val isPrivate: Boolean
)

// ── Posts ────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ApiPost(
    val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorProfileImageUrl: String? = null,
    val caption: String = "",
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val mediaType: String = "image",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val createdAt: Long = 0L,
    val likedBy: List<String> = emptyList(),
    val reactions: Map<String, String> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class CreatePostBody(val caption: String, val mediaType: String)

@JsonClass(generateAdapter = true)
data class ReactBody(val emoji: String)

@JsonClass(generateAdapter = true)
data class CommentBody(val text: String)

@JsonClass(generateAdapter = true)
data class ApiComment(
    val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)

// ── Stories ──────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ApiStory(
    val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorProfileImageUrl: String? = null,
    val imageUrl: String = "",
    val mediaType: String = "image",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L
)

// ── Chat ─────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ApiChatRoom(
    val id: String = "",
    val type: String = "direct",
    val participants: List<String> = emptyList(),
    val name: String? = null,
    val lastMessage: String? = null,
    val lastMessageAt: Long = 0L,
    val lastMessageSenderId: String? = null,
    val unreadFor: List<String> = emptyList(),
    val typingUsers: List<String> = emptyList(),
    val createdAt: Long = 0L
)

@JsonClass(generateAdapter = true)
data class ApiMessage(
    val id: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val text: String = "",
    val voiceUrl: String? = null,
    val createdAt: Long = 0L,
    val readBy: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SendMessageBody(val text: String)

@JsonClass(generateAdapter = true)
data class CreateDirectChatBody(val otherUserId: String)

@JsonClass(generateAdapter = true)
data class CreateDirectChatResponse(val roomId: String)

// ── Message Requests ─────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ApiMessageRequest(
    val id: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val fromProfileImageUrl: String? = null,
    val toUserId: String = "",
    val status: String = "pending",
    val createdAt: Long = 0L
)

@JsonClass(generateAdapter = true)
data class SendRequestBody(
    val toUserId: String,
    val toUsername: String,
    val toProfileImageUrl: String?
)

@JsonClass(generateAdapter = true)
data class AcceptRequestResponse(val roomId: String)

// ── Follow ───────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class FollowStatusResponse(val isFollowing: Boolean)

// ── Local Network ────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ApiLocalPost(
    val id: String = "",
    val networkId: String = "",
    val alias: String = "",
    val deviceIdHash: String = "",
    val text: String = "",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val reactionsCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class JoinNetworkBody(val networkId: String, val deviceHash: String, val alias: String)

@JsonClass(generateAdapter = true)
data class LocalPostBody(val networkId: String, val deviceHash: String, val alias: String, val text: String)

@JsonClass(generateAdapter = true)
data class LocalReactBody(val deviceHash: String, val emoji: String)

// ─────────────────────────────────────────────────────────────────────────────
// Retrofit Interface
// ─────────────────────────────────────────────────────────────────────────────

interface VulaApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────
    @POST("/api/auth/request-code")
    suspend fun requestCode(@Body body: RequestCodeBody): Response<RequestCodeResponse>

    @POST("/api/auth/verify-code")
    suspend fun verifyCode(@Body body: VerifyCodeBody): Response<VerifyCodeResponse>

    // ── Users ─────────────────────────────────────────────────────────────────
    @GET("/api/users/me")
    suspend fun getMe(): Response<ApiUser>

    @GET("/api/users/{userId}")
    suspend fun getUser(@Path("userId") userId: String): Response<ApiUser>

    @PUT("/api/users/me")
    suspend fun updateProfile(@Body body: UpdateProfileBody): Response<ApiUser>

    @Multipart
    @POST("/api/users/me/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): Response<ApiUser>

    @Multipart
    @POST("/api/users/me/banner")
    suspend fun uploadBanner(@Part file: MultipartBody.Part): Response<ApiUser>

    @GET("/api/users/search")
    suspend fun searchUsers(@Query("q") query: String, @Query("limit") limit: Int = 25): Response<List<ApiUser>>

    @GET("/api/users/suggested")
    suspend fun getSuggestedUsers(@Query("limit") limit: Int = 15): Response<List<ApiUser>>

    // ── Follow ────────────────────────────────────────────────────────────────
    @POST("/api/users/{userId}/follow")
    suspend fun followUser(@Path("userId") userId: String): Response<Unit>

    @DELETE("/api/users/{userId}/follow")
    suspend fun unfollowUser(@Path("userId") userId: String): Response<Unit>

    @GET("/api/users/{userId}/follow-status")
    suspend fun getFollowStatus(@Path("userId") userId: String): Response<FollowStatusResponse>

    // ── Posts ─────────────────────────────────────────────────────────────────
    @GET("/api/posts/feed")
    suspend fun getFeed(@Query("page") page: Int = 1, @Query("limit") limit: Int = 20): Response<List<ApiPost>>

    @GET("/api/posts/user/{userId}")
    suspend fun getUserPosts(@Path("userId") userId: String): Response<List<ApiPost>>

    @GET("/api/posts/explore")
    suspend fun getExplorePosts(
        @Query("filter") filter: String = "trending",
        @Query("limit") limit: Int = 40
    ): Response<List<ApiPost>>

    @Multipart
    @POST("/api/posts")
    suspend fun createPost(
        @Part media: MultipartBody.Part?,
        @Part("caption") caption: okhttp3.RequestBody,
        @Part("mediaType") mediaType: okhttp3.RequestBody
    ): Response<ApiPost>

    @POST("/api/posts/{postId}/react")
    suspend fun reactToPost(@Path("postId") postId: String, @Body body: ReactBody): Response<Unit>

    @DELETE("/api/posts/{postId}/react")
    suspend fun removeReaction(@Path("postId") postId: String): Response<Unit>

    @POST("/api/posts/{postId}/comments")
    suspend fun addComment(@Path("postId") postId: String, @Body body: CommentBody): Response<ApiComment>

    @GET("/api/posts/{postId}/comments")
    suspend fun getComments(@Path("postId") postId: String): Response<List<ApiComment>>

    // ── Stories ───────────────────────────────────────────────────────────────
    @GET("/api/stories")
    suspend fun getStories(): Response<List<ApiStory>>

    @Multipart
    @POST("/api/stories")
    suspend fun createStory(
        @Part media: MultipartBody.Part,
        @Part("mediaType") mediaType: okhttp3.RequestBody
    ): Response<ApiStory>

    // ── Chat ──────────────────────────────────────────────────────────────────
    @GET("/api/chat/rooms")
    suspend fun getChatRooms(): Response<List<ApiChatRoom>>

    @POST("/api/chat/rooms/direct")
    suspend fun createDirectChat(@Body body: CreateDirectChatBody): Response<CreateDirectChatResponse>

    @GET("/api/chat/rooms/{roomId}/messages")
    suspend fun getMessages(@Path("roomId") roomId: String): Response<List<ApiMessage>>

    @POST("/api/chat/rooms/{roomId}/messages")
    suspend fun sendMessage(@Path("roomId") roomId: String, @Body body: SendMessageBody): Response<ApiMessage>

    @POST("/api/chat/rooms/{roomId}/read")
    suspend fun markRoomRead(@Path("roomId") roomId: String): Response<Unit>

    @POST("/api/chat/rooms/{roomId}/typing")
    suspend fun setTypingStatus(@Path("roomId") roomId: String, @Query("typing") typing: Boolean): Response<Unit>

    // ── Message Requests ──────────────────────────────────────────────────────
    @POST("/api/chat/requests")
    suspend fun sendMessageRequest(@Body body: SendRequestBody): Response<Unit>

    @GET("/api/chat/requests/incoming")
    suspend fun getIncomingRequests(): Response<List<ApiMessageRequest>>

    @GET("/api/chat/requests/status")
    suspend fun getRequestStatusTo(@Query("toUserId") toUserId: String): Response<ApiMessageRequest?>

    @POST("/api/chat/requests/{requestId}/accept")
    suspend fun acceptMessageRequest(@Path("requestId") requestId: String): Response<AcceptRequestResponse>

    @POST("/api/chat/requests/{requestId}/decline")
    suspend fun declineMessageRequest(@Path("requestId") requestId: String): Response<Unit>

    // ── Local Network ─────────────────────────────────────────────────────────
    @POST("/api/local/join")
    suspend fun joinNetwork(@Body body: JoinNetworkBody): Response<Unit>

    @POST("/api/local/leave")
    suspend fun leaveNetwork(@Body body: JoinNetworkBody): Response<Unit>

    @GET("/api/local/feed")
    suspend fun getLocalFeed(@Query("networkId") networkId: String): Response<List<ApiLocalPost>>

    @POST("/api/local/posts")
    suspend fun postToLocal(@Body body: LocalPostBody): Response<ApiLocalPost>

    @POST("/api/local/posts/{postId}/react")
    suspend fun reactToLocalPost(@Path("postId") postId: String, @Body body: LocalReactBody): Response<Unit>

    @GET("/api/local/people")
    suspend fun getPeopleHere(@Query("networkId") networkId: String): Response<List<String>>

    // ── Voice Notes ───────────────────────────────────────────────────────────
    @Multipart
    @POST("/api/media/voice")
    suspend fun uploadVoiceNote(@Part file: MultipartBody.Part): Response<UploadResponse>
}

@JsonClass(generateAdapter = true)
data class UploadResponse(val url: String)
