package com.vula.app.global.di

import com.vula.app.global.data.FollowRepository
import com.vula.app.global.data.FollowRepositoryImpl
import com.vula.app.global.data.PostRepository
import com.vula.app.global.data.PostRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GlobalModule {

    @Binds
    @Singleton
    abstract fun bindPostRepository(
        postRepositoryImpl: PostRepositoryImpl
    ): PostRepository

    @Binds
    @Singleton
    abstract fun bindFollowRepository(
        followRepositoryImpl: FollowRepositoryImpl
    ): FollowRepository
}
