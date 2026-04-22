package com.vula.app.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.vula.app.chat.data.ChatRepository
import com.vula.app.core.model.ChatRoom
import com.vula.app.core.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _roomsState = MutableStateFlow<List<ChatRoom>>(emptyList())
    val roomsState: StateFlow<List<ChatRoom>> = _roomsState.asStateFlow()

    private val _messagesState = MutableStateFlow<List<Message>>(emptyList())
    val messagesState: StateFlow<List<Message>> = _messagesState.asStateFlow()

    val currentUserId: String? get() = auth.currentUser?.uid

    init {
        loadChatRooms()
    }

    private fun loadChatRooms() {
        viewModelScope.launch {
            chatRepository.getChatRooms().collect { rooms ->
                _roomsState.value = rooms
            }
        }
    }

    fun loadMessages(chatRoomId: String) {
        viewModelScope.launch {
            chatRepository.getMessages(chatRoomId).collect { msgs ->
                _messagesState.value = msgs
                val uid = currentUserId
                if (uid != null) {
                    msgs.filter { !it.readBy.contains(uid) }.forEach { unreadMsg ->
                        chatRepository.markMessageRead(chatRoomId, unreadMsg.id)
                    }
                }
            }
        }
    }

    fun sendMessage(chatRoomId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            chatRepository.sendMessage(chatRoomId, text.trim())
        }
    }

    fun createDirectChat(otherUserId: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val result = chatRepository.createDirectChat(otherUserId)
            onResult(result.getOrNull())
        }
    }
}
