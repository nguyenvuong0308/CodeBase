package com.core.ads.admob

import android.app.Activity
import android.app.Application
import android.util.Log
import com.core.ads.AdsSdkInitializer
import com.core.ads.model.AdHolder
import com.core.ads.model.NativeAdHolder
import com.core.ads.model.RewardedAdHolder
import com.core.analytics.AdjustAnalytics
import com.core.analytics.AnalyticsManager
import com.core.config.domain.RemoteConfigRepository
import com.core.config.domain.data.*
import com.core.preference.AppPreferences
import com.core.preference.PurchasePreferences
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class LowDeviceAdsTest {
    private val config = mock(RemoteConfigRepository::class.java)
    private val initializer = object : AdsSdkInitializer {
        override var isLowDevice = false
        override fun admobAppId() = ""
        override fun onUpdateGdprConsent(consentGranted: Boolean?) {}
        override fun onAdInitCompleted() {}
    }
    private lateinit var manager: AdmobManager
    private val name = CoreAdPlaceName.ACTION_OPEN_APP
    private val place = mock(NativeAdPlace::class.java)
    private val types = listOf(AdType.Banner, AdType.Native, AdType.NativeInterstitial,
        AdType.Interstitial, AdType.RewardedVideo, AdType.RewardedInterstitial, AdType.AppOpen)

    @Before
    fun setUp() {
        val app = mock(AppConfig::class.java)
        `when`(app.isEnableAds).thenReturn(true)
        `when`(config.getAppConfig()).thenReturn(app)
        `when`(config.getAdsDisableByCountry()).thenReturn(emptyList())
        `when`(config.getAdPlaceBy(name)).thenReturn(place)
        `when`(place.placeName).thenReturn(name)
        manager = AdmobManager(RuntimeEnvironment.getApplication(), config,
            mock(PurchasePreferences::class.java), mock(AnalyticsManager::class.java),
            mock(AppPreferences::class.java), initializer, mock(AdjustAnalytics::class.java))
    }

    @After
    fun tearDown() {
        AdmobManager::class.java.getDeclaredField("applicationScope").let {
            it.isAccessible = true
            (it.get(manager) as CoroutineScope).cancel()
        }
    }

    private fun configure(isDisabled: Boolean) {
        `when`(place.isDisableForLowDevice).thenReturn(isDisabled)
    }

    @Test
    fun `initializer contract defaults to normal device`() {
        val defaultInitializer = object : AdsSdkInitializer {
            override fun admobAppId() = ""
            override fun onUpdateGdprConsent(consentGranted: Boolean?) {}
            override fun onAdInitCompleted() {}
        }
        assertFalse(defaultInitializer.isLowDevice)
    }

    @Test
    fun `only flagged placements are disabled on low devices including resolved placements`() {
        val resolvedCheck = AdmobManager::class.java.getDeclaredMethod("isNotAbleToVisibleResolvedAdPlace", AdPlace::class.java)
            .apply { isAccessible = true }
        for (isDisabled in listOf(false, true)) {
            configure(isDisabled)
            for (isLow in listOf(false, true, false)) {
                initializer.isLowDevice = isLow
                for (type in types) {
                    `when`(place.adType).thenReturn(type)
                    val expected = isLow && isDisabled
                    assertEquals("$type low=$isLow disabled=$isDisabled", expected, manager.isNotAbleToVisibleAdsToUser(name))
                    assertEquals(expected, resolvedCheck.invoke(manager, place))
                }
            }
        }
    }

    @Test
    fun `placements with the same type and unit have independent low device flags`() {
        initializer.isLowDevice = true
        val otherName = CoreAdPlaceName.ACTION_OPEN_APP_FIRST_OPEN
        val otherPlace = mock(NativeAdPlace::class.java)
        `when`(otherPlace.placeName).thenReturn(otherName)
        `when`(otherPlace.adId).thenReturn("shared-unit")
        `when`(place.adId).thenReturn("shared-unit")
        `when`(config.getAdPlaceBy(otherName)).thenReturn(otherPlace)
        for (type in types) {
            `when`(place.adType).thenReturn(type)
            `when`(otherPlace.adType).thenReturn(type)
            for (isDisabled in listOf(true, false)) {
                configure(isDisabled)
                `when`(otherPlace.isDisableForLowDevice).thenReturn(!isDisabled)
                assertEquals(isDisabled, manager.isNotAbleToVisibleAdsToUser(name))
                assertEquals(!isDisabled, manager.isNotAbleToVisibleAdsToUser(otherName))
            }
        }
    }

    @Test
    fun `resolved nested native uses its own flag even when repository place differs`() {
        initializer.isLowDevice = true
        configure(false)
        `when`(place.adType).thenReturn(AdType.Native)
        val nestedPlace = mock(NativeAdPlace::class.java)
        `when`(nestedPlace.placeName).thenReturn(name)
        `when`(nestedPlace.adType).thenReturn(AdType.Native)
        `when`(nestedPlace.isDisableForLowDevice).thenReturn(true)
        val resolvedCheck = AdmobManager::class.java.getDeclaredMethod("isNotAbleToVisibleResolvedAdPlace", AdPlace::class.java)
            .apply { isAccessible = true }

        assertFalse(manager.isNotAbleToVisibleAdsToUser(name))
        assertEquals(true, resolvedCheck.invoke(manager, nestedPlace))
    }

    @Test
    fun `blocked formats log operation placement type and both flags`() {
        initializer.isLowDevice = true
        for (type in types) {
            configure(true)
            `when`(place.adType).thenReturn(type)
            ShadowLog.clear()

            assertTrue(manager.isNotAbleToVisibleAdsToUser(name))

            val log = ShadowLog.getLogsForTag("AdmobManager")
                .filter { it.msg.startsWith("LowDeviceAds:") }.single()
            assertEquals(Log.DEBUG, log.type)
            assertTrue(log.msg.contains("operation=isNotAbleToVisibleAdsToUser"))
            assertTrue(log.msg.contains("placement=${name.name}"))
            assertTrue(log.msg.contains("adType=${type.key}"))
            assertTrue(log.msg.contains("isLowDevice=true"))
            assertTrue(log.msg.contains("is_disable_for_low_device=true"))
        }
    }

    @Test
    fun `allowed ads do not emit low device blocked logs`() {
        `when`(place.adType).thenReturn(AdType.Native)
        for ((isLow, isDisabled) in listOf(false to true, true to false)) {
            initializer.isLowDevice = isLow
            configure(isDisabled)
            ShadowLog.clear()

            assertFalse(manager.isNotAbleToVisibleAdsToUser(name))

            assertTrue(ShadowLog.getLogsForTag("AdmobManager").none { it.msg.startsWith("LowDeviceAds:") })
        }
    }

    @Test
    fun `disabled rewards are not ready and cached natives are not exposed`() {
        configure(true)
        `when`(place.adType).thenReturn(AdType.RewardedVideo)
        cache("adHolderFullScreenMap", RewardedAdHolder(place, mock(RewardedAd::class.java)))
        assertTrue(manager.isRewardReady(name))
        initializer.isLowDevice = true
        ShadowLog.clear()
        assertFalse(manager.isRewardReady(name))
        assertTrue(ShadowLog.getLogsForTag("AdmobManager").any {
            it.msg.contains("LowDeviceAds: blocked operation=isRewardReady")
        })

        configure(true)
        `when`(place.adType).thenReturn(AdType.Native)
        val nativeHolder = NativeAdHolder(place, mock(NativeAd::class.java))
        cache("adHolderBannerNativeMap", nativeHolder)
        val activity = mock(Activity::class.java)
        ShadowLog.clear()
        assertNull(manager.getNativeHolder(activity, name))
        assertTrue(ShadowLog.getLogsForTag("AdmobManager").any {
            it.msg.contains("LowDeviceAds: blocked operation=getNativeHolder")
        })
        initializer.isLowDevice = false
        assertSame(nativeHolder, manager.getNativeHolder(activity, name))
    }

    @Suppress("UNCHECKED_CAST")
    private fun cache(fieldName: String, holder: AdHolder) {
        val field = AdmobManager::class.java.getDeclaredField(fieldName).apply { isAccessible = true }
        (field.get(manager) as MutableMap<IAdPlaceName, AdHolder>)[name] = holder
    }
}
