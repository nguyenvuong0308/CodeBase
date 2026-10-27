package com.codebasetemplate.required.firebase

import com.core.config.domain.data.AdPlacesRemoteConfigKeyProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class AppAdPlacesRemoteConfigKeyProviderTest {

    private val provider: AdPlacesRemoteConfigKeyProvider = AppAdPlacesRemoteConfigKeyProvider()

    @Test
    fun `optional app provider preserves default fallback keys through the core contract`() {
        assertEquals("banner_native_ad_places_2", provider.bannerNativeAdPlacesKey)
        assertEquals("app_open_ad_places_2", provider.appOpenAdPlacesKey)
        assertEquals(
            "rewarded_rewardedinter_inter_ad_places_2",
            provider.rewardedRewardedInterInterAdPlacesKey,
        )
    }
}
