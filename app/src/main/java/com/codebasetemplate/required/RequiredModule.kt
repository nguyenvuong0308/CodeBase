package com.codebasetemplate.required

import android.app.Application
import android.content.Context
import com.codebasetemplate.Navigator
import com.codebasetemplate.features.feature_uninstall.ui.UninstallActivityHost
import com.codebasetemplate.required.adjust.AdjustTracking
import com.codebasetemplate.required.ads.AdmobAdsInitializer
import com.codebasetemplate.required.ads.AppAdPlaceName
import com.codebasetemplate.required.ads.ProviderAppProviderAdPlaceName
import com.codebasetemplate.required.firebase.GetDataFromRemoteUseCaseImpl
import com.codebasetemplate.required.inapp.ProductIdProviderImpl
import com.codebasetemplate.required.shortcut.AppShortCut
import com.codebasetemplate.required.startflow.AppOnBoardingContentProvider
import com.codebasetemplate.required.startflow.AppOnBoardingV1UiCustomizer
import com.codebasetemplate.required.startflow.AppOnBoardingV2UiCustomizer
import com.codebasetemplate.required.startflow.AppOnBoardingV3FullAdsPageRenderer
import com.codebasetemplate.required.startflow.AppOnBoardingV3PageRenderer
import com.codebasetemplate.required.startflow.AppOnBoardingV3UiCustomizer
import com.codebasetemplate.required.update.InAppUpdateImpl
import com.core.ads.AdsSdkInitializer
import com.core.analytics.AdjustAnalytics
import com.core.billing.ProductIdProvider
import com.core.config.domain.GetDataFromRemoteConfigUseCase
import com.core.config.domain.data.IAppProviderAdPlaceName
import com.core.startflow.StartFlowNavigator
import com.core.startflow.onboarding.OnBoardingContentProvider
import com.core.startflow.onboarding.v1.OnBoardingV1UiCustomizer
import com.core.startflow.onboarding.v2.OnBoardingV2UiCustomizer
import com.core.startflow.onboarding.v3.OnBoardingV3FullAdsPageRenderer
import com.core.startflow.onboarding.v3.OnBoardingV3PageRenderer
import com.core.startflow.onboarding.v3.OnBoardingV3UiCustomizer
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
    /**
     * Cung cấp danh sách product id dùng cho billing/in-app purchase.
     */
    @Provides
    @Singleton
    fun providerProductIds(provider: ProductIdProviderImpl): ProductIdProvider = provider

    /**
     * Cung cấp use case lấy dữ liệu remote config cho module core.
     */
    @Provides
    @Singleton
    fun providerGetDataFromRemoteUseCase(useCase: GetDataFromRemoteUseCaseImpl): GetDataFromRemoteConfigUseCase =
        useCase

    /**
     * Cung cấp danh sách ad placement của app để core config có thể đọc theo interface chung.
     */
    @Provides
    @Singleton
    fun providerProviderAppProviderAdPlaceName(provider: ProviderAppProviderAdPlaceName): IAppProviderAdPlaceName =
        provider

    /**
     * Cung cấp implementation kiểm tra và chạy in-app update.
     */
    @Provides
    @Singleton
    fun provideInAppUpdateImpl(app: Application): InAppUpdateImpl = InAppUpdateImpl(app)

    /**
     * Khởi tạo SDK quảng cáo bằng implementation AdMob của app.
     */
    @Provides
    @Singleton
    fun provideAdsSkdInitializer(@ApplicationContext context: Context): AdsSdkInitializer =
        AdmobAdsInitializer(context)

    /**
     * Cung cấp analytics tracking bằng Adjust.
     */
    @Provides
    @Singleton
    fun provideAdjustTracking(): AdjustAnalytics = AdjustTracking()

    /**
     * Đăng ký custom UI onboarding của app vào set customizer mà module startflow sử dụng.
     */
    @Provides
    @IntoSet
    @Singleton
    fun provideAppOnBoardingV1UiCustomizer(
        customizer: AppOnBoardingV1UiCustomizer
    ): OnBoardingV1UiCustomizer = customizer

    @Provides
    @IntoSet
    @Singleton
    fun provideAppOnBoardingV2UiCustomizer(
        customizer: AppOnBoardingV2UiCustomizer
    ): OnBoardingV2UiCustomizer = customizer

    @Provides
    @IntoSet
    @Singleton
    fun provideAppOnBoardingV3UiCustomizer(
        customizer: AppOnBoardingV3UiCustomizer
    ): OnBoardingV3UiCustomizer = customizer

    @Provides
    @IntoSet
    @Singleton
    fun provideAppOnBoardingV3PageRenderer(
        renderer: AppOnBoardingV3PageRenderer
    ): OnBoardingV3PageRenderer = renderer

    @Provides
    @IntoSet
    @Singleton
    fun provideAppOnBoardingV3FullAdsPageRenderer(
        renderer: AppOnBoardingV3FullAdsPageRenderer
    ): OnBoardingV3FullAdsPageRenderer = renderer

    @Provides
    @IntoSet
    @Singleton
    fun provideAppOnBoardingContentProvider(
        provider: AppOnBoardingContentProvider
    ): OnBoardingContentProvider = provider

    /**
     * Bind StartFlowNavigator để core:startflow biết các Activity cụ thể nằm trong module app.
     */
    @Provides
    @Singleton
    fun provideStartFlowNavigator(): StartFlowNavigator = object : StartFlowNavigator {

        /**
         * Không có màn đặc biệt sau bước chọn ngôn ngữ, nên tiếp tục dùng flow mặc định.
         */
        override fun otherLanguageEnd(): Class<*>? {
            return null
        }

        /**
         * Màn chính sau khi Splash/Language/Onboarding hoàn tất.
         */
        override fun mainClass(): Class<*> = Navigator.mainClass()

        /**
         * Màn uninstall được mở khi user vào app qua shortcut uninstall.
         */
        override fun uninstallClass(): Class<*> = UninstallActivityHost::class.java

        /**
         * Tạo hoặc cập nhật shortcut của app theo cấu hình bật/tắt từ start flow.
         */
        override fun setUpShortCut(context: Context, isEnable: Boolean, isEnableUninstall: Boolean) {
            AppShortCut.setUpShortCut(context, isEnable, isEnableUninstall)
        }

        /**
         * Preload quảng cáo ở màn Home để có sẵn khi user đi qua Splash.
         */
        override fun mainPreloadAdPlaceNames() = listOf(AppAdPlaceName.ANCHORED_BOTTOM_HOME)

        /**
         * Preload quảng cáo cho các bước của uninstall flow.
         */
        override fun uninstallPreloadAdPlaceNames() = listOf(
            AppAdPlaceName.ANCHORED_UNINSTALL_BOTTOM_STEP_1,
            AppAdPlaceName.ANCHORED_UNINSTALL_BOTTOM_STEP_2
        )
    }

}
