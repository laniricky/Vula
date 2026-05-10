package com.vula.app.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vula.app.chat.data.ChatRepository
import com.vula.app.core.data.SessionManager
import com.vula.app.core.model.ChatRoom
import com.vula.app.core.model.Message
import com.vula.app.core.model.MessageRequest
import com.vula.app.core.network.VulaApiService
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
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val api: VulaApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _roomsState = MutableStateFlow<List<ChatRoom>>(emptyList())
    val roomsState: StateFlow<List<ChatRoom>> = _roomsState.asStateFlow()

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

    val unreadCount: StateFlow<Int> = combine(_roomsState, _incomingRequests) { rooms, requests ->
        val uid = currentUserId ?: return@combine 0
        rooms.count { it.unreadFor.contains(uid) } + requests.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val currentUserId: String?
        get() = _cachedUserId

    private var _cachedUserId: String? = null

    init {
        viewModelScope.launch {
            _cachedUserId = sessionManager.getUserIdNow()
            loadChatRooms()
            loadIncomingRequests()
        }
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
        val uid          = currentUserId ?: return
        val currentNames = _roomNames.value.toMutableMap()
        var changed      = false

        for (room in rooms) {
            if (room.type == "direct") {
                val otherId = room.participants.firstOrNull { it != uid }
                if (otherId != null && !currentNames.containsKey(room.id)) {
                    try {
                        val response = api.getUser(otherId)
                        val username = response.body()?.username
                        if (username != null) {
                            currentNames[room.id] = username
                            changed = true
                        }
                    } catch (_: Exception) {}
                }
            }
        }
        if (changed) _roomNames.value = currentNames
    }

    private fun loadIncomingRequests() {
        viewModelScope.launch {
            chatRepository.getIncomingRequests().collect { _incomingRequests.value = it }
        }
    }

    fun loadMessages(chatRoomId: String) {
        viewModelScope.launch {
            chatRepository.markRoomRead(chatRoomId)

            launch {
                chatRepository.getChatRooms().collect { rooms ->
                    _currentRoom.value = rooms.find { it.id == chatRoomId }
                }
            }

            chatRepository.getMessages(chatRoomId).collect { msgs ->
                _messagesState.value = msgs
                val uid = currentUserId
                if (uid != null) {
                    msgs.filter { !it.readBy.contains(uid) }.forEach {
                        chatRepository.markMessageRead(chatRoomId, it.id)
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
            delay(3_000)
            isTypingLocally = false
            chatRepository.setTypingStatus(chatRoomId, false)
        }
    }

    fun sendMessage(chatRoomId: String, text: String) {
        if (text.isBlank()) return
        typingJob?.cancel()
        if (isTypingLocally) {
            isTypingLocally = false
            viewModelScope.launch { chatRepository.setTypingStatus(chatRoomId, false) }
        }
        viewModelScope.launch { chatRepository.sendMessage(chatRoomId, text.trim()) }
    }

    fun createDirectChat(otherUserId: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            onResult(chatRepository.createDirectChat(otherUserId).getOrNull())
        }
    }

    fun sendMessageRequest(toUserId: String, toUsername: String, toProfileImageUrl: String?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(chatRepository.sendMessageRequest(toUserId, toUsername, toProfileImageUrl).isSuccess)
        }
    }

    fun acceptRequest(requestId: String, fromUserId: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            onResult(chatRepository.acceptMessageRequest(requestId, fromUserId).getOrNull())
        }
    }

    fun declineRequest(requestId: String) {
        viewModelScope.launch { chatRepository.declineMessageRequest(requestId) }
    }
}
