package com.vula.app.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * AppModule — Firebase providers have been removed as part of Phase 3 migration.
 * Auth, Firestore, and Storage are now provided by the Vula Go backend via:
 *   - NetworkModule  (Retrofit + OkHttp)
 *   - DataStoreModule (JWT token session storage)
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
