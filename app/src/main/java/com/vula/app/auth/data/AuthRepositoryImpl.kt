package com.vula.app.auth.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.vula.app.core.model.User
import com.vula.app.core.util.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                trySend(null)
            } else {
                firestore.collection(Constants.USERS_COLLECTION)
                    .document(firebaseUser.uid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val user = snapshot.toObject(User::class.java)
                        trySend(user)
                    }
                    .addOnFailureListener {
                        trySend(null)
                    }
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun register(phoneNumber: String, username: String, password: String): Result<Unit> {
        return try {
            val lowercaseUsername = username.lowercase()
            val phoneClean = phoneNumber.trim().replace("\\s+".toRegex(), "")
            
            // Check if username exists
            val usernameDoc = try {
                firestore.collection(Constants.USERNAMES_COLLECTION)
                    .document(lowercaseUsername)
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .await()
            } catch (e: Exception) {
                android.util.Log.e("VulaAuth", "Firestore error checking username", e)
                return Result.failure(Exception("Network error or DB not initialized: ${e.message}"))
            }
                
            if (usernameDoc.exists()) {
                return Result.failure(Exception("Username already taken"))
            }

            // Create Firebase Auth user using phone number as email prefix
            val email = "$phoneClean@vula.local"
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("Failed to create user")

            // Create User document
            val hashedPhone = com.vula.app.core.util.HashUtils.sha256(phoneClean)
            val user = User(
                id = userId,
                username = lowercaseUsername,
                phoneNumber = phoneClean,
                phoneHash = hashedPhone,
                displayName = username,
                createdAt = System.currentTimeMillis()
            )

            firestore.runBatch { batch ->
                val userRef = firestore.collection(Constants.USERS_COLLECTION).document(userId)
                val usernameRef = firestore.collection(Constants.USERNAMES_COLLECTION).document(lowercaseUsername)
                
                batch.set(userRef, user)
                batch.set(usernameRef, mapOf("userId" to userId))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(phoneNumber: String, password: String): Result<Unit> {
        return try {
            val phoneClean = phoneNumber.trim().replace("\\s+".toRegex(), "")
            val email = "$phoneClean@vula.local"
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            
            // Save FCM token
            val userId = authResult.user?.uid
            if (userId != null) {
                try {
                    val token = FirebaseMessaging.getInstance().token.await()
                    firestore.collection(Constants.USERS_COLLECTION)
                        .document(userId)
                        .update("fcmToken", token)
                        .await()
                } catch (e: Exception) {
                    android.util.Log.e("VulaAuth", "Failed to save FCM token", e)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(phoneNumber: String): Result<Unit> {
        return try {
            val phoneClean = phoneNumber.trim().replace("\\s+".toRegex(), "")
            val email = "$phoneClean@vula.local"
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Could not send reset link. Check your phone number and try again."))
        }
    }

    override fun logout() {
        auth.signOut()
    }
}
