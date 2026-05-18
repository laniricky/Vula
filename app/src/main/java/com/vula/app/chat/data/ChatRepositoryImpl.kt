package com.vula.app.chat.data

import com.vula.app.core.model.ChatRoom
import com.vula.app.core.model.Message
import com.vula.app.core.model.MessageRequest
import com.vula.app.core.network.AcceptRequestResponse
import com.vula.app.core.network.ApiChatRoom
import com.vula.app.core.network.ApiMessage
import com.vula.app.core.network.ApiMessageRequest
import com.vula.app.core.network.CreateDirectChatBody
import com.vula.app.core.network.SendMessageBody
import com.vula.app.core.network.SendRequestBody
import com.vula.app.core.network.VulaApiService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val api: VulaApiService,
    private val wsManager: ChatWebSocketManager
) : ChatRepository {

    init {
        wsManager.connect()
    }

    override suspend fun createDirectChat(otherUserId: String): Result<String> {
        return try {
            val response = api.createDirectChat(CreateDirectChatBody(otherUserId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.roomId)
            } else {
                Result.failure(Exception("Create chat failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getChatRooms(): Flow<List<ChatRoom>> = flow {
        // Initial fetch
        try {
            val response = api.getChatRooms()
            if (response.isSuccessful) {
                emit(response.body()?.map { it.toChatRoom() } ?: emptyList())
            }
        } catch (_: Exception) { }
        
        // When a new message comes in, we can re-fetch or rely on the backend.
        // For simplicity in this sprint, we'll re-fetch the rooms when a message event fires
        wsManager.messageEvents.collect {
            try {
                val response = api.getChatRooms()
                if (response.isSuccessful) {
                    emit(response.body()?.map { it.toChatRoom() } ?: emptyList())
                }
            } catch (_: Exception) { }
        }
    }

    override fun getMessages(chatRoomId: String): Flow<List<Message>> = flow {
        var currentMessages = listOf<Message>()
        try {
            val response = api.getMessages(chatRoomId)
            if (response.isSuccessful) {
                currentMessages = response.body()?.map { it.toMessage() } ?: emptyList()
                emit(currentMessages)
            }
        } catch (_: Exception) { }
        
        wsManager.messageEvents.collect { apiMsg ->
            if (apiMsg.roomId == chatRoomId || (apiMsg as? Any) != null) { // Type check workaround
                // Add the new message to the list
                currentMessages = currentMessages + apiMsg.toMessage()
                emit(currentMessages)
            }
        }
    }

    override suspend fun sendMessage(chatRoomId: String, text: String): Result<Unit> {
        return try {
            val response = api.sendMessage(chatRoomId, SendMessageBody(text))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Send failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markMessageRead(chatRoomId: String, messageId: String): Result<Unit> =
        Result.success(Unit) // handled server-side on markRoomRead

    override suspend fun markRoomRead(chatRoomId: String): Result<Unit> {
        return try {
            api.markRoomRead(chatRoomId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setTypingStatus(chatRoomId: String, isTyping: Boolean): Result<Unit> {
        return try {
            if (isTyping) {
                wsManager.sendTypingIndicator(chatRoomId)
            }
            api.setTypingStatus(chatRoomId, isTyping)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Message Requests ──────────────────────────────────────────────────────

    override suspend fun sendMessageRequest(
        toUserId: String,
        toUsername: String,
        toProfileImageUrl: String?
    ): Result<Unit> {
        return try {
            val response = api.sendMessageRequest(
                SendRequestBody(toUserId, toUsername, toProfileImageUrl)
            )
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Request failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getIncomingRequests(): Flow<List<MessageRequest>> = flow {
        while (true) {
            try {
                val response = api.getIncomingRequests()
                if (response.isSuccessful) {
                    emit(response.body()?.map { it.toMessageRequest() } ?: emptyList())
                }
            } catch (_: Exception) {}
            delay(5_000)
        }
    }

    override fun getRequestStatusTo(toUserId: String): Flow<String?> = flow {
        while (true) {
            try {
                val response = api.getRequestStatusTo(toUserId)
                emit(if (response.isSuccessful) response.body()?.status else null)
            } catch (_: Exception) { emit(null) }
            delay(5_000)
        }
    }

    override suspend fun acceptMessageRequest(
        requestId: String,
        fromUserId: String
    ): Result<String> {
        return try {
            val response = api.acceptMessageRequest(requestId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.roomId)
            } else {
                Result.failure(Exception("Accept failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun declineMessageRequest(requestId: String): Result<Unit> {
        return try {
            val response = api.declineMessageRequest(requestId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Decline failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ── Mappers ───────────────────────────────────────────────────────────────────

private fun ApiChatRoom.toChatRoom() = ChatRoom(
    id                  = id,
    type                = type,
    participants        = participants,
    name                = name,
    lastMessage         = lastMessage,
    lastMessageAt       = lastMessageAt,
    lastMessageSenderId = lastMessageSenderId,
    unreadFor           = unreadFor,
    typingUsers         = typingUsers,
    createdAt           = createdAt
)

private fun ApiMessage.toMessage() = Message(
    id             = id,
    senderId       = senderId,
    senderUsername = senderUsername,
    text           = text,
    voiceUrl       = voiceUrl,
    createdAt      = createdAt,
    readBy         = readBy
)

private fun ApiMessageRequest.toMessageRequest() = MessageRequest(
    id                  = id,
    fromUserId          = fromUserId,
    fromUsername        = fromUsername,
    fromProfileImageUrl = fromProfileImageUrl,
    toUserId            = toUserId,
    status              = status,
    createdAt           = createdAt
)
