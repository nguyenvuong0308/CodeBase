package com.core.config.domain.data

interface AdPlacesRemoteConfigKeyProvider {
    val bannerNativeAdPlacesKey: String
        get() = DEFAULT_BANNER_NATIVE_AD_PLACES_KEY

    val appOpenAdPlacesKey: String
        get() = DEFAULT_APP_OPEN_AD_PLACES_KEY

    val rewardedRewardedInterInterAdPlacesKey: String
        get() = DEFAULT_REWARDED_REWARDED_INTER_INTER_AD_PLACES_KEY

    companion object {
        const val DEFAULT_BANNER_NATIVE_AD_PLACES_KEY = "banner_native_ad_places_2"
        const val DEFAULT_APP_OPEN_AD_PLACES_KEY = "app_open_ad_places_2"
        const val DEFAULT_REWARDED_REWARDED_INTER_INTER_AD_PLACES_KEY =
            "rewarded_rewardedinter_inter_ad_places_2"
    }
}

internal object DefaultAdPlacesRemoteConfigKeyProvider : AdPlacesRemoteConfigKeyProvider

internal fun Set<AdPlacesRemoteConfigKeyProvider>.activeAdPlacesRemoteConfigKeyProvider():
    AdPlacesRemoteConfigKeyProvider {
    return firstOrNull() ?: DefaultAdPlacesRemoteConfigKeyProvider
}
