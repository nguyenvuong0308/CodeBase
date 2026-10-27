package com.codebasetemplate.required.firebase

import com.core.config.domain.data.AdPlacesRemoteConfigKeyProvider
import javax.inject.Inject
import javax.inject.Singleton

/** Optional app override; register in RequiredModule when custom fallback keys are needed. */
@Singleton
class AppAdPlacesRemoteConfigKeyProvider @Inject constructor() :
    AdPlacesRemoteConfigKeyProvider {
    override val bannerNativeAdPlacesKey = "banner_native_ad_places_2"
    override val appOpenAdPlacesKey = "app_open_ad_places_2"
    override val rewardedRewardedInterInterAdPlacesKey =
        "rewarded_rewardedinter_inter_ad_places_2"
}
