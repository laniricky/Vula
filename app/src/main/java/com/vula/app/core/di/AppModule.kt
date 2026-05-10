package com.vula.app.core.di

import com.vula.app.core.data.SessionManager
import com.vula.app.core.network.VulaApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AppModule — provides app-scoped singletons.
 * Firebase has been fully removed; auth state lives in SessionManager (DataStore-backed).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // SessionManager is @Inject constructor — Hilt creates it automatically.
}
