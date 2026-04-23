package com.vula.app.contacts.di

import com.vula.app.contacts.data.ContactsRepository
import com.vula.app.contacts.data.ContactsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ContactsModule {

    @Binds
    @Singleton
    abstract fun bindContactsRepository(
        contactsRepositoryImpl: ContactsRepositoryImpl
    ): ContactsRepository
}
