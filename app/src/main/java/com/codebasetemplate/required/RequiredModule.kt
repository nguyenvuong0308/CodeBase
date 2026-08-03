package com.codebasetemplate.required

import android.app.Application
import android.content.Context
import com.codebasetemplate.Navigator
import com.codebasetemplate.features.feature_onboarding.ui.helper.OnBoardingConfigFactory
import com.codebasetemplate.features.feature_uninstall.ui.UninstallActivityHost
import com.codebasetemplate.required.adjust.AdjustTracking
import com.codebasetemplate.required.ads.AdmobAdsInitializer
import com.codebasetemplate.required.ads.AppAdPlaceName
import com.codebasetemplate.required.ads.ProviderAppProviderAdPlaceName
import com.codebasetemplate.required.ads.ReopenActionImpl
import com.codebasetemplate.required.firebase.GetDataFromRemoteUseCaseImpl
import com.codebasetemplate.required.inapp.ProductIdProviderImpl
import com.codebasetemplate.required.shortcut.AppShortCut
import com.codebasetemplate.required.startflow.AppOnBoardingUiCustomizer
import com.codebasetemplate.required.update.InAppUpdateImpl
import com.core.ads.AdsSdkInitializer
import com.core.ads.admob.ReopenAction
import com.core.analytics.AdjustAnalytics
import com.core.billing.ProductIdProvider
import com.core.config.domain.GetDataFromRemoteConfigUseCase
import com.core.config.domain.RemoteConfigRepository
import com.core.config.domain.data.IAppProviderAdPlaceName
import com.core.config.domain.data.OnBoardingConfig
import com.core.startflow.StartFlowNavigator
import com.core.startflow.onboarding.OnBoardingUiCustomizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
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

    @Provides
    @IntoSet
    @Singleton
    fun provideAppOnBoardingUiCustomizer(
        customizer: AppOnBoardingUiCustomizer
    ): OnBoardingUiCustomizer = customizer

    @Provides
    @Singleton
    fun provideStartFlowNavigator(): StartFlowNavigator = object : StartFlowNavigator {
        override fun mainClass(): Class<*> = Navigator.mainClass()

        override fun uninstallClass(): Class<*> = UninstallActivityHost::class.java

        override fun onBoardingClass(onBoardingConfig: OnBoardingConfig): Class<*> {
            return OnBoardingConfigFactory.getOnBoardingClass(onBoardingConfig)
        }

        override fun setUpShortCut(context: Context, isEnable: Boolean, isEnableUninstall: Boolean) {
            AppShortCut.setUpShortCut(context, isEnable, isEnableUninstall)
        }

        override fun mainPreloadAdPlaceNames() = listOf(AppAdPlaceName.ANCHORED_BOTTOM_HOME)

        override fun uninstallPreloadAdPlaceNames() = listOf(
            AppAdPlaceName.ANCHORED_UNINSTALL_BOTTOM_STEP_1,
            AppAdPlaceName.ANCHORED_UNINSTALL_BOTTOM_STEP_2
        )
    }

}
