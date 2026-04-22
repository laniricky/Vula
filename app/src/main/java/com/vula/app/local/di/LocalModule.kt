package com.vula.app.local.di

import com.vula.app.local.data.LocalRepository
import com.vula.app.local.data.LocalRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocalModule {
    @Binds
    @Singleton
    abstract fun bindLocalRepository(
        localRepositoryImpl: LocalRepositoryImpl
    ): LocalRepository
}
