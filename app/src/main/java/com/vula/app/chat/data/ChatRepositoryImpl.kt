package com.vula.app.chat.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.vula.app.core.model.ChatRoom
import com.vula.app.core.model.Message
import com.vula.app.core.model.MessageRequest
import com.vula.app.core.model.User
import com.vula.app.core.util.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ChatRepository {

    override suspend fun createDirectChat(otherUserId: String): Result<String> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: throw Exception("Not logged in")

            val querySnapshot = firestore.collection(Constants.CHAT_ROOMS_COLLECTION)
                .whereArrayContains("participants", currentUserId)
                .get()
                .await()

            for (doc in querySnapshot.documents) {
                val room = doc.toObject(ChatRoom::class.java)
                if (room != null && room.type == "direct" && room.participants.contains(otherUserId)) {
                    return Result.success(room.id)
                }
            }

            val roomRef = firestore.collection(Constants.CHAT_ROOMS_COLLECTION).document()
            val room = ChatRoom(
                id = roomRef.id,
                type = "direct",
                participants = listOf(currentUserId, otherUserId),
                createdAt = System.currentTimeMillis()
            )
            roomRef.set(room).await()
            Result.success(room.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getChatRooms(): Flow<List<ChatRoom>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            close()
            return@callbackFlow
        }

        val query = firestore.collection(Constants.CHAT_ROOMS_COLLECTION)
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            if (snapshot != null) {
                trySend(snapshot.documents.mapNotNull { it.toObject(ChatRoom::class.java) })
            }
        }
        awaitClose { listener.remove() }
    }

    override fun getMessages(chatRoomId: String): Flow<List<Message>> = callbackFlow {
        val query = firestore.collection(Constants.CHAT_ROOMS_COLLECTION).document(chatRoomId)
            .collection(Constants.MESSAGES_SUBCOLLECTION)
            .orderBy("createdAt", Query.Direction.ASCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            if (snapshot != null) {
                trySend(snapshot.documents.mapNotNull { it.toObject(Message::class.java) })
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun sendMessage(chatRoomId: String, text: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: throw Exception("Not logged in")
            val userDoc = firestore.collection(Constants.USERS_COLLECTION).document(currentUserId).get().await()
            val user = userDoc.toObject(User::class.java) ?: throw Exception("User not found")

            val roomRef = firestore.collection(Constants.CHAT_ROOMS_COLLECTION).document(chatRoomId)
            val msgRef = roomRef.collection(Constants.MESSAGES_SUBCOLLECTION).document()

            // Fetch room to know who else is in it for unreadFor
            val roomSnap = roomRef.get().await()
            val room = roomSnap.toObject(ChatRoom::class.java)
            val otherParticipants = room?.participants?.filter { it != currentUserId } ?: emptyList()

            val now = System.currentTimeMillis()
            val message = Message(
                id = msgRef.id,
                senderId = currentUserId,
                senderUsername = user.username,
                text = text,
                createdAt = now,
                readBy = listOf(currentUserId)
            )

            firestore.runBatch { batch ->
                batch.set(msgRef, message)
                batch.update(roomRef, "lastMessage", text)
                batch.update(roomRef, "lastMessageAt", now)
                batch.update(roomRef, "lastMessageSenderId", currentUserId)
                batch.update(roomRef, "unreadFor", otherParticipants)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markMessageRead(chatRoomId: String, messageId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.success(Unit)
            val msgRef = firestore.collection(Constants.CHAT_ROOMS_COLLECTION).document(chatRoomId)
                .collection(Constants.MESSAGES_SUBCOLLECTION).document(messageId)
            msgRef.update("readBy", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markRoomRead(chatRoomId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.success(Unit)
            val roomRef = firestore.collection(Constants.CHAT_ROOMS_COLLECTION).document(chatRoomId)
            roomRef.update("unreadFor",
                com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setTypingStatus(chatRoomId: String, isTyping: Boolean): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.success(Unit)
            val roomRef = firestore.collection(Constants.CHAT_ROOMS_COLLECTION).document(chatRoomId)
            
            if (isTyping) {
                roomRef.update("typingUsers", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId)).await()
            } else {
                roomRef.update("typingUsers", com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId)).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Message Requests ─────────────────────────────────────────────────────

    override suspend fun sendMessageRequest(
        toUserId: String,
        toUsername: String,
        toProfileImageUrl: String?
    ): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: throw Exception("Not logged in")
            val userDoc = firestore.collection(Constants.USERS_COLLECTION).document(currentUserId).get().await()
            val user = userDoc.toObject(User::class.java) ?: throw Exception("User not found")

            // Idempotent: one request per (from, to) pair
            val existing = firestore.collection(Constants.MESSAGE_REQUESTS_COLLECTION)
                .whereEqualTo("fromUserId", currentUserId)
                .whereEqualTo("toUserId", toUserId)
                .get().await()
            if (!existing.isEmpty) return Result.success(Unit)

            val ref = firestore.collection(Constants.MESSAGE_REQUESTS_COLLECTION).document()
            val request = MessageRequest(
                id = ref.id,
                fromUserId = currentUserId,
                fromUsername = user.username,
                fromProfileImageUrl = user.profileImageUrl,
                toUserId = toUserId,
                status = "pending",
                createdAt = System.currentTimeMillis()
            )
            ref.set(request).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getIncomingRequests(): Flow<List<MessageRequest>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) { close(); return@callbackFlow }

        val query = firestore.collection(Constants.MESSAGE_REQUESTS_COLLECTION)
            .whereEqualTo("toUserId", currentUserId)
            .whereEqualTo("status", "pending")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            if (snapshot != null) {
                trySend(snapshot.documents.mapNotNull { it.toObject(MessageRequest::class.java) })
            }
        }
        awaitClose { listener.remove() }
    }

    override fun getRequestStatusTo(toUserId: String): Flow<String?> = callbackFlow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) { trySend(null); close(); return@callbackFlow }

        val query = firestore.collection(Constants.MESSAGE_REQUESTS_COLLECTION)
            .whereEqualTo("fromUserId", currentUserId)
            .whereEqualTo("toUserId", toUserId)
            .limit(1)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val doc = snapshot?.documents?.firstOrNull()
            trySend(doc?.getString("status"))
        }
        awaitClose { listener.remove() }
    }

    override suspend fun acceptMessageRequest(requestId: String, fromUserId: String): Result<String> {
        return try {
            val requestRef = firestore.collection(Constants.MESSAGE_REQUESTS_COLLECTION).document(requestId)
            requestRef.update("status", "accepted").await()
            val roomId = createDirectChat(fromUserId).getOrThrow()
            Result.success(roomId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun declineMessageRequest(requestId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.MESSAGE_REQUESTS_COLLECTION)
                .document(requestId)
                .update("status", "declined")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
