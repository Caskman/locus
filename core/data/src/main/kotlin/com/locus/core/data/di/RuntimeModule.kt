package com.locus.core.data.di

import com.locus.core.data.source.remote.aws.RemoteStorageInterface
import com.locus.core.data.source.remote.aws.S3Client
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RuntimeModule {
    @Binds
    @Singleton
    abstract fun bindRemoteStorageInterface(client: S3Client): RemoteStorageInterface
}
