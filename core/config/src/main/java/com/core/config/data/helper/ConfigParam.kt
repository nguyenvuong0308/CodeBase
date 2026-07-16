package com.core.config.data.helper

import com.core.config.data.model.AdPlaceModel
import com.core.config.data.model.AppConfigModel
import com.core.config.data.model.AppOpenAdConfigModel
import com.core.config.data.model.BannerAdConfigModel
import com.core.config.data.model.IapConfigModel
import com.core.config.data.model.InterstitialAdConfigModel
import com.core.config.data.model.NativeAdConfigModel
import com.core.config.data.model.PreventAdClickConfigModel
import com.core.config.data.model.RequestConsentConfigModel
import com.core.config.data.model.RewardedAdConfigModel
import com.core.config.data.model.RewardedInterstitialAdConfigModel
import com.core.config.data.model.SplashScreenConfigModel

internal sealed class ConfigParam<T : Any> {
    abstract val key: String

    companion object {
        const val DEFAULT_OPEN_APP_AD_TIME_MILLIS_DELAY_BEFORE_SHOW = 200L
        const val DEFAULT_OPEN_APP_AD_TIME_INTERVAL = 3600L

        const val INTERSTITIAL_AD_CONFIG_DEFAULT_ADS_PER_SESSION = 50
        const val INTERSTITIAL_AD_CONFIG_DEFAULT_TIME_PER_SESSION = 600L
        const val INTERSTITIAL_AD_CONFIG_DEFAULT_TIME_INTERVAL = 37L
        const val REOPEN_TO_INTERSTITIAL_AD_CONFIG_DEFAULT_TIME_INTERVAL = 30L

        const val PREVENT_AD_CLICK_CONFIG_DEFAULT_MAX_AD_CLICK_PER_SESSION = 6
        const val PREVENT_AD_CLICK_CONFIG_DEFAULT_TIME_PER_SESSION = 120L
        const val PREVENT_AD_CLICK_CONFIG_DEFAULT_TIME_DISABLE = 1800L

        const val SPLASH_SCREEN_CONFIG_DEFAULT_MAX_TIME_TO_WAIT_APP_OPEN_AD = 30L
        const val SPLASH_SCREEN_CONFIG_DEFAULT_TIME_SKIP_APP_OPEN_AD_WHEN_NOT_AVAILABLE = 5L
        const val SPLASH_SCREEN_CONFIG_DEFAULT_MIN_TIME_WAIT_PROGRESS_BEFORE_SHOW_AD = 5L
        const val SPLASH_SCREEN_CONFIG_DEFAULT_ENABLE_RETRY = true
        const val SPLASH_SCREEN_CONFIG_DEFAULT_MAX_RETRY_COUNT = 10
        const val SPLASH_SCREEN_CONFIG_DEFAULT_RETRY_FIXED_DELAY = 1000L
        const val SPLASH_SCREEN_CONFIG_DEFAULT_IS_LOAD_BEFORE_CONSENT = true

        const val RETRY_IS_ENABLE_RETRY = false
        const val RETRY_MAX_RETRY_COUNT = 5
        const val IS_HIDE_WHEN_ERROR = false
        const val EXPIRED_NATIVE_TIME_DEFAULT = 60
        val RETRY_INTERVAL_LIST = listOf(3L, 6L, 9L, 12L, 15L)

        const val TIME_WAIT_RETRY_ON_CONTEXT = 5
        const val MAX_RETRY_ON_CONTEXT = 2
    }

    internal object AppConfig: ConfigParam<AppConfigModel>() {

        override val key = "application_config"

    }

    internal object IapConfig: ConfigParam<IapConfigModel>() {

        override val key = "iap_config"

    }

    internal object PreventAdClickConfigParam: ConfigParam<PreventAdClickConfigModel>() {

        override val key = "prevent_ad_click_config"

    }

    internal object AdsDisabledByCountryParam: ConfigParam<String>() {

        override val key = "ads_disabled_by_country"

    }

    internal object AdPlacesDisableWhenDetectTestAdParam: ConfigParam<String>() {

        override val key = "ad_places_disable_when_detect_test_ad"

    }

    internal object IsTurnOnAdPlacesDisabledWhenDetectTestAdParam: ConfigParam<Boolean>() {

        override val key = "is_turn_on_ad_places_disabled_when_detect_test_ad"

    }

    internal object SplashScreenConfigParam: ConfigParam<SplashScreenConfigModel>() {

        override val key = "splash_screen_config"

    }

    internal object AdPlacesVersionConfigParam: ConfigParam<com.core.config.data.model.AdPlacesVersionConfigModel>() {

        override val key = "ad_places_version_config"

    }

    internal object BannerNativeAdPlaces: ConfigParam<AdPlaceModel>() {

        override val key = "banner_native_ad_places"

    }

    internal object BannerNativeAdPlaces2: ConfigParam<AdPlaceModel>() {

        override val key = "banner_native_ad_places_2"

    }

    internal object AppOpenAdPlaces: ConfigParam<AdPlaceModel>() {

        override val key = "app_open_ad_places"

    }

    internal object AppOpenAdPlaces2: ConfigParam<AdPlaceModel>() {

        override val key = "app_open_ad_places_2"

    }

    internal object RewardedRewardedInterInterAdPlaces: ConfigParam<AdPlaceModel>() {

        override val key = "rewarded_rewardedinter_inter_ad_places"

    }

    internal object RewardedRewardedInterInterAdPlaces2: ConfigParam<AdPlaceModel>() {

        override val key = "rewarded_rewardedinter_inter_ad_places_2"

    }

    internal object BannerAdsParam: ConfigParam<BannerAdConfigModel>() {

        override val key = "banner_ad_config"

    }

    internal object NativeAdsParam: ConfigParam<NativeAdConfigModel>() {

        override val key = "native_ad_config"

    }

    internal object InterstitialAdsParam: ConfigParam<InterstitialAdConfigModel>() {

        override val key = "interstitial_ad_config"

    }

    internal object InterstitialRewardedAdsParam: ConfigParam<RewardedInterstitialAdConfigModel>() {

        override val key = "rewarded_interstitial_ad_config"

    }

    internal object RewardedAdsParam: ConfigParam<RewardedAdConfigModel>() {

        override val key = "rewarded_ad_config"

    }

    internal object AppOpenAdsParam: ConfigParam<AppOpenAdConfigModel>() {

        override val key = "app_open_ad_config"

    }

    internal object RequestConsentConfigParam: ConfigParam<RequestConsentConfigModel>() {

        override val key = "request_consent_config"

    }
}
