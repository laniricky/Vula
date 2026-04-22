package com.vula.app.chat.data

import com.vula.app.core.model.ChatRoom
import com.vula.app.core.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun createDirectChat(otherUserId: String): Result<String>
    fun getChatRooms(): Flow<List<ChatRoom>>
    fun getMessages(chatRoomId: String): Flow<List<Message>>
    suspend fun sendMessage(chatRoomId: String, text: String): Result<Unit>
    suspend fun markMessageRead(chatRoomId: String, messageId: String): Result<Unit>
}
