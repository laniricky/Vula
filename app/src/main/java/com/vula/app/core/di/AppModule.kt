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

    // The PC's local Wi-Fi IP so both the emulator and Android device can talk to it
    private const val EMULATOR_HOST = "10.100.4.198"

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        val auth = Firebase.auth
        if (BuildConfig.DEBUG) {
            auth.useEmulator(EMULATOR_HOST, 9099)
        }
        return auth
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val firestore = Firebase.firestore
        if (BuildConfig.DEBUG) {
            FirebaseFirestore.setLoggingEnabled(true)
            firestore.useEmulator(EMULATOR_HOST, 8088)
        }
        return firestore
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        val storage = Firebase.storage
        if (BuildConfig.DEBUG) {
            storage.useEmulator(EMULATOR_HOST, 9199)
        }
        return storage
    }
}
