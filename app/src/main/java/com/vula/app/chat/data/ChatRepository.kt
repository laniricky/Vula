package com.vula.app.chat.data

import com.vula.app.core.model.ChatRoom
import com.vula.app.core.model.Message
import com.vula.app.core.model.MessageRequest
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun createDirectChat(otherUserId: String): Result<String>
    fun getChatRooms(): Flow<List<ChatRoom>>
    fun getMessages(chatRoomId: String): Flow<List<Message>>
    suspend fun sendMessage(chatRoomId: String, text: String): Result<Unit>
    suspend fun markMessageRead(chatRoomId: String, messageId: String): Result<Unit>
    suspend fun setTypingStatus(chatRoomId: String, isTyping: Boolean): Result<Unit>
    // Read receipts / unread
    suspend fun markRoomRead(chatRoomId: String): Result<Unit>
    // Message requests
    suspend fun sendMessageRequest(toUserId: String, toUsername: String, toProfileImageUrl: String?): Result<Unit>
    fun getIncomingRequests(): Flow<List<MessageRequest>>
    fun getRequestStatusTo(toUserId: String): Flow<String?> // null = none, "pending", "accepted"
    suspend fun acceptMessageRequest(requestId: String, fromUserId: String): Result<String> // returns roomId
    suspend fun declineMessageRequest(requestId: String): Result<Unit>
}
