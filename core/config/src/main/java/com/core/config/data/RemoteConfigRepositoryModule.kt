package com.core.config.data

import dagger.Binds
import dagger.Module
import dagger.multibindings.Multibinds
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.core.config.data.RemoteConfigRepositoryImpl
import com.core.config.domain.RemoteConfigRepository
import com.core.config.domain.data.AdPlacesRemoteConfigKeyProvider

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteConfigRepositoryModule {

    @Multibinds
    abstract fun bindAdPlacesRemoteConfigKeyProviders(): Set<AdPlacesRemoteConfigKeyProvider>

    @Binds
    internal abstract fun bindRemoteConfigRepository(remoteConfigRepository: RemoteConfigRepositoryImpl): RemoteConfigRepository

}
