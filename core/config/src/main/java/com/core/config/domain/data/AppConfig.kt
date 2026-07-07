package com.core.config.domain.data

data class AppConfig(
    val isHideNavigationBar: Boolean,
    val isEnableAds: Boolean,
    val isHideNativeBannerWhenNetworkError : Boolean,

    val isAlwaysShowIntroAndLanguageScreen: Boolean,
    val isAlwaysShowIntroAndLanguageScreenWithInterval: Boolean,
    val isEnableIntroductionScreen : Boolean,
    val isEnableChangeLanguageScreen : Boolean,

    val isAlwaysPreloadBannerNativeAdsWhenStart : Boolean,

    val isEnableAppShortCut : Boolean,
    val isEnableAppShortcutUninstall : Boolean,
    val isEnableOpenAppAdsFromUninstallShortcut : Boolean,
    val isEnableOpenAppAdsFromShortcut : Boolean,
    val introActionShowType: Int,
    val isPreloadBannerNativeExit: Boolean,
    val intervalDayAlwaysShowIntroAndLanguage: Int,

    /**0 = list
     * 1 = Grid*/
    /**<!--intro data-->
    <!--0 = không có ads-->
    <!--1 = Có ads-->
    <!--2 = Full ads-->
     */
    val introData: List<Int>,
    val introDataV2: List<Int>,
    val introDataV3: List<Int>,
) {
    override fun toString(): String {
        val builder = StringBuilder()
        builder
            .append("\n")
            .append("isHideNavigationBar = ").append(isHideNavigationBar).append(",\n")
            .append("isEnableAds = ").append(isEnableAds).append(",\n")
            .append("isHideNativeBannerWhenNetworkError = ").append(isHideNativeBannerWhenNetworkError).append(",\n")
            .append("isAlwaysShowIntroAndLanguageScreen = ").append(isAlwaysShowIntroAndLanguageScreen).append(",\n")
            .append("isEnableIntroductionScreen = ").append(isEnableIntroductionScreen).append(",\n")
            .append("isEnableChangeLanguageScreen = ").append(isEnableChangeLanguageScreen).append(",\n")
            .append("isEnableAppShortCut = ").append(isEnableAppShortCut).append(",\n")
            .append("isEnableAppShortcutUninstall = ").append(isEnableAppShortcutUninstall).append(",\n")
            .append("isEnableOpenAppAdsFromUninstallShortcut = ").append(isEnableOpenAppAdsFromUninstallShortcut).append(",\n")
            .append("isEnableOpenAppAdsFromShortcut = ").append(isEnableOpenAppAdsFromShortcut).append(",\n")
            .append("introActionShowType = ").append(introActionShowType).append(",\n")
            .append("introData = ").append(introData)
        return builder.toString()
    }

    companion object {
        const val DEFINE_INTRO_NO_ADS = 0
        const val DEFINE_INTRO_HAVE_ADS = 1
        const val DEFINE_INTRO_FULL_AD = 2
    }
}