package com.core.config.domain.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AdPlacesRemoteConfigKeyProviderTest {

    @Test
    fun `empty provider set uses core default keys`() {
        val provider = emptySet<AdPlacesRemoteConfigKeyProvider>()
            .activeAdPlacesRemoteConfigKeyProvider()

        assertEquals("banner_native_ad_places_2", provider.bannerNativeAdPlacesKey)
        assertEquals("app_open_ad_places_2", provider.appOpenAdPlacesKey)
        assertEquals(
            "rewarded_rewardedinter_inter_ad_places_2",
            provider.rewardedRewardedInterInterAdPlacesKey,
        )
    }

    @Test
    fun `app provider replaces all fallback keys`() {
        val appProvider = object : AdPlacesRemoteConfigKeyProvider {
            override val bannerNativeAdPlacesKey = "app_banner_native_ad_places"
            override val appOpenAdPlacesKey = "app_app_open_ad_places"
            override val rewardedRewardedInterInterAdPlacesKey = "app_fullscreen_ad_places"
        }

        val provider = setOf(appProvider).activeAdPlacesRemoteConfigKeyProvider()

        assertEquals("app_banner_native_ad_places", provider.bannerNativeAdPlacesKey)
        assertEquals("app_app_open_ad_places", provider.appOpenAdPlacesKey)
        assertEquals("app_fullscreen_ad_places", provider.rewardedRewardedInterInterAdPlacesKey)
    }

    @Test
    fun `app provider can replace only selected keys`() {
        val appProvider = object : AdPlacesRemoteConfigKeyProvider {
            override val appOpenAdPlacesKey = "custom_app_open_ad_places"
        }

        val provider = setOf(appProvider).activeAdPlacesRemoteConfigKeyProvider()

        assertEquals("banner_native_ad_places_2", provider.bannerNativeAdPlacesKey)
        assertEquals("custom_app_open_ad_places", provider.appOpenAdPlacesKey)
        assertEquals(
            "rewarded_rewardedinter_inter_ad_places_2",
            provider.rewardedRewardedInterInterAdPlacesKey,
        )
    }
}
