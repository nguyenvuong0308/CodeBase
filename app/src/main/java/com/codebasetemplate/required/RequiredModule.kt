package com.codebasetemplate.required

import android.app.Application
import android.content.Context
import com.codebasetemplate.required.adjust.AdjustTracking
import com.codebasetemplate.required.ads.AdmobAdsInitializer
import com.codebasetemplate.required.ads.ProviderAppProviderAdPlaceName
import com.codebasetemplate.required.ads.ReopenActionImpl
import com.codebasetemplate.required.firebase.GetDataFromRemoteUseCaseImpl
import com.codebasetemplate.required.inapp.ProductIdProviderImpl
import com.codebasetemplate.required.update.InAppUpdateImpl
import com.core.ads.AdsSdkInitializer
import com.core.ads.admob.ReopenAction
import com.core.analytics.AdjustAnalytics
import com.core.billing.ProductIdProvider
import com.core.config.domain.GetDataFromRemoteConfigUseCase
import com.core.config.domain.RemoteConfigRepository
import com.core.config.domain.data.IAppProviderAdPlaceName
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun providerGetDataFromRemoteUseCase(useCase: GetDataFromRemoteUseCaseImpl): GetDataFromRemoteConfigUseCase =
        useCase

    @Provides
    @Singleton
    fun providerProviderAppProviderAdPlaceName(provider: ProviderAppProviderAdPlaceName): IAppProviderAdPlaceName =
        provider

    @Provides
    @Singleton
    fun provideInAppUpdateImpl(app: Application): InAppUpdateImpl = InAppUpdateImpl(app)

    @Provides
    @Singleton
    fun provideAdsSkdInitializer(@ApplicationContext context: Context): AdsSdkInitializer =
        AdmobAdsInitializer(context)

    @Provides
    @Singleton
    fun provideAdjustTracking(): AdjustAnalytics = AdjustTracking()

    @Provides
    @Singleton
    fun provideReopenAction(remoteConfigRepository: RemoteConfigRepository): ReopenAction = ReopenActionImpl(remoteConfigRepository)

}