package com.codebasetemplate

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.codebasetemplate.features.feature_demo_banner_native.ui.NativeGalleryActivity
import com.codebasetemplate.required.ads.AppAdPlaceName
import com.core.ads.BaseAdmobApplication
import com.core.ads.customviews.ads.NativeSmallTemplateView
import com.core.ads.customviews.ads.NativeInterstitialV1View
import com.core.ads.customviews.ads.NativeInterstitialV2View
import com.core.ads.customviews.ads.NativeInterstitialV3View
import com.core.ads.customviews.ads.NativeInterstitialV4View
import com.core.ads.customviews.ads.NativeInterstitialV5View
import com.core.ads.customviews.ads.NativeInterstitialV6View
import com.core.ads.domain.AdFullScreenUiResource
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.ads.domain.AdOpenAdUiResource
import com.core.ads.domain.DialogNativeFakeInterstitial
import com.core.config.domain.data.AdType
import com.core.config.domain.data.NativeAdPlace
import com.core.config.domain.data.NativeTemplateSize
import com.core.config.domain.data.CoreAdPlaceName
import com.core.config.domain.data.IAdPlaceName
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/** Runs against the real Next-Gen SDK and Google test ad units; no SDK mocks. */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AdmobDeviceSmokeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun test01_uninitializedRequestsFromWorkerFailSafely() = withActivity { activity, scope ->
        assertFalse("Run this class in a fresh instrumentation process", MobileAds.isInitialized)
        val bannerEvents = LinkedBlockingQueue<AdLoadBannerNativeUiResource>()
        val fullEvents = LinkedBlockingQueue<AdFullScreenUiResource>()
        val openEvents = LinkedBlockingQueue<AdOpenAdUiResource>()
        onMain {
            scope.launch { activity.adsManager.adLoadBannerNativeFlow.collect { bannerEvents.offer(it) } }
            scope.launch { activity.adsManager.adFullScreenFlow.collect { fullEvents.offer(it) } }
            scope.launch { activity.appOpenAdManager.adOpenAppFlow.collect { openEvents.offer(it) } }
        }
        assertNotSame(Looper.getMainLooper(), Looper.myLooper())
        activity.adsManager.loadBannerNativeAd(activity, AppAdPlaceName.ANCHORED_NATIVE_TEST, "device-preinit", false, false)
        activity.adsManager.loadFullscreenAd(activity, AppAdPlaceName.REWARD_TEST, "device-preinit")
        activity.appOpenAdManager.fetchAd(activity, CoreAdPlaceName.APP_REOPEN)
        activity.adsManager.startDisableAdCountDownTimer()
        assertTrue(bannerEvents.poll(10, TimeUnit.SECONDS) is AdLoadBannerNativeUiResource.AdFailed)
        assertTrue(fullEvents.poll(10, TimeUnit.SECONDS) is AdFullScreenUiResource.AdNotValidOrLoadFailed)
        assertTrue(openEvents.poll(10, TimeUnit.SECONDS) is AdOpenAdUiResource.AdNotValidOrLoadFailed)
        log("PASS pre-init native/fullscreen/app-open and timer from worker")
    }

    @Test
    fun test02_realNativeBannerRenderAndReleaseFromWorker() = withActivity { activity, scope ->
        initializeSdk()
        requireTestUnits(activity, AppAdPlaceName.ANCHORED_NATIVE_TEST, AppAdPlaceName.ANCHORED_BANNER_TEST)
        val events = LinkedBlockingQueue<AdLoadBannerNativeUiResource>()
        onMain { scope.launch { activity.adsManager.adLoadBannerNativeFlow.collect { events.offer(it) } } }

        activity.adsManager.loadBannerNativeAd(activity, AppAdPlaceName.ANCHORED_NATIVE_TEST, "device-native", false, true)
        val native = awaitEvent(events, "native load") {
            it is AdLoadBannerNativeUiResource.NativeAdLoaded || it is AdLoadBannerNativeUiResource.AdFailed
        }
        assertTrue("Native result: $native", native is AdLoadBannerNativeUiResource.NativeAdLoaded)
        native as AdLoadBannerNativeUiResource.NativeAdLoaded
        lateinit var rendered: LinearLayout
        onMain {
            rendered = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(android.graphics.Color.WHITE)
            }
            activity.findViewById<ViewGroup>(android.R.id.content).addView(rendered)
            rendered.addView(NativeSmallTemplateView(activity).apply { setNativeAd(native.nativeAd) })
        }
        log("PASS native loaded and registered in real NativeAdView")

        activity.adsManager.loadBannerNativeAd(activity, AppAdPlaceName.ANCHORED_BANNER_TEST, "device-banner", false, false)
        val banner = awaitEvent(events, "banner load") {
            it is AdLoadBannerNativeUiResource.BannerAdLoaded ||
                it is AdLoadBannerNativeUiResource.AdFailed && it.adPlaceName == AppAdPlaceName.ANCHORED_BANNER_TEST
        }
        assertTrue("Banner result: $banner", banner is AdLoadBannerNativeUiResource.BannerAdLoaded)
        banner as AdLoadBannerNativeUiResource.BannerAdLoaded
        onMain { rendered.addView(banner.bannerAd) }
        Thread.sleep(2_000)
        screenshot("admob-native-banner.png")
        activity.adsManager.releaseBannerNative(AppAdPlaceName.ANCHORED_BANNER_TEST)
        onMain {
            assertNull(banner.bannerAd.parent)
            rendered.removeAllViews()
        }
        activity.adsManager.releaseBannerNative(AppAdPlaceName.ANCHORED_NATIVE_TEST)
        onMain { assertNull(activity.adsManager.getNativeHolder(activity, AppAdPlaceName.ANCHORED_NATIVE_TEST)?.nativeAd) }
        log("PASS attached banner/native released from worker")

        repeat(3) {
            activity.adsManager.loadBannerNativeAd(activity, AppAdPlaceName.ANCHORED_NATIVE_TEST, "device-cancel-$it", false, true)
            activity.adsManager.releaseBannerNative(AppAdPlaceName.ANCHORED_NATIVE_TEST)
            activity.adsManager.loadBannerNativeAd(activity, AppAdPlaceName.ANCHORED_BANNER_TEST, "device-cancel-$it", false, true)
            activity.adsManager.releaseBannerNative(AppAdPlaceName.ANCHORED_BANNER_TEST)
            instrumentation.waitForIdleSync()
        }
        Thread.sleep(8_000) // Allow real network callbacks to arrive after cancellation.
        onMain { assertNull(activity.adsManager.getNativeHolder(activity, AppAdPlaceName.ANCHORED_NATIVE_TEST)?.nativeAd) }
        log("PASS 3 native/banner load-release cycles; no resurrected native after 8 seconds")
    }

    @Test
    fun test03_realFullscreenLoadShowAndDismiss() = withActivity { activity, scope ->
        initializeSdk()
        requireTestUnits(activity, AppAdPlaceName.FULLSCREEN_TEST, AppAdPlaceName.REWARD_TEST, CoreAdPlaceName.APP_REOPEN)
        val events = LinkedBlockingQueue<AdFullScreenUiResource>()
        val openEvents = LinkedBlockingQueue<AdOpenAdUiResource>()
        onMain {
            scope.launch { activity.adsManager.adFullScreenFlow.collect { events.offer(it) } }
            scope.launch { activity.appOpenAdManager.adOpenAppFlow.collect { openEvents.offer(it) } }
        }
        activity.adsManager.loadFullscreenAd(activity, AppAdPlaceName.FULLSCREEN_TEST, "device-interstitial")
        val interstitial = awaitEvent(events, "interstitial load") {
            it is AdFullScreenUiResource.AdLoaded || it is AdFullScreenUiResource.AdNotValidOrLoadFailed
        }
        assertTrue("Interstitial result: $interstitial", interstitial is AdFullScreenUiResource.AdLoaded)
        log("PASS real interstitial load")

        activity.appOpenAdManager.fetchAd(activity, CoreAdPlaceName.APP_REOPEN)
        val open = awaitEvent(openEvents, "app-open load") { true }
        assertTrue("App-open result: $open", open is AdOpenAdUiResource.AdLoaded)
        log("PASS real app-open load")

        activity.adsManager.loadFullscreenAd(activity, AppAdPlaceName.REWARD_TEST, "device-rewarded")
        val reward = awaitEvent(events, "rewarded load") {
            it.rootAdPlaceName == AppAdPlaceName.REWARD_TEST &&
                (it is AdFullScreenUiResource.AdLoaded || it is AdFullScreenUiResource.AdNotValidOrLoadFailed)
        }
        assertTrue("Rewarded result: $reward", reward is AdFullScreenUiResource.AdLoaded)
        activity.adsManager.showAd(activity, activity.supportFragmentManager, AppAdPlaceName.REWARD_TEST, "device-rewarded")
        val shown = awaitEvent(events, "rewarded show") {
            it is AdFullScreenUiResource.AdSucceedToShow || it is AdFullScreenUiResource.AdCompleted
        }
        assertTrue("Show result: $shown", shown is AdFullScreenUiResource.AdSucceedToShow)
        screenshot("admob-rewarded.png")
        log("PASS real rewarded show from worker; waiting for close")
        closeTestAd(events)
        onMain { assertFalse(activity.adsManager.isHasFullscreenAdShowing()) }
        log("PASS rewarded dismissal clears fullscreen showing state")
    }

    @Test
    fun test04_actionAppOpenNativeInterstitialLoadShowClose() = withActivity { activity, scope ->
        initializeSdk()
        val placeName = CoreAdPlaceName.ACTION_OPEN_APP
        requireTestUnits(activity, placeName)
        val configuredPlace = activity.remoteConfigRepository.getAdPlaceBy(placeName)
        org.junit.Assume.assumeTrue(
            "Configure action_app_open as native_interstitial with a Google native test ad unit",
            configuredPlace is NativeAdPlace && configuredPlace.adType == AdType.NativeInterstitial,
        )
        onMain {
            val place = configuredPlace as NativeAdPlace
            assertEquals(AdType.NativeInterstitial, place.adType)
            assertEquals(NativeTemplateSize.FullInterstitialV3, place.nativeTemplateSize)
            assertEquals(1, place.closeStepCount)
            assertEquals(5, place.countDownTimer)
        }
        verifyNativeInterstitialLoadShowClose(activity, scope, placeName)
    }

    @Test
    fun test05_nativeInterstitialLoadShowClose() = withActivity { activity, scope ->
        initializeSdk()
        val placeName = AppAdPlaceName.FULLSCREEN_NATIVE_INTERSTITIAL
        requireTestUnits(activity, placeName)
        val place = activity.remoteConfigRepository.getAdPlaceBy(placeName)
        assertTrue("Expected dedicated native_interstitial test placement", place is NativeAdPlace)
        place as NativeAdPlace
        assertEquals(AdType.NativeInterstitial, place.adType)
        assertEquals(NativeTemplateSize.FullInterstitialV3, place.nativeTemplateSize)
        verifyNativeInterstitialLoadShowClose(activity, scope, placeName)
    }

    private fun verifyNativeInterstitialLoadShowClose(
        activity: NativeGalleryActivity,
        scope: CoroutineScope,
        placeName: IAdPlaceName,
    ) {
        val events = LinkedBlockingQueue<AdFullScreenUiResource>()
        val nativeEvents = LinkedBlockingQueue<AdLoadBannerNativeUiResource>()
        onMain {
            scope.launch {
                activity.adsManager.adFullScreenFlow.collect {
                    if (it.rootAdPlaceName == placeName) events.offer(it)
                }
            }
            scope.launch {
                activity.adsManager.adLoadBannerNativeFlow.collect {
                    if (it.commonAdPlaceName == placeName) nativeEvents.offer(it)
                }
            }
        }

        activity.adsManager.loadFullscreenAd(activity, placeName, "device-native-interstitial")
        val loaded = awaitEvent(events, "${placeName.name} native load") {
            it is AdFullScreenUiResource.AdLoaded || it is AdFullScreenUiResource.AdNotValidOrLoadFailed
        }
        assertTrue("Native fullscreen load: $loaded", loaded is AdFullScreenUiResource.AdLoaded)
        activity.adsManager.showAd(activity, activity.supportFragmentManager, placeName, "device-native-interstitial")
        val shown = awaitEvent(events, "${placeName.name} native show") {
            it is AdFullScreenUiResource.AdSucceedToShow || it is AdFullScreenUiResource.AdCompleted
        }
        assertTrue("Native fullscreen show: $shown", shown is AdFullScreenUiResource.AdSucceedToShow)
        onMain {
            assertTrue(activity.adsManager.isHasFullscreenAdShowing())
            assertTrue(activity.supportFragmentManager.findFragmentByTag(
                "DialogNativeInterstitial_device-native-interstitial") is DialogNativeFakeInterstitial)
        }
        Thread.sleep(1_500)
        screenshot("${placeName.name}-native-interstitial.png")
        assertFalse("Must not complete before the close button is pressed",
            events.any { it is AdFullScreenUiResource.AdCompleted })

        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
        var clicked = false
        while (!clicked && System.nanoTime() < deadline) {
            onMain {
                val dialog = activity.supportFragmentManager.findFragmentByTag(
                    "DialogNativeInterstitial_device-native-interstitial") as? DialogNativeFakeInterstitial
                val next = dialog?.view?.findViewById<View>(com.core.ads.R.id.tvNext)
                if (next?.isShown == true && next.isEnabled) next.performClick()
                val close = dialog?.view?.findViewById<View>(com.core.ads.R.id.tvClose)
                if (close?.isShown == true && close.isEnabled) clicked = close.performClick()
            }
            if (!clicked) Thread.sleep(200)
        }
        assertTrue("Native close button must become available after countdown", clicked)
        val dismissed = awaitEvent(events, "${placeName.name} native dismiss") {
            it is AdFullScreenUiResource.AdDismissed
        }
        assertTrue(dismissed is AdFullScreenUiResource.AdDismissed)
        val completed = awaitEvent(events, "${placeName.name} native complete") {
            it is AdFullScreenUiResource.AdCompleted
        } as AdFullScreenUiResource.AdCompleted
        assertTrue(completed.isShown)
        assertFalse(completed.isEarnedReward)
        onMain { assertFalse(activity.adsManager.isHasFullscreenAdShowing()) }
        assertTrue("Fullscreen native must not require banner/native events", nativeEvents.isEmpty())
        log("PASS ${placeName.name} native_interstitial: real load, render, countdown, close and fullscreen callbacks")
    }

    private fun initializeSdk() {
        if (MobileAds.isInitialized) return
        val app = instrumentation.targetContext.applicationContext as BaseAdmobApplication
        val config = InitializationConfig.Builder("ca-app-pub-3940256099942544~3347511713")
            .setRequestConfiguration(app.createAdsRequestConfiguration()).build()
        assertNotSame(Looper.getMainLooper(), Looper.myLooper())
        MobileAds.initialize(app, config)
        assertTrue(MobileAds.isInitialized)
        log("PASS real SDK initialize on worker")
    }

    @Test
    fun test06_nativeInterstitialV1Landscape() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV1, NativeInterstitialV1View::class.java,
    )

    @Test
    fun test07_nativeInterstitialV2Landscape() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV2, NativeInterstitialV2View::class.java,
    )

    @Test
    fun test08_nativeInterstitialV3Landscape() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV3, NativeInterstitialV3View::class.java,
    )

    @Test
    fun test09_nativeInterstitialV1Portrait() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV1, NativeInterstitialV1View::class.java, landscape = false,
    )

    @Test
    fun test10_nativeInterstitialV2Portrait() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV2, NativeInterstitialV2View::class.java, landscape = false,
    )

    @Test fun test11_nativeInterstitialV4Portrait() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV4, NativeInterstitialV4View::class.java, landscape = false)
    @Test fun test12_nativeInterstitialV4Landscape() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV4, NativeInterstitialV4View::class.java)
    @Test fun test13_nativeInterstitialV5Portrait() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV5, NativeInterstitialV5View::class.java, landscape = false)
    @Test fun test14_nativeInterstitialV5Landscape() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV5, NativeInterstitialV5View::class.java)
    @Test fun test15_nativeInterstitialV6Portrait() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV6, NativeInterstitialV6View::class.java, landscape = false)
    @Test fun test16_nativeInterstitialV6Landscape() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV6, NativeInterstitialV6View::class.java)

    @Test fun test17_nativeInterstitialV4DarkPortrait() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV4, NativeInterstitialV4View::class.java, landscape = false, theme = "dark")
    @Test fun test18_nativeInterstitialV4DarkLandscape() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV4, NativeInterstitialV4View::class.java, landscape = true, theme = "dark")
    @Test fun test19_nativeInterstitialV4LightPortrait() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV4, NativeInterstitialV4View::class.java, landscape = false, theme = "light")
    @Test fun test20_nativeInterstitialV4LightLandscape() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV4, NativeInterstitialV4View::class.java, landscape = true, theme = "light")
    @Test fun test21_nativeInterstitialV5DarkPortrait() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV5, NativeInterstitialV5View::class.java, landscape = false, theme = "dark")
    @Test fun test22_nativeInterstitialV5DarkLandscape() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV5, NativeInterstitialV5View::class.java, landscape = true, theme = "dark")
    @Test fun test23_nativeInterstitialV5LightPortrait() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV5, NativeInterstitialV5View::class.java, landscape = false, theme = "light")
    @Test fun test24_nativeInterstitialV5LightLandscape() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV5, NativeInterstitialV5View::class.java, landscape = true, theme = "light")
    @Test fun test25_nativeInterstitialV6DarkPortrait() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV6, NativeInterstitialV6View::class.java, landscape = false, theme = "dark")
    @Test fun test26_nativeInterstitialV6DarkLandscape() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV6, NativeInterstitialV6View::class.java, landscape = true, theme = "dark")
    @Test fun test27_nativeInterstitialV6LightPortrait() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV6, NativeInterstitialV6View::class.java, landscape = false, theme = "light")
    @Test fun test28_nativeInterstitialV6LightLandscape() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV6, NativeInterstitialV6View::class.java, landscape = true, theme = "light")

    @Test fun test29_nativeInterstitialV4ShufflePortrait() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV4, NativeInterstitialV4View::class.java, landscape = false, shufflePosition = true)
    @Test fun test30_nativeInterstitialV4ShuffleLandscape() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV4, NativeInterstitialV4View::class.java, landscape = true, shufflePosition = true)
    @Test fun test31_nativeInterstitialV5ShufflePortrait() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV5, NativeInterstitialV5View::class.java, landscape = false, shufflePosition = true)
    @Test fun test32_nativeInterstitialV5ShuffleLandscape() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV5, NativeInterstitialV5View::class.java, landscape = true, shufflePosition = true)
    @Test fun test33_nativeInterstitialV6ShufflePortrait() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV6, NativeInterstitialV6View::class.java, landscape = false, shufflePosition = true)
    @Test fun test34_nativeInterstitialV6ShuffleLandscape() = verifyNativeInterstitialTemplate(
        NativeTemplateSize.FullInterstitialV6, NativeInterstitialV6View::class.java, landscape = true, shufflePosition = true)

    @Test fun testGuide_v4_dark_left_countdown() = runGuideCase("v4-dark-left-countdown")
    @Test fun testGuide_v4_dark_left_close() = runGuideCase("v4-dark-left-close")
    @Test fun testGuide_v4_dark_right_countdown() = runGuideCase("v4-dark-right-countdown")
    @Test fun testGuide_v4_dark_right_close() = runGuideCase("v4-dark-right-close")
    @Test fun testGuide_v4_dark_shuffle_countdown() = runGuideCase("v4-dark-shuffle-countdown")
    @Test fun testGuide_v4_dark_shuffle_close() = runGuideCase("v4-dark-shuffle-close")
    @Test fun testGuide_v4_light_left_countdown() = runGuideCase("v4-light-left-countdown")
    @Test fun testGuide_v4_light_left_close() = runGuideCase("v4-light-left-close")
    @Test fun testGuide_v4_light_right_countdown() = runGuideCase("v4-light-right-countdown")
    @Test fun testGuide_v4_light_right_close() = runGuideCase("v4-light-right-close")
    @Test fun testGuide_v4_light_shuffle_countdown() = runGuideCase("v4-light-shuffle-countdown")
    @Test fun testGuide_v4_light_shuffle_close() = runGuideCase("v4-light-shuffle-close")
    @Test fun testGuide_v5_dark_left_countdown() = runGuideCase("v5-dark-left-countdown")
    @Test fun testGuide_v5_dark_left_close() = runGuideCase("v5-dark-left-close")
    @Test fun testGuide_v5_dark_right_countdown() = runGuideCase("v5-dark-right-countdown")
    @Test fun testGuide_v5_dark_right_close() = runGuideCase("v5-dark-right-close")
    @Test fun testGuide_v5_dark_shuffle_countdown() = runGuideCase("v5-dark-shuffle-countdown")
    @Test fun testGuide_v5_dark_shuffle_close() = runGuideCase("v5-dark-shuffle-close")
    @Test fun testGuide_v5_light_left_countdown() = runGuideCase("v5-light-left-countdown")
    @Test fun testGuide_v5_light_left_close() = runGuideCase("v5-light-left-close")
    @Test fun testGuide_v5_light_right_countdown() = runGuideCase("v5-light-right-countdown")
    @Test fun testGuide_v5_light_right_close() = runGuideCase("v5-light-right-close")
    @Test fun testGuide_v5_light_shuffle_countdown() = runGuideCase("v5-light-shuffle-countdown")
    @Test fun testGuide_v5_light_shuffle_close() = runGuideCase("v5-light-shuffle-close")
    @Test fun testGuide_v6_dark_left_countdown() = runGuideCase("v6-dark-left-countdown")
    @Test fun testGuide_v6_dark_left_close() = runGuideCase("v6-dark-left-close")
    @Test fun testGuide_v6_dark_right_countdown() = runGuideCase("v6-dark-right-countdown")
    @Test fun testGuide_v6_dark_right_close() = runGuideCase("v6-dark-right-close")
    @Test fun testGuide_v6_dark_shuffle_countdown() = runGuideCase("v6-dark-shuffle-countdown")
    @Test fun testGuide_v6_dark_shuffle_close() = runGuideCase("v6-dark-shuffle-close")
    @Test fun testGuide_v6_light_left_countdown() = runGuideCase("v6-light-left-countdown")
    @Test fun testGuide_v6_light_left_close() = runGuideCase("v6-light-left-close")
    @Test fun testGuide_v6_light_right_countdown() = runGuideCase("v6-light-right-countdown")
    @Test fun testGuide_v6_light_right_close() = runGuideCase("v6-light-right-close")
    @Test fun testGuide_v6_light_shuffle_countdown() = runGuideCase("v6-light-shuffle-countdown")
    @Test fun testGuide_v6_light_shuffle_close() = runGuideCase("v6-light-shuffle-close")

    /** Real SDK + production dialog UI. V4–V6 select their template and colors from config. */
    @Test fun testGuide_v4_dark_left_countdown_compact() = runGuideCase("v4-dark-left-countdown-compact")
    @Test fun testGuide_v4_dark_left_close_compact() = runGuideCase("v4-dark-left-close-compact")
    @Test fun testGuide_v4_dark_right_countdown_compact() = runGuideCase("v4-dark-right-countdown-compact")
    @Test fun testGuide_v4_dark_right_close_compact() = runGuideCase("v4-dark-right-close-compact")
    @Test fun testGuide_v4_dark_shuffle_countdown_compact() = runGuideCase("v4-dark-shuffle-countdown-compact")
    @Test fun testGuide_v4_dark_shuffle_close_compact() = runGuideCase("v4-dark-shuffle-close-compact")
    @Test fun testGuide_v4_light_left_countdown_compact() = runGuideCase("v4-light-left-countdown-compact")
    @Test fun testGuide_v4_light_left_close_compact() = runGuideCase("v4-light-left-close-compact")
    @Test fun testGuide_v4_light_right_countdown_compact() = runGuideCase("v4-light-right-countdown-compact")
    @Test fun testGuide_v4_light_right_close_compact() = runGuideCase("v4-light-right-close-compact")
    @Test fun testGuide_v4_light_shuffle_countdown_compact() = runGuideCase("v4-light-shuffle-countdown-compact")
    @Test fun testGuide_v4_light_shuffle_close_compact() = runGuideCase("v4-light-shuffle-close-compact")
    @Test fun testGuide_v5_dark_left_countdown_compact() = runGuideCase("v5-dark-left-countdown-compact")
    @Test fun testGuide_v5_dark_left_close_compact() = runGuideCase("v5-dark-left-close-compact")
    @Test fun testGuide_v5_dark_right_countdown_compact() = runGuideCase("v5-dark-right-countdown-compact")
    @Test fun testGuide_v5_dark_right_close_compact() = runGuideCase("v5-dark-right-close-compact")
    @Test fun testGuide_v5_dark_shuffle_countdown_compact() = runGuideCase("v5-dark-shuffle-countdown-compact")
    @Test fun testGuide_v5_dark_shuffle_close_compact() = runGuideCase("v5-dark-shuffle-close-compact")
    @Test fun testGuide_v5_light_left_countdown_compact() = runGuideCase("v5-light-left-countdown-compact")
    @Test fun testGuide_v5_light_left_close_compact() = runGuideCase("v5-light-left-close-compact")
    @Test fun testGuide_v5_light_right_countdown_compact() = runGuideCase("v5-light-right-countdown-compact")
    @Test fun testGuide_v5_light_right_close_compact() = runGuideCase("v5-light-right-close-compact")
    @Test fun testGuide_v5_light_shuffle_countdown_compact() = runGuideCase("v5-light-shuffle-countdown-compact")
    @Test fun testGuide_v5_light_shuffle_close_compact() = runGuideCase("v5-light-shuffle-close-compact")
    @Test fun testGuide_v6_dark_left_countdown_compact() = runGuideCase("v6-dark-left-countdown-compact")
    @Test fun testGuide_v6_dark_left_close_compact() = runGuideCase("v6-dark-left-close-compact")
    @Test fun testGuide_v6_dark_right_countdown_compact() = runGuideCase("v6-dark-right-countdown-compact")
    @Test fun testGuide_v6_dark_right_close_compact() = runGuideCase("v6-dark-right-close-compact")
    @Test fun testGuide_v6_dark_shuffle_countdown_compact() = runGuideCase("v6-dark-shuffle-countdown-compact")
    @Test fun testGuide_v6_dark_shuffle_close_compact() = runGuideCase("v6-dark-shuffle-close-compact")
    @Test fun testGuide_v6_light_left_countdown_compact() = runGuideCase("v6-light-left-countdown-compact")
    @Test fun testGuide_v6_light_left_close_compact() = runGuideCase("v6-light-left-close-compact")
    @Test fun testGuide_v6_light_right_countdown_compact() = runGuideCase("v6-light-right-countdown-compact")
    @Test fun testGuide_v6_light_right_close_compact() = runGuideCase("v6-light-right-close-compact")
    @Test fun testGuide_v6_light_shuffle_countdown_compact() = runGuideCase("v6-light-shuffle-countdown-compact")
    @Test fun testGuide_v6_light_shuffle_close_compact() = runGuideCase("v6-light-shuffle-close-compact")

    @Test fun testRegression_v4_portrait() {
        captureNativeInterstitialTemplate(NativeTemplateSize.FullInterstitialV4, NativeInterstitialV4View::class.java, landscape = false, reviewRegression = true)
    }
    @Test fun testRegression_v4_landscape() {
        captureNativeInterstitialTemplate(NativeTemplateSize.FullInterstitialV4, NativeInterstitialV4View::class.java, landscape = true, reviewRegression = true)
    }
    @Test fun testRegression_v5_portrait() {
        captureNativeInterstitialTemplate(NativeTemplateSize.FullInterstitialV5, NativeInterstitialV5View::class.java, landscape = false, reviewRegression = true)
    }
    @Test fun testRegression_v5_landscape() {
        captureNativeInterstitialTemplate(NativeTemplateSize.FullInterstitialV5, NativeInterstitialV5View::class.java, landscape = true, reviewRegression = true)
    }
    @Test fun testRegression_v6_portrait() {
        captureNativeInterstitialTemplate(NativeTemplateSize.FullInterstitialV6, NativeInterstitialV6View::class.java, landscape = false, reviewRegression = true)
    }
    @Test fun testRegression_v6_landscape() {
        captureNativeInterstitialTemplate(NativeTemplateSize.FullInterstitialV6, NativeInterstitialV6View::class.java, landscape = true, reviewRegression = true)
    }

    @Test fun testThemeShuffle_v4_portrait() = runThemeShuffle(4, false)
    @Test fun testThemeShuffle_v4_landscape() = runThemeShuffle(4, true)
    @Test fun testThemeShuffle_v5_portrait() = runThemeShuffle(5, false)
    @Test fun testThemeShuffle_v5_landscape() = runThemeShuffle(5, true)
    @Test fun testThemeShuffle_v6_portrait() = runThemeShuffle(6, false)
    @Test fun testThemeShuffle_v6_landscape() = runThemeShuffle(6, true)

    @Test fun testTemplateShuffle_portrait() = runTemplateShuffle(false)
    @Test fun testTemplateShuffle_landscape() = runTemplateShuffle(true)

    private fun runTemplateShuffle(landscape: Boolean) {
        val case = org.json.JSONArray(instrumentation.context.assets.open("native-template-shuffle-cases.json").bufferedReader().use { it.readText() }).getJSONObject(0)
        val expected = (4..6).flatMap { v -> listOf("v$v-light", "v$v-dark") }.toSet()
        val seen = mutableSetOf<String?>()
        repeat(60) {
            seen += captureNativeInterstitialTemplate(NativeTemplateSize.FullInterstitialV4,
                com.core.ads.customviews.ads.NativeInterstitialConceptView::class.java, landscape, theme = "dark", guideCase = case)
            if (seen == expected) return
        }
        fail("Did not observe all template/theme combinations after 60 binds: $seen")
    }

    private fun runThemeShuffle(version: Int, landscape: Boolean) {
        val cases = org.json.JSONArray(instrumentation.context.assets.open("native-theme-shuffle-cases.json").bufferedReader().use { it.readText() })
        val case = (0 until cases.length()).map { cases.getJSONObject(it) }.single { it.getInt("version") == version }
        val template = when (version) { 4 -> NativeTemplateSize.FullInterstitialV4; 5 -> NativeTemplateSize.FullInterstitialV5; else -> NativeTemplateSize.FullInterstitialV6 }
        val viewClass = when (version) { 4 -> NativeInterstitialV4View::class.java; 5 -> NativeInterstitialV5View::class.java; else -> NativeInterstitialV6View::class.java }
        val seen = mutableSetOf<String?>()
        // Real production RNG: cap retries to make an unlikely/missing outcome an explicit failure.
        repeat(20) {
            seen += captureNativeInterstitialTemplate(template, viewClass, landscape, theme = "dark", guideCase = case)
            if (seen == setOf("light", "dark")) return
        }
        fail("Shuffle did not produce both themes after 20 binds: $seen")
    }

    private fun concept(view: View): com.core.ads.customviews.ads.NativeInterstitialConceptView? =
        view as? com.core.ads.customviews.ads.NativeInterstitialConceptView
            ?: (view as? ViewGroup)?.let { group -> (0 until group.childCount).firstNotNullOfOrNull { concept(group.getChildAt(it)) } }

    private fun NativeAdPlace.expectedTheme(theme: String?): NativeAdPlace {
        val colors = nativeThemeColors?.get(theme) ?: return this
        return copy(
            backgroundColor = colors["background_color"] ?: backgroundColor,
            backgroundFullColor = colors["background_full_color"] ?: backgroundFullColor,
            mediaBackgroundColor = colors["media_background_color"] ?: mediaBackgroundColor,
            primaryTextColor = colors["primary_text_color"] ?: primaryTextColor,
            bodyTextColor = colors["body_text_color"] ?: bodyTextColor,
            ctaTextColor = colors["cta_text_color"] ?: ctaTextColor,
            backgroundCta = colors["background_cta"] ?: backgroundCta,
            borderColor = colors["border_color"] ?: borderColor,
            backgroundColorAdsNotifyView = colors["background_color_ads_notify_view"] ?: backgroundColorAdsNotifyView,
            textColorAdsNotifyView = colors["text_color_ads_notify_view"] ?: textColorAdsNotifyView,
            progressBarTint = colors["progress_bar_tint"] ?: colors["primary_text_color"] ?: progressBarTint,
        )
    }

    private fun runGuideCase(id: String) {
        val cases = org.json.JSONArray(instrumentation.context.assets.open("native-interstitial-guide-cases.json").bufferedReader().use { it.readText() })
        val case = (0 until cases.length()).map { cases.getJSONObject(it) }.single { it.getString("id") == id }
        val template = when (case.getInt("version")) {
            4 -> NativeTemplateSize.FullInterstitialV4
            5 -> NativeTemplateSize.FullInterstitialV5
            else -> NativeTemplateSize.FullInterstitialV6
        }
        val viewClass = when (case.getInt("version")) {
            4 -> NativeInterstitialV4View::class.java
            5 -> NativeInterstitialV5View::class.java
            else -> NativeInterstitialV6View::class.java
        }
        for (landscape in listOf(false, true)) {
            verifyNativeInterstitialTemplate(template, viewClass, landscape, theme = case.getString("theme"), guideCase = case)
        }
    }

    private fun NativeAdPlace.withGuideConfig(config: org.json.JSONObject): NativeAdPlace {
        assertEquals(placeName.name, config.getString("place_name"))
        assertEquals(adId, config.getString("ad_id"))
        assertEquals("native_interstitial", config.getString("ad_type"))
        assertTrue(config.getString("native_template_size") in listOf(nativeTemplateSize.key, "full_interstitial_shuffle"))
        return copy(
            isEnable = config.getBoolean("is_enable"),
            isTrackingClick = config.getBoolean("is_tracking_click"),
            isAutoLoadAfterDismiss = config.getBoolean("is_auto_load_after_dismiss"),
            backgroundColor = config.getString("background_color"),
            backgroundFullColor = config.getString("background_full_color"),
            mediaBackgroundColor = config.getString("media_background_color"),
            backgroundRadius = config.getInt("background_radius"),
            primaryTextColor = config.getString("primary_text_color"),
            bodyTextColor = config.getString("body_text_color"),
            ctaTextColor = config.getString("cta_text_color"),
            backgroundCta = config.getString("background_cta"),
            ctaRadius = config.getInt("cta_radius_dp"),
            borderColor = config.getString("border_color"),
            backgroundColorAdsNotifyView = config.getString("background_color_ads_notify_view"),
            textColorAdsNotifyView = config.getString("text_color_ads_notify_view"),
            controlClosePosition = config.getString("control_close_position"),
            controlCloseStyle = config.getString("control_close_style"),
            controlCloseReadyStyle = if (config.has("control_close_ready_style")) config.getString("control_close_ready_style") else null,
            nativeTemplateSize = NativeTemplateSize.getSizeBy(config.getString("native_template_size")),
            nativeTheme = config.optString("native_theme").takeIf { it.isNotBlank() },
            nativeThemeColors = config.optJSONObject("native_theme_colors")?.let { palettes ->
                palettes.keys().asSequence().associateWith { key ->
                    val colors = palettes.getJSONObject(key)
                    colors.keys().asSequence().associateWith { colors.getString(it) }
                }
            },
            countDownTimer = config.getInt("count_down_timer"),
            progressBarTint = config.getString("progress_bar_tint"),
            hideTextCountDown = config.getBoolean("hide_text_count_down"),
            hideProgressCountDown = config.getBoolean("hide_progress_count_down"),
        )
    }

    private fun verifyNativeInterstitialTemplate(
        template: NativeTemplateSize,
        viewClass: Class<out View>,
        landscape: Boolean = true,
        theme: String? = null,
        shufflePosition: Boolean = false,
        guideCase: org.json.JSONObject? = null,
    ) {
        captureNativeInterstitialTemplate(template, viewClass, landscape, theme, shufflePosition, guideCase)
    }

    private fun captureNativeInterstitialTemplate(
        template: NativeTemplateSize,
        viewClass: Class<out View>,
        landscape: Boolean = true,
        theme: String? = null,
        shufflePosition: Boolean = false,
        guideCase: org.json.JSONObject? = null,
        reviewRegression: Boolean = false,
    ): String? {
        var actualTheme: String? = null
        var actualVersion: Int? = null
        val shuffleTemplate = guideCase?.getJSONObject("config")?.optString("native_template_size") == "full_interstitial_shuffle"
        val orientationLabel = if (landscape) "landscape" else "portrait"
        val screenshotTheme = if (shufflePosition) "shuffle" else theme
        var guidePrefix = guideCase?.let { "guide-${it.getString("id")}-$orientationLabel" }
        val expectedOrientation = if (landscape) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
        val newConcept = template in listOf(NativeTemplateSize.FullInterstitialV4, NativeTemplateSize.FullInterstitialV5, NativeTemplateSize.FullInterstitialV6)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        ActivityScenario.launch(NativeGalleryActivity::class.java).use { scenario ->
            try {
                scenario.onActivity {
                    it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    it.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                var orientedActivity: NativeGalleryActivity? = null
                val rotationDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
                while (orientedActivity == null && System.nanoTime() < rotationDeadline) {
                    scenario.onActivity {
                        if (it.resources.configuration.orientation == expectedOrientation) {
                            orientedActivity = it
                        }
                    }
                    if (orientedActivity == null) Thread.sleep(200)
                }
                assertNotNull("Activity must reach $orientationLabel", orientedActivity)
                instrumentation.waitForIdleSync()
                scenario.onActivity {
                    it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    orientedActivity = it
                }
                val activity = requireNotNull(orientedActivity)
                initializeSdk()
                val basePlaceName = when (template) {
                    NativeTemplateSize.FullInterstitialV4 -> AppAdPlaceName.FULLSCREEN_NATIVE_INTERSTITIAL_V4
                    NativeTemplateSize.FullInterstitialV5 -> AppAdPlaceName.FULLSCREEN_NATIVE_INTERSTITIAL_V5
                    NativeTemplateSize.FullInterstitialV6 -> AppAdPlaceName.FULLSCREEN_NATIVE_INTERSTITIAL_V6
                    else -> AppAdPlaceName.FULLSCREEN_NATIVE_INTERSTITIAL
                }
                val placeName = if (theme == null) basePlaceName else requireNotNull(
                    AppAdPlaceName.fromKey("${basePlaceName.name}_$theme"))
                requireTestUnits(activity, placeName)
                val events = LinkedBlockingQueue<AdFullScreenUiResource>()
                onMain {
                    activity.adsManager.releaseBannerNative(placeName)
                    scope.launch {
                        activity.adsManager.adFullScreenFlow.collect {
                            if (it.rootAdPlaceName == placeName) events.offer(it)
                        }
                    }
                }
                activity.adsManager.loadFullscreenAd(activity, placeName, template.key)
                val loaded = awaitEvent(events, "${template.key} $orientationLabel load") {
                    it is AdFullScreenUiResource.AdLoaded || it is AdFullScreenUiResource.AdNotValidOrLoadFailed
                }
                assertTrue("Native load: $loaded", loaded is AdFullScreenUiResource.AdLoaded)
                val closed = CountDownLatch(1)
                lateinit var dialog: DialogNativeFakeInterstitial
                var initialCloseGravity: Int? = null
                val readyAppearance = org.json.JSONObject()
                onMain {
                    val holder = requireNotNull(activity.adsManager.getNativeHolder(activity, placeName))
                    if (newConcept) assertSame("New template must be selected by config", template, (holder.adPlace as NativeAdPlace).nativeTemplateSize)
                    else holder.adPlace = (holder.adPlace as NativeAdPlace).copy(nativeTemplateSize = template)
                    // Model-level config fixture; production factory/style/render pipeline remains unchanged.
                    if (shufflePosition) holder.adPlace = (holder.adPlace as NativeAdPlace).copy(controlClosePosition = "shuffle")
                    if (guideCase != null) holder.adPlace = (holder.adPlace as NativeAdPlace).withGuideConfig(guideCase.getJSONObject("config"))
                    dialog = DialogNativeFakeInterstitial.newInstance(placeName).apply {
                        onClose = { closed.countDown() }
                    }
                    dialog.showNow(activity.supportFragmentManager, "$orientationLabel-${template.key}")
                    assertEquals(expectedOrientation, activity.resources.configuration.orientation)
                    assertTrue("Expected ${viewClass.simpleName}", containsView(dialog.requireView(), viewClass))
                    if (newConcept) {
                        val root = dialog.requireView()
                        actualTheme = concept(root)?.resolvedNativeTheme
                        actualVersion = when (concept(root)) {
                            is NativeInterstitialV4View -> 4
                            is NativeInterstitialV5View -> 5
                            is NativeInterstitialV6View -> 6
                            else -> null
                        }
                        if (shuffleTemplate) {
                            assertTrue("Only V4/V5/V6 may be shuffled", actualVersion in 4..6)
                            guidePrefix = "$guidePrefix-v$actualVersion"
                        }
                        if (guideCase?.getJSONObject("config")?.optString("native_theme") == "shuffle") {
                            assertTrue("Shuffle must choose a named palette", actualTheme in listOf("light", "dark"))
                            guidePrefix = "$guidePrefix-$actualTheme"
                        }
                        val configured = (holder.adPlace as NativeAdPlace).expectedTheme(actualTheme)
                        initialCloseGravity = (root.findViewById<View>(com.core.ads.R.id.controlClose).layoutParams as android.widget.FrameLayout.LayoutParams).gravity
                        val closeStyle = configured.controlCloseStyle?.trim()?.equals("close", ignoreCase = true) == true
                        val close = root.findViewById<View>(com.core.ads.R.id.tvClose)
                        val progress = root.findViewById<com.google.android.material.progressindicator.CircularProgressIndicator>(com.core.ads.R.id.actionProgress)
                        if ((configured.countDownTimer ?: 5) > 0) {
                            assertEquals("Close visibility must follow remote style", if (closeStyle) View.VISIBLE else View.INVISIBLE, close.visibility)
                            assertFalse("Close must stay disabled during countdown", close.isEnabled)
                            close.performClick()
                            assertEquals("Early tap must not dismiss", 1L, closed.count)
                        }
                        assertEquals("Progress visibility must follow remote style",
                            closeStyle && configured.hideProgressCountDown != true, progress.visibility == View.VISIBLE)
                        assertEquals("Progress color must come from config",
                            Color.parseColor(configured.progressBarTint ?: requireNotNull(configured.primaryTextColor)), progress.indicatorColor.single())
                        log("CLOSE_STYLE ${template.key} $orientationLabel: ${configured.controlCloseStyle}")
                    }
                }
                try {
                    if (guideCase != null) {
                        Thread.sleep(750)
                        onMain {
                            val root = dialog.requireView()
                            assertFalse("Waiting screenshot must precede enabled close", root.findViewById<View>(com.core.ads.R.id.tvClose).isEnabled)
                            val waitingClose = root.findViewById<View>(com.core.ads.R.id.tvClose)
                            assertEquals("Waiting close retains 48dp", root.resources.getDimensionPixelSize(com.core.ads.R.dimen.native_interstitial_close_default_size), waitingClose.width)
                            assertEquals("Waiting close retains 12dp padding", root.resources.getDimensionPixelSize(com.core.ads.R.dimen.native_interstitial_close_default_padding), waitingClose.paddingLeft)
                            assertNotNull("Waiting close retains background", root.findViewById<View>(com.core.ads.R.id.controlClose).background)
                            val numeric = guideCase.getString("style") == "countdown"
                            assertEquals("Numeric countdown visibility", numeric, root.findViewById<View>(com.core.ads.R.id.lnCountDown).isShown)
                            assertEquals("Progress visibility", !numeric, root.findViewById<View>(com.core.ads.R.id.actionProgress).isShown)
                            if (numeric) assertTrue("Countdown text must contain a remaining second",
                                root.findViewById<TextView>(com.core.ads.R.id.tvCountDownTime).text.any(Char::isDigit))
                        }
                        captureNativeInterstitialScreenshot("$guidePrefix-waiting.png", landscape)
                    }
                    if (newConcept && theme == null && !shufflePosition) {
                        Thread.sleep(750)
                        captureNativeInterstitialScreenshot("${template.key}-$orientationLabel-close-style-waiting.png", landscape)
                    }
                    // Wait for the actual close control; V3 can require Next before Close.
                    val closeDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(25)
                    var ready = false
                    while (!ready && System.nanoTime() < closeDeadline) {
                        onMain {
                            val next = dialog.view?.findViewById<View>(com.core.ads.R.id.tvNext)
                            if (next?.isShown == true && next.isEnabled) next.performClick()
                            val close = dialog.view?.findViewById<View>(com.core.ads.R.id.tvClose)
                            ready = close?.isShown == true && close.isEnabled
                        }
                        if (!ready) Thread.sleep(200)
                    }
                    Thread.sleep(1_000) // Let media and window animations settle before capturing.
                    captureNativeInterstitialScreenshot(guidePrefix?.let { "$it-ready.png" }
                        ?: "${template.key}-$orientationLabel${screenshotTheme?.let { "-$it" } ?: if (newConcept) "-close-style" else ""}.png", landscape)
                    assertTrue("Close control must become available", ready)
                    var compactTapPoint: android.graphics.Point? = null
                    onMain {
                        val root = dialog.requireView()
                        assertTrue("Dialog must have $orientationLabel dimensions",
                            if (landscape) root.width > root.height else root.height > root.width)
                        listOf(com.core.ads.R.id.cta, com.core.ads.R.id.primary, com.core.ads.R.id.body, com.core.ads.R.id.tvClose,
                            com.core.ads.R.id.media_view).forEach { id ->
                            val view = root.findViewById<View>(id)
                            val visible = Rect()
                            val label = root.resources.getResourceEntryName(id)
                            assertTrue("$label must be visible", view.getGlobalVisibleRect(visible))
                            assertEquals("$label clipped horizontally", view.width, visible.width())
                            assertEquals("$label clipped vertically", view.height, visible.height())
                        }
                        if (template != NativeTemplateSize.FullInterstitialV3) {
                            assertEquals("Theme must remain stable through countdown", actualTheme, concept(root)?.resolvedNativeTheme)
                            assertEquals("Template must remain stable", actualVersion, when (concept(root)) {
                                is NativeInterstitialV4View -> 4; is NativeInterstitialV5View -> 5; is NativeInterstitialV6View -> 6; else -> null
                            })
                            val configuredPlace = (requireNotNull(activity.adsManager.getNativeHolder(activity, placeName)).adPlace as NativeAdPlace).expectedTheme(actualTheme)
                            listOf(
                                com.core.ads.R.id.primary to requireNotNull(configuredPlace.primaryTextColor),
                                com.core.ads.R.id.body to requireNotNull(configuredPlace.bodyTextColor),
                            ).forEach { (id, configuredColor) ->
                                val text = root.findViewById<TextView>(id)
                                val label = text.resources.getResourceEntryName(id)
                                assertEquals("${template.key} $label must match config", Color.parseColor(configuredColor), text.currentTextColor)
                                log("CONFIG_COLOR ${template.key} $orientationLabel $label: $configuredColor matches rendered text")
                            }
                            val media = root.findViewById<View>(com.core.ads.R.id.media_view)
                            if (newConcept) {
                                val compact = configuredPlace.controlCloseReadyStyle?.trim()?.equals("compact", ignoreCase = true) == true
                                listOf(
                                    com.core.ads.R.id.native_ad_view to configuredPlace.backgroundFullColor,
                                    com.core.ads.R.id.background to configuredPlace.backgroundColor,
                                    com.core.ads.R.id.media_view to configuredPlace.mediaBackgroundColor,
                                    com.core.ads.R.id.layout_cta to configuredPlace.backgroundCta,
                                ).forEach { (id, color) ->
                                    val bg = root.findViewById<View>(id).background
                                    val actual = when (bg) {
                                        is android.graphics.drawable.ColorDrawable -> bg.color
                                        is android.graphics.drawable.GradientDrawable -> requireNotNull(bg.color).defaultColor
                                        else -> error("Unsupported background for $id: $bg")
                                    }
                                    assertEquals("${template.key} $theme background must match config: $id",
                                        Color.parseColor(requireNotNull(color)), actual)
                                }
                                val closeImage = root.findViewById<android.widget.ImageView>(com.core.ads.R.id.tvClose)
                                val closeContainer = root.findViewById<View>(com.core.ads.R.id.controlClose)
                                if (compact) assertNull("Compact close has no outer background", closeContainer.background)
                                else assertEquals("Default close background still comes from config",
                                    Color.parseColor(requireNotNull(configuredPlace.backgroundColor)),
                                    (closeContainer.background as android.graphics.drawable.GradientDrawable).color!!.defaultColor)
                                val expectedCloseSize = root.resources.getDimensionPixelSize(if (compact) com.core.ads.R.dimen.native_interstitial_close_compact_size else com.core.ads.R.dimen.native_interstitial_close_default_size)
                                val expectedClosePadding = root.resources.getDimensionPixelSize(if (compact) com.core.ads.R.dimen.native_interstitial_close_compact_padding else com.core.ads.R.dimen.native_interstitial_close_default_padding)
                                assertEquals("Close width follows ready style", expectedCloseSize, closeImage.width)
                                assertEquals("Close height follows ready style", expectedCloseSize, closeImage.height)
                                listOf(closeImage.paddingLeft, closeImage.paddingRight, closeImage.paddingTop, closeImage.paddingBottom)
                                    .forEach { assertEquals("Close padding follows ready style", expectedClosePadding, it) }
                                val readyProgress = root.findViewById<View>(com.core.ads.R.id.actionProgress)
                                assertEquals("Ready progress follows style", !compact && configuredPlace.controlCloseStyle == "close" && configuredPlace.hideProgressCountDown != true, readyProgress.isShown)
                                readyAppearance.put("closeSizePx", closeImage.width).put("closePaddingPx", closeImage.paddingLeft)
                                    .put("progressVisible", readyProgress.isShown).put("hasBackground", closeContainer.background != null)
                                    .put("touchSizePx", closeContainer.width).put("density", root.resources.displayMetrics.density.toDouble())
                                assertEquals("X color must match config",
                                    android.graphics.PorterDuffColorFilter(Color.parseColor(requireNotNull(configuredPlace.primaryTextColor)), android.graphics.PorterDuff.Mode.SRC_ATOP),
                                    closeImage.colorFilter)
                                val closeBounds = Rect().also { root.findViewById<View>(com.core.ads.R.id.tvClose).getGlobalVisibleRect(it) }
                                val badgeBounds = Rect().also { root.findViewById<View>(com.core.ads.R.id.ad_notification_view).getGlobalVisibleRect(it) }
                                val screenBounds = Rect().also { root.getGlobalVisibleRect(it) }
                                val closeGravity = (root.findViewById<View>(com.core.ads.R.id.controlClose).layoutParams as android.widget.FrameLayout.LayoutParams).gravity
                                assertEquals("Close side must remain stable during countdown", initialCloseGravity, closeGravity)
                                val closeOnLeft = when (configuredPlace.controlClosePosition?.trim()?.lowercase()) {
                                    "shuffle" -> {
                                        assertTrue("Shuffle must select left or right", closeGravity in listOf(
                                            android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START,
                                            android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END))
                                        closeGravity == (android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START)
                                    }
                                    "left", "start" -> true
                                    else -> false
                                }
                                assertTrue("Close must follow control_close_position=${configuredPlace.controlClosePosition}",
                                    if (closeOnLeft) closeBounds.right < screenBounds.centerX() else closeBounds.left > screenBounds.centerX())
                                assertTrue("Ad badge must be on the opposite side of Close",
                                    if (closeOnLeft) badgeBounds.left > screenBounds.centerX() else badgeBounds.right < screenBounds.centerX())
                                assertFalse("Ad badge and Close must not overlap", Rect.intersects(closeBounds, badgeBounds))
                                log("CLOSE_POSITION ${template.key} $orientationLabel: ${configuredPlace.controlClosePosition}, close=$closeBounds, badge=$badgeBounds")
                                assertEquals("Orientation-specific concept layout", landscape,
                                    root.findViewById<View>(com.core.ads.R.id.concept_media_end) != null)
                                assertTrue("Concept media must remain large", media.width >= root.width * 0.40f && media.height >= root.height * 0.30f)
                                val mediaBounds = Rect().also { media.getGlobalVisibleRect(it) }
                                listOf(com.core.ads.R.id.primary, com.core.ads.R.id.body, com.core.ads.R.id.cta).forEach { id ->
                                    val bounds = Rect().also { root.findViewById<View>(id).getGlobalVisibleRect(it) }
                                    assertFalse("Media must not overlap text or CTA", Rect.intersects(mediaBounds, bounds))
                                }
                                val density = root.resources.displayMetrics.density
                                assertTrue("CTA touch height", root.findViewById<View>(com.core.ads.R.id.cta).height >= 48 * density)
                                assertTrue("Close touch width", closeContainer.width >= 48 * density)
                                assertTrue("Close touch height", closeContainer.height >= 48 * density)
                                assertEquals(Color.parseColor(requireNotNull(configuredPlace.ctaTextColor)),
                                    root.findViewById<TextView>(com.core.ads.R.id.cta).currentTextColor)
                            } else if (landscape) {
                                assertTrue("Landscape media width must be at least 45% of dialog", media.width >= root.width * 0.45f)
                                assertTrue("Landscape media height must be at least 65% of dialog", media.height >= root.height * 0.65f)
                            } else {
                                assertNull("Portrait must not inflate the landscape layout", root.findViewById<View>(com.core.ads.R.id.media_end))
                                val mediaBounds = Rect().also { media.getGlobalVisibleRect(it) }
                                val cardBounds = Rect().also { root.findViewById<View>(com.core.ads.R.id.background).getGlobalVisibleRect(it) }
                                assertTrue("Portrait media must be above the content card", mediaBounds.bottom <= cardBounds.top)
                                val textBounds = listOf(com.core.ads.R.id.primary, com.core.ads.R.id.body, com.core.ads.R.id.cta).map { id ->
                                    Rect().also { root.findViewById<View>(id).getGlobalVisibleRect(it) }
                                }
                                textBounds.zipWithNext().forEach { (above, below) ->
                                    assertTrue("Portrait title, body and CTA must remain vertically stacked", above.bottom <= below.top)
                                }
                            }
                            log("MEDIA ${template.key} $orientationLabel: ${media.width}x${media.height}, dialog ${root.width}x${root.height}")
                        }
                        val compactReady = guideCase?.getJSONObject("config")?.optString("control_close_ready_style") == "compact"
                        if (compactReady) {
                            val touchBounds = Rect().also { root.findViewById<View>(com.core.ads.R.id.controlClose).getGlobalVisibleRect(it) }
                            val iconBounds = Rect().also { root.findViewById<View>(com.core.ads.R.id.tvClose).getGlobalVisibleRect(it) }
                            // Tap inside the transparent 48dp target, outside the 28dp icon.
                            compactTapPoint = android.graphics.Point(touchBounds.left + 2, touchBounds.centerY())
                            assertFalse("Test tap must be outside the visible icon", iconBounds.contains(compactTapPoint!!.x, compactTapPoint!!.y))
                        }
                        else if (!reviewRegression) root.findViewById<View>(com.core.ads.R.id.tvClose).performClick()
                    }
                    if (reviewRegression) {
                        verifyOptionalIconAndThemeFallback(dialog, requireNotNull(actualVersion), landscape)
                        onMain { dialog.requireView().findViewById<View>(com.core.ads.R.id.tvClose).performClick() }
                    }
                    compactTapPoint?.let { point ->
                        val now = android.os.SystemClock.uptimeMillis()
                        listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP).forEachIndexed { index, action ->
                            val event = android.view.MotionEvent.obtain(now, now + index * 30L, action, point.x.toFloat(), point.y.toFloat(), 0)
                            try { instrumentation.sendPointerSync(event) } finally { event.recycle() }
                        }
                    }
                    assertTrue("Close callback must fire", closed.await(5, TimeUnit.SECONDS))
                    instrumentation.waitForIdleSync()
                    onMain { assertFalse("Dialog must dismiss", dialog.isAdded) }
                    if (guideCase != null) {
                        val evidence = org.json.JSONObject().put("caseId", guideCase.getString("id"))
                            .put("orientation", orientationLabel).put("config", guideCase.getJSONObject("config"))
                            .put("actualClosePosition", if (initialCloseGravity == (android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START)) "left" else "right")
                            .put("actualTemplate", "full_interstitial_v$actualVersion").put("actualTheme", actualTheme ?: "base").put("readyAppearance", readyAppearance)
                            .put("passed", true).put("device", android.os.Build.MODEL).put("api", android.os.Build.VERSION.SDK_INT)
                        val pipes = instrumentation.uiAutomation.executeShellCommandRw("dd of=/data/local/tmp/native-interstitial-$orientationLabel/$guidePrefix.json")
                        ParcelFileDescriptor.AutoCloseOutputStream(pipes[1]).use { it.write(evidence.toString(2).toByteArray()) }
                        ParcelFileDescriptor.AutoCloseInputStream(pipes[0]).use { it.readBytes() }
                    }
                    log("PASS ${template.key}: $orientationLabel render, visible CTA/title/body/close, screenshot and dismissal")
                } finally {
                    onMain {
                        if (dialog.isAdded) dialog.dismissNow()
                        activity.adsManager.releaseBannerNative(placeName)
                    }
                }
            } finally {
                scope.cancel()
            }
        }
        return if (shuffleTemplate) "v$actualVersion-$actualTheme" else actualTheme
    }

    /** Optional-asset simulation on a real Google test ad; no changes to SDK creative text. */
    private fun verifyOptionalIconAndThemeFallback(dialog: DialogNativeFakeInterstitial, version: Int, landscape: Boolean) {
        val orientation = if (landscape) "landscape" else "portrait"
        lateinit var root: com.core.ads.customviews.ads.NativeInterstitialConceptView
        var baseColors: List<Any?> = emptyList()
        fun renderedColors(): List<Any?> = buildList {
            listOf(com.core.ads.R.id.primary, com.core.ads.R.id.body, com.core.ads.R.id.cta,
                com.core.ads.R.id.tvCountDownTime, com.core.ads.R.id.ad_notification_view).forEach {
                add(root.findViewById<TextView>(it).currentTextColor)
            }
            listOf(com.core.ads.R.id.background, com.core.ads.R.id.native_ad_view, com.core.ads.R.id.media_view,
                com.core.ads.R.id.layout_cta, com.core.ads.R.id.controlClose, com.core.ads.R.id.ad_notification_view).forEach {
                add(when (val background = root.findViewById<View>(it).background) {
                    null -> null
                    is android.graphics.drawable.ColorDrawable -> background.color
                    is android.graphics.drawable.GradientDrawable -> background.color?.defaultColor
                    else -> error("Unexpected drawable $background")
                })
            }
            add(root.findViewById<android.widget.ImageView>(com.core.ads.R.id.tvClose).colorFilter)
        }
        onMain {
            root = requireNotNull(concept(dialog.requireView()))
            baseColors = renderedColors()
            root.findViewById<View>(com.core.ads.R.id.icon).visibility = View.GONE
            root.requestLayout()
        }
        awaitRenderedFrame(root)
        onMain {
            val bounds = listOf(com.core.ads.R.id.primary, com.core.ads.R.id.body, com.core.ads.R.id.cta).map {
                val view = root.findViewById<View>(it)
                Rect().also { rect ->
                    assertTrue("Missing-icon asset is visible", view.getGlobalVisibleRect(rect))
                    assertEquals("Missing-icon asset is not clipped horizontally", view.width, rect.width())
                    assertEquals("Missing-icon asset is not clipped vertically", view.height, rect.height())
                }
            }
            bounds.zipWithNext().forEach { (above, below) -> assertTrue("Missing icon must not overlap headline/body/CTA", above.bottom <= below.top) }
        }
        captureNativeInterstitialScreenshot("review-v$version-$orientation-no-icon.png", landscape)
        onMain {
            val keys = listOf("background_color", "background_full_color", "media_background_color", "primary_text_color",
                "body_text_color", "cta_text_color", "background_cta", "background_color_ads_notify_view", "text_color_ads_notify_view")
            root.applyStyles(com.core.ads.customviews.ads.NativeTemplateStyle.Builder()
                .withControlCloseStyle("close").withControlClosePosition("left")
                .withNativeTheme("dark", mapOf("dark" to keys.associateWith { "#EC4899" })).build())
            assertEquals("Apply palette before verifying restoration", Color.parseColor("#EC4899"), root.findViewById<TextView>(com.core.ads.R.id.primary).currentTextColor)
            root.applyStyles(com.core.ads.customviews.ads.NativeTemplateStyle.Builder()
                .withControlCloseStyle("close").withControlClosePosition("left")
                .withNativeTheme("light", mapOf("light" to null)).build())
            assertEquals("Missing palette and missing base fields restore base colors", baseColors, renderedColors())
            root.onHostPause(); root.onHostResume()
            assertEquals("Resume preserves restored colors", baseColors, renderedColors())
            assertTrue("Theme changes must not restart the completed countdown", root.findViewById<View>(com.core.ads.R.id.tvClose).isEnabled)
            log("PASS REGRESSION V$version $orientation: no-icon geometry, palette restoration, countdown stability")
        }
        awaitRenderedFrame(root)
        captureNativeInterstitialScreenshot("review-v$version-$orientation-theme-fallback.png", landscape)
    }

    private fun awaitRenderedFrame(view: View) {
        val committed = CountDownLatch(1)
        onMain {
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                view.viewTreeObserver.registerFrameCommitCallback { committed.countDown() }
                view.invalidate()
            } else {
                view.postOnAnimation { view.postOnAnimation { committed.countDown() } }
            }
        }
        assertTrue("Updated UI frame must be committed before geometry checks and screenshot", committed.await(5, TimeUnit.SECONDS))
        // Allow the committed buffer to reach the screen compositor used by takeScreenshot.
        Thread.sleep(100)
        instrumentation.waitForIdleSync()
        onMain { assertFalse("Screenshot must not race pending layout", view.isLayoutRequested) }
    }

    private fun containsView(view: View, type: Class<out View>): Boolean =
        type.isInstance(view) || view is ViewGroup && (0 until view.childCount).any {
            containsView(view.getChildAt(it), type)
        }

    private fun captureNativeInterstitialScreenshot(name: String, landscape: Boolean) {
        val orientation = if (landscape) "landscape" else "portrait"
        val directory = "/data/local/tmp/native-interstitial-$orientation"
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            assertTrue("Screenshot must be $orientation",
                if (landscape) bitmap.width > bitmap.height else bitmap.height > bitmap.width)
            // Shell-owned artifacts survive Gradle's uninstall of the instrumented APK.
            ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
                "mkdir -p $directory",
            )).use { it.readBytes() }
            val pipes = instrumentation.uiAutomation.executeShellCommandRw(
                "dd of=$directory/$name",
            )
            ParcelFileDescriptor.AutoCloseOutputStream(pipes[1]).use {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
            ParcelFileDescriptor.AutoCloseInputStream(pipes[0]).use { it.readBytes() }
            log("SCREENSHOT $name ${bitmap.width}x${bitmap.height}")
        } finally {
            bitmap.recycle()
        }
    }

    private fun requireTestUnits(activity: NativeGalleryActivity, vararg places: IAdPlaceName) = onMain {
        places.forEach {
            val place = activity.remoteConfigRepository.getAdPlaceBy(it)
            assertTrue("Expected Google test unit for ${it.name}: ${place.adId}", place.adId.startsWith("ca-app-pub-3940256099942544/"))
            assertTrue(place.highFloorAdIds.all { id -> id.startsWith("ca-app-pub-3940256099942544/") })
        }
    }

    private fun closeTestAd(events: LinkedBlockingQueue<AdFullScreenUiResource>) {
        val start = android.os.SystemClock.elapsedRealtime()
        var lastBack = start
        while (android.os.SystemClock.elapsedRealtime() - start < 60_000) {
            if (events.poll(500, TimeUnit.MILLISECONDS) is AdFullScreenUiResource.AdCompleted) return
            val root = instrumentation.uiAutomation.rootInActiveWindow
            findClose(root)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            val now = android.os.SystemClock.elapsedRealtime()
            if (now - start > 15_000 && now - lastBack > 5_000) {
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                lastBack = now
            }
        }
        fail("Rewarded ad did not dismiss within 60 seconds")
    }

    private fun findClose(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        val labels = setOf("close", "close ad", "dismiss ad", "đóng", "đóng quảng cáo")
        if (node.isClickable && (node.contentDescription?.toString()?.lowercase() in labels || node.text?.toString()?.lowercase() in labels)) return node
        for (i in 0 until node.childCount) findClose(node.getChild(i))?.let { return it }
        return null
    }

    private fun <T : Any> awaitEvent(queue: LinkedBlockingQueue<T>, label: String, accept: (T) -> Boolean): T {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(55)
        while (System.nanoTime() < deadline) {
            val event = queue.poll(1, TimeUnit.SECONDS) ?: continue
            log("$label: ${event.javaClass.simpleName}")
            if (accept(event)) return event
        }
        throw AssertionError("Timed out waiting for $label")
    }

    private fun screenshot(name: String) {
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        File(instrumentation.targetContext.cacheDir, name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    private fun withActivity(block: (NativeGalleryActivity, CoroutineScope) -> Unit) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        ActivityScenario.launch(NativeGalleryActivity::class.java).use { scenario ->
            lateinit var activity: NativeGalleryActivity
            scenario.onActivity { activity = it }
            try { block(activity, scope) } finally { scope.cancel() }
        }
    }

    private fun onMain(block: () -> Unit) {
        val failure = java.util.concurrent.atomic.AtomicReference<Throwable>()
        instrumentation.runOnMainSync { try { block() } catch (error: Throwable) { failure.set(error) } }
        failure.get()?.let { throw AssertionError("Main-thread test assertion failed", it) }
    }
    private fun log(message: String) = Log.i("AdmobDeviceSmoke", message)
}
