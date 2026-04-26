package com.vula.app.core.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.vula.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        val auth = Firebase.auth
        if (BuildConfig.DEBUG) {
            auth.useEmulator(FirebaseConfig.emulatorHost, 9099)
        }
        return auth
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val firestore = Firebase.firestore
        if (BuildConfig.DEBUG) {
            FirebaseFirestore.setLoggingEnabled(true)
            firestore.useEmulator(FirebaseConfig.emulatorHost, 8088)
        }
        @Suppress("DEPRECATION")
        firestore.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        return firestore
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        val storage = Firebase.storage
        if (BuildConfig.DEBUG) {
            storage.useEmulator(FirebaseConfig.emulatorHost, 9199)
        }
        return storage
    }
}
