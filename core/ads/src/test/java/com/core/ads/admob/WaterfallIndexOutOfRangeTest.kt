package com.core.ads.admob

import android.app.Activity
import android.app.Application
import com.core.ads.AdsSdkInitializer
import com.core.ads.domain.AdFullScreenUiResource
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.ads.domain.AdsManager
import com.core.ads.model.AdHolder
import com.core.ads.model.AppOpenAdHolder
import com.core.ads.model.BannerAdHolder
import com.core.ads.model.InterstitialAdHolder
import com.core.ads.model.RewardedAdHolder
import com.core.ads.model.RewardedInterstitialAdHolder
import com.core.analytics.AdjustAnalytics
import com.core.analytics.AnalyticsManager
import com.core.config.domain.RemoteConfigRepository
import com.core.config.domain.data.*
import com.core.preference.AppPreferences
import com.core.preference.PurchasePreferences
import java.util.Optional
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import android.os.Looper

/**
 * Waterfall index được tính từ list của lượt load trước. Nếu remote config làm list ngắn lại
 * (ví dụ bớt high_floor_ad_ids) thì lượt load kế tiếp nhận index vượt list -> phải fail
 * an toàn thay vì ném IndexOutOfBoundsException.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, shadows = [ShadowAdsReadiness::class])
@LooperMode(LooperMode.Mode.PAUSED)
class WaterfallIndexOutOfRangeTest {
    private val name = CoreAdPlaceName.ACTION_OPEN_APP
    private lateinit var manager: AdmobManager
    private lateinit var activity: Activity
    private lateinit var config: RemoteConfigRepository
    private val scope = CoroutineScope(Dispatchers.Main)
    private val fullScreenEvents = mutableListOf<AdFullScreenUiResource>()
    private val bannerNativeEvents = mutableListOf<AdLoadBannerNativeUiResource>()

    @Before
    fun setUp() {
        ShadowAdsReadiness.ready = true
        config = mock(RemoteConfigRepository::class.java)
        val app = mock(AppConfig::class.java)
        `when`(app.isEnableAds).thenReturn(true)
        `when`(config.getAppConfig()).thenReturn(app)
        `when`(config.getAdsDisableByCountry()).thenReturn(emptyList())
        manager = AdmobManager(
            RuntimeEnvironment.getApplication(), config,
            mock(PurchasePreferences::class.java), mock(AnalyticsManager::class.java),
            mock(AppPreferences::class.java), mock(AdsSdkInitializer::class.java),
            mock(AdjustAnalytics::class.java)
        )
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        scope.launch(start = CoroutineStart.UNDISPATCHED) { manager.adFullScreenFlow.collect { fullScreenEvents += it } }
        scope.launch(start = CoroutineStart.UNDISPATCHED) { manager.adLoadBannerNativeFlow.collect { bannerNativeEvents += it } }
    }

    @After
    fun tearDown() {
        scope.cancel()
        AdmobManager::class.java.getDeclaredField("applicationScope").let {
            it.isAccessible = true
            (it.get(manager) as CoroutineScope).cancel()
        }
        activity.finish()
    }

    @Test
    fun `interstitial load with stale waterfall index fails instead of crashing`() {
        val holder = InterstitialAdHolder(adPlace = mockPlace(InterstitialAdPlace::class.java, AdType.Interstitial))
        invokeFullScreenLoad("loadInterstitialIfNeed", InterstitialAdHolder::class.java, holder)
        assertFullScreenFailed(holder)
    }

    @Test
    fun `rewarded interstitial load with stale waterfall index fails instead of crashing`() {
        val holder = RewardedInterstitialAdHolder(
            adPlace = mockPlace(RewardedInterstitialAdPlace::class.java, AdType.RewardedInterstitial)
        )
        invokeFullScreenLoad("loadRewardedInterstitialIfNeed", RewardedInterstitialAdHolder::class.java, holder)
        assertFullScreenFailed(holder)
    }

    @Test
    fun `rewarded load with stale waterfall index fails instead of crashing`() {
        val holder = RewardedAdHolder(adPlace = mockPlace(RewardedVideoAdPlace::class.java, AdType.RewardedVideo))
        invokeFullScreenLoad("loadRewardedIfNeed", RewardedAdHolder::class.java, holder)
        assertFullScreenFailed(holder)
    }

    @Test
    fun `banner load with stale waterfall index fails instead of crashing`() {
        val place = mockPlace(BannerAdPlace::class.java, AdType.Banner)
        `when`(place.bannerSize).thenReturn(BannerSize.Anchored)
        `when`(place.isCollapsible).thenReturn(false)
        `when`(config.getAdPlaceBy(name)).thenReturn(place)
        val holder = BannerAdHolder(adPlace = place)
        AdmobManager::class.java.getDeclaredMethod(
            "loadBannerAdIfNeed", Activity::class.java, BannerAdHolder::class.java,
            Boolean::class.javaPrimitiveType, String::class.java, Int::class.javaPrimitiveType
        ).apply { isAccessible = true }
            .invoke(manager, activity, holder, false, "banner", STALE_INDEX)
        idle()
        assertFalse(holder.isLoading)
        assertNull(holder.bannerAd)
        assertEquals(1, bannerNativeEvents.filterIsInstance<AdLoadBannerNativeUiResource.AdFailed>().size)
    }

    @Test
    fun `app open fetch with stale waterfall index fails instead of crashing`() {
        val place = mockPlace(AppOpenAdPlace::class.java, AdType.AppOpen)
        `when`(config.getAdPlaceBy(name)).thenReturn(place)
        val holder = AppOpenAdHolder(adPlace = place)
        val adsManager = mock(AdsManager::class.java)
        `when`(adsManager.isNotAbleToVisibleAdsToUser(name)).thenReturn(false)
        `when`(adsManager.getOrCreateAppOpenAdHolderBy(place)).thenReturn(holder)
        val appOpenManager = AppOpenAdManager(
            RuntimeEnvironment.getApplication(), config, adsManager, ReOpenShowCondition(),
            mock(AdjustAnalytics::class.java), Optional.empty()
        )

        appOpenManager.fetchAd(activity, name, STALE_INDEX)
        idle()

        assertFalse(holder.isLoading)
        assertNull(holder.appOpenAd)
        verify(adsManager).getOrCreateAppOpenAdHolderBy(place)
    }

    private fun <T : AdPlace> mockPlace(type: Class<T>, adType: AdType): T {
        // Waterfall hiện tại chỉ còn 1 id (high floor đã bị bỏ khỏi remote config).
        return mock(type).also {
            `when`(it.placeName).thenReturn(name)
            `when`(it.adType).thenReturn(adType)
            `when`(it.adId).thenReturn("fallback-unit")
            `when`(it.highFloorAdIds).thenReturn(emptyList())
            `when`(it.isTutorialFlow).thenReturn(false)
        }
    }

    private fun <H : AdHolder> invokeFullScreenLoad(method: String, holderType: Class<H>, holder: H) {
        AdmobManager::class.java.getDeclaredMethod(
            method, Activity::class.java, holderType, Int::class.javaPrimitiveType
        ).apply { isAccessible = true }
            .invoke(manager, activity, holder, STALE_INDEX)
        idle()
    }

    private fun assertFullScreenFailed(holder: AdHolder) {
        assertFalse(holder.isLoading)
        assertFalse(holder.isAdLoaded())
        assertEquals(1, fullScreenEvents.filterIsInstance<AdFullScreenUiResource.AdNotValidOrLoadFailed>().size)
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private companion object {
        // Index của lượt kế tiếp trong waterfall cũ [high-floor, fallback].
        const val STALE_INDEX = 1
    }
}
