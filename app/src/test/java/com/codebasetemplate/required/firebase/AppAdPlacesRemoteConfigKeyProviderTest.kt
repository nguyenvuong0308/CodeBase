package com.codebasetemplate.required.firebase

import org.junit.Assert.assertEquals
import org.junit.Test

class AppAdPlacesRemoteConfigKeyProviderTest {

    private val provider = AppAdPlacesRemoteConfigKeyProvider()

    @Test
    fun `app defines the remote config fallback keys`() {
        assertEquals("banner_native_ad_places_2", provider.bannerNativeAdPlacesKey)
        assertEquals("app_open_ad_places_2", provider.appOpenAdPlacesKey)
        assertEquals(
            "rewarded_rewardedinter_inter_ad_places_2",
            provider.rewardedRewardedInterInterAdPlacesKey,
        )
    }
}
