package com.vula.app.auth.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

    override suspend fun register(username: String, password: String): Result<Unit> {
        return try {
            val lowercaseUsername = username.lowercase()
            
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

            // Create Firebase Auth user
            // We use {username}@vula.local as a workaround for username login
            val email = "$lowercaseUsername@vula.local"
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("Failed to create user")

            // Create User document
            val user = User(
                id = userId,
                username = lowercaseUsername,
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

    override suspend fun login(username: String, password: String): Result<Unit> {
        return try {
            val lowercaseUsername = username.lowercase()
            val email = "$lowercaseUsername@vula.local"
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(username: String): Result<Unit> {
        return try {
            val lowercaseUsername = username.trim().lowercase()
            // The vula.local email convention — same trick used during login
            val email = "$lowercaseUsername@vula.local"
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Could not send reset link. Check your username and try again."))
        }
    }

    override fun logout() {
        auth.signOut()
    }
}
