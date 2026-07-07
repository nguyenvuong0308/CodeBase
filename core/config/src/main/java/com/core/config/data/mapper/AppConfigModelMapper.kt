package com.core.config.data.mapper

import com.core.config.data.model.AppConfigModel
import com.core.config.domain.data.AppConfig
import com.core.config.domain.data.AppConfig.Companion.DEFINE_INTRO_HAVE_ADS
import com.core.config.domain.data.AppConfig.Companion.DEFINE_INTRO_NO_ADS
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AppConfigModelMapper @Inject constructor(
) : ModelMapper<AppConfigModel, AppConfig> {

    override fun toData(model: AppConfigModel): AppConfig {
        return AppConfig(
            isHideNavigationBar = model.isHideNavigationBar ?: false,
            isEnableAds = model.isEnableAds ?: true,
            isAlwaysShowIntroAndLanguageScreen = model.isAlwaysShowIntroAndLanguageScreen ?: false,
            isAlwaysShowIntroAndLanguageScreenWithInterval = model.isAlwaysShowIntroAndLanguageScreenWithInterval ?: false,
            isEnableIntroductionScreen = model.isEnableIntroductionScreen ?: true,
            isEnableChangeLanguageScreen = model.isEnableChangeLanguageScreen ?: true,
            isEnableAppShortCut = model.isEnableAppShortCut ?: false,
            isEnableAppShortcutUninstall = model.isEnableAppShortcutUninstall ?: false,
            isEnableOpenAppAdsFromUninstallShortcut = model.isEnableOpenAppAdsFromUninstallShortcut
                ?: false,
            isEnableOpenAppAdsFromShortcut = model.isEnableOpenAppAdsFromShortcut ?: false,
            introActionShowType = model.introActionShowType ?: 1,
            isHideNativeBannerWhenNetworkError = model.isHideNativeBannerWhenNetworkError ?: false,
            introData = model.introData ?: arrayListOf(
                DEFINE_INTRO_HAVE_ADS,
                DEFINE_INTRO_HAVE_ADS,
                DEFINE_INTRO_HAVE_ADS
            ),
            introDataV2 = model.intro_data_v2 ?: arrayListOf(
                DEFINE_INTRO_NO_ADS,
                DEFINE_INTRO_NO_ADS,
                DEFINE_INTRO_HAVE_ADS
            ),
            introDataV3 = model.introDataV3 ?: arrayListOf(
                DEFINE_INTRO_NO_ADS,
                DEFINE_INTRO_NO_ADS,
                DEFINE_INTRO_HAVE_ADS
            ),
            isAlwaysPreloadBannerNativeAdsWhenStart = model.isAlwaysPreloadBannerNativeAdsWhenStart ?: true,
            isPreloadBannerNativeExit = model.isPreloadBannerNativeExit ?: false,
            intervalDayAlwaysShowIntroAndLanguage = model.intervalDayAlwaysShowIntroAndLanguage ?: 3,
        )
    }

}