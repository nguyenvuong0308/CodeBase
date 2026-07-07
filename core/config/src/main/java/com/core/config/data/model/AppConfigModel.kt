package com.core.config.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AppConfigModel(

    @Json(name = "is_hide_navigation_bar")
    val isHideNavigationBar: Boolean?,

    @Json(name = "is_enable_ads")
    val isEnableAds: Boolean?,

    @Json(name = "is_hide_native_banner_when_network_error")
    val isHideNativeBannerWhenNetworkError : Boolean?,

    @Json(name = "is_always_show_intro_and_language_screen")
    val isAlwaysShowIntroAndLanguageScreen : Boolean?,

    @Json(name = "is_always_show_intro_and_language_screen_with_interval")
    val isAlwaysShowIntroAndLanguageScreenWithInterval : Boolean?,

    @Json(name = "is_enable_introduction_screen")
    val isEnableIntroductionScreen : Boolean?,

    @Json(name = "is_enable_change_language_screen")
    val isEnableChangeLanguageScreen : Boolean?,

    @Json(name = "is_enable_app_shortcut")
    val isEnableAppShortCut : Boolean?,

    @Json(name = "is_enable_app_shortcut_uninstall")
    val isEnableAppShortcutUninstall : Boolean?,

    @Json(name = "is_enable_open_app_ads_from_uninstall_shortcut")
    val isEnableOpenAppAdsFromUninstallShortcut : Boolean?,

    @Json(name = "is_enable_open_app_ads_from_shortcut")
    val isEnableOpenAppAdsFromShortcut : Boolean?,

    @Json(name = "intro_action_show_type")
    val introActionShowType: Int?,

    @Json(name = "is_always_preload_banner_native_ads_when_start")
    val isAlwaysPreloadBannerNativeAdsWhenStart: Boolean?,

    @Json(name = "is_preload_banner_native_exit")
    val isPreloadBannerNativeExit: Boolean?,

    @Json(name = "interval_day_always_show_intro_and_language")
    val intervalDayAlwaysShowIntroAndLanguage: Int?,

    /**<!--intro data-->
        <!--0 = không có ads-->
        <!--1 = Có ads-->
        <!--2 = Full ads-->
     */
    @Json(name = "intro_data")
    val introData: List<Int>?,
    @Json(name = "intro_data_v2")
    val intro_data_v2: List<Int>?,
    @Json(name = "intro_data_v3")
    val introDataV3: List<Int>?

)
