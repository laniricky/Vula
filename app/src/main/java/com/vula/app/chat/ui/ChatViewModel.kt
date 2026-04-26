package com.vula.app.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.vula.app.chat.data.ChatRepository
import com.vula.app.core.model.ChatRoom
import com.vula.app.core.model.Message
import com.vula.app.core.model.MessageRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import com.vula.app.core.util.Constants
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _roomsState = MutableStateFlow<List<ChatRoom>>(emptyList())
    val roomsState: StateFlow<List<ChatRoom>> = _roomsState.asStateFlow()
    
    // Map of roomId -> Other User's Name
    private val _roomNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val roomNames: StateFlow<Map<String, String>> = _roomNames.asStateFlow()

    private val _messagesState = MutableStateFlow<List<Message>>(emptyList())
    val messagesState: StateFlow<List<Message>> = _messagesState.asStateFlow()

    private val _incomingRequests = MutableStateFlow<List<MessageRequest>>(emptyList())
    val incomingRequests: StateFlow<List<MessageRequest>> = _incomingRequests.asStateFlow()

    private val _currentRoom = MutableStateFlow<ChatRoom?>(null)
    val currentRoom: StateFlow<ChatRoom?> = _currentRoom.asStateFlow()

    private var typingJob: Job? = null
    private var isTypingLocally = false

    /** Count of chat rooms with unread messages + pending incoming requests */
    val unreadCount: StateFlow<Int> = combine(_roomsState, _incomingRequests) { rooms, requests ->
        val uid = currentUserId ?: return@combine 0
        val unreadRooms = rooms.count { room -> room.unreadFor.contains(uid) }
        unreadRooms + requests.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val currentUserId: String? get() = auth.currentUser?.uid

    init {
        loadChatRooms()
        loadIncomingRequests()
    }

    private fun loadChatRooms() {
        viewModelScope.launch {
            chatRepository.getChatRooms().collect { rooms ->
                _roomsState.value = rooms
                resolveRoomNames(rooms)
            }
        }
    }
    
    private suspend fun resolveRoomNames(rooms: List<ChatRoom>) {
        val uid = currentUserId ?: return
        val currentNames = _roomNames.value.toMutableMap()
        var changed = false
        
        for (room in rooms) {
            if (room.type == "direct") {
                val otherId = room.participants.firstOrNull { it != uid }
                if (otherId != null && !currentNames.containsKey(room.id)) {
                    try {
                        val doc = firestore.collection(Constants.USERS_COLLECTION).document(otherId).get().await()
                        val username = doc.getString("username")
                        if (username != null) {
                            currentNames[room.id] = username
                            changed = true
                        }
                    } catch (e: Exception) {
                        // ignore error, keep defaulting to "Chat"
                    }
                }
            }
        }
        if (changed) {
            _roomNames.value = currentNames
        }
    }

    private fun loadIncomingRequests() {
        viewModelScope.launch {
            chatRepository.getIncomingRequests().collect { requests ->
                _incomingRequests.value = requests
            }
        }
    }

    fun loadMessages(chatRoomId: String) {
        viewModelScope.launch {
            chatRepository.markRoomRead(chatRoomId)
            
            // Observe the room itself for typing indicators
            launch {
                chatRepository.getChatRooms().collect { rooms ->
                    _currentRoom.value = rooms.find { it.id == chatRoomId }
                }
            }

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

    fun setTyping(chatRoomId: String) {
        if (!isTypingLocally) {
            isTypingLocally = true
            viewModelScope.launch { chatRepository.setTypingStatus(chatRoomId, true) }
        }
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(3000) // Clear typing status after 3 seconds of inactivity
            isTypingLocally = false
            chatRepository.setTypingStatus(chatRoomId, false)
        }
    }

    fun sendMessage(chatRoomId: String, text: String) {
        if (text.isBlank()) return
        
        // Immediately clear typing status
        typingJob?.cancel()
        if (isTypingLocally) {
            isTypingLocally = false
            viewModelScope.launch { chatRepository.setTypingStatus(chatRoomId, false) }
        }

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

    fun sendMessageRequest(
        toUserId: String,
        toUsername: String,
        toProfileImageUrl: String?,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val result = chatRepository.sendMessageRequest(toUserId, toUsername, toProfileImageUrl)
            onResult(result.isSuccess)
        }
    }

    fun acceptRequest(requestId: String, fromUserId: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val result = chatRepository.acceptMessageRequest(requestId, fromUserId)
            onResult(result.getOrNull())
        }
    }

    fun declineRequest(requestId: String) {
        viewModelScope.launch {
            chatRepository.declineMessageRequest(requestId)
        }
    }
}
