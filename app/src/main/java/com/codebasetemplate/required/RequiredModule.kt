package com.codebasetemplate.required

import android.app.Application
import com.codebasetemplate.required.ads.ProviderAppProviderAdPlaceName
import com.codebasetemplate.required.firebase.GetDataFromRemoteUseCaseImpl
import com.codebasetemplate.required.inapp.ProductIdProviderImpl
import com.codebasetemplate.required.update.InAppUpdateImpl
import com.core.billing.ProductIdProvider
import com.core.config.domain.GetDataFromRemoteConfigUseCase
import com.core.config.domain.data.IAppProviderAdPlaceName
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class RequiredModule {
    @Provides
    @Singleton
    fun providerProductIds(provider: ProductIdProviderImpl): ProductIdProvider = provider

    @Provides
    @Singleton
    fun providerGetDataFromRemoteUseCase(useCase: GetDataFromRemoteUseCaseImpl): GetDataFromRemoteConfigUseCase = useCase

    @Provides
    @Singleton
    fun providerProviderAppProviderAdPlaceName(provider: ProviderAppProviderAdPlaceName): IAppProviderAdPlaceName= provider

    @Provides
    @Singleton
    fun provideInAppUpdateImpl(app: Application): InAppUpdateImpl = InAppUpdateImpl(app)
}