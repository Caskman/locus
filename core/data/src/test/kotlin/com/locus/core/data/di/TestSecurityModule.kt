package com.locus.core.data.di

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [SecurityModule::class]
)
object TestSecurityModule {
    @Provides
    @Singleton
    fun provideAead(): Aead {
        return com.google.crypto.tink.KeysetHandle.generateNew(
            KeyTemplates.get("AES256_GCM"),
        ).getPrimitive(Aead::class.java)
    }
}
