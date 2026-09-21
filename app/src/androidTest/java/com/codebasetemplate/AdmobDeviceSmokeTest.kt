package com.codebasetemplate

import android.graphics.Bitmap
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.codebasetemplate.features.feature_demo_banner_native.ui.NativeGalleryActivity
import com.codebasetemplate.required.ads.AppAdPlaceName
import com.core.ads.BaseAdmobApplication
import com.core.ads.customviews.ads.NativeSmallTemplateView
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
        onMain {
            val place = activity.remoteConfigRepository.getAdPlaceBy(placeName) as NativeAdPlace
            assertEquals(AdType.NativeInterstitial, place.adType)
            assertEquals(NativeTemplateSize.FullInterstitialV3, place.nativeTemplateSize)
            assertEquals(1, place.closeStepCount)
            assertEquals(5, place.countDownTimer)
        }
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

        activity.adsManager.loadFullscreenAd(activity, placeName, "device-action-app-open")
        val loaded = awaitEvent(events, "action_app_open native load") {
            it is AdFullScreenUiResource.AdLoaded || it is AdFullScreenUiResource.AdNotValidOrLoadFailed
        }
        assertTrue("Native fullscreen load: $loaded", loaded is AdFullScreenUiResource.AdLoaded)
        activity.adsManager.showAd(activity, activity.supportFragmentManager, placeName, "device-action-app-open")
        val shown = awaitEvent(events, "action_app_open native show") {
            it is AdFullScreenUiResource.AdSucceedToShow || it is AdFullScreenUiResource.AdCompleted
        }
        assertTrue("Native fullscreen show: $shown", shown is AdFullScreenUiResource.AdSucceedToShow)
        onMain {
            assertTrue(activity.adsManager.isHasFullscreenAdShowing())
            assertTrue(activity.supportFragmentManager.findFragmentByTag(
                "DialogNativeInterstitial_device-action-app-open") is DialogNativeFakeInterstitial)
        }
        Thread.sleep(1_500)
        screenshot("action-app-open-native-interstitial.png")
        assertFalse("Must not complete before the close button is pressed",
            events.any { it is AdFullScreenUiResource.AdCompleted })

        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        var clicked = false
        while (!clicked && System.nanoTime() < deadline) {
            onMain {
                val dialog = activity.supportFragmentManager.findFragmentByTag(
                    "DialogNativeInterstitial_device-action-app-open") as? DialogNativeFakeInterstitial
                val close = dialog?.view?.findViewById<View>(com.core.ads.R.id.tvClose)
                if (close?.isShown == true && close.isEnabled) clicked = close.performClick()
            }
            if (!clicked) Thread.sleep(200)
        }
        assertTrue("Native close button must become available after countdown", clicked)
        val dismissed = awaitEvent(events, "action_app_open native dismiss") {
            it is AdFullScreenUiResource.AdDismissed
        }
        assertTrue(dismissed is AdFullScreenUiResource.AdDismissed)
        val completed = awaitEvent(events, "action_app_open native complete") {
            it is AdFullScreenUiResource.AdCompleted
        } as AdFullScreenUiResource.AdCompleted
        assertTrue(completed.isShown)
        assertFalse(completed.isEarnedReward)
        onMain { assertFalse(activity.adsManager.isHasFullscreenAdShowing()) }
        assertTrue("Fullscreen native must not require banner/native events", nativeEvents.isEmpty())
        log("PASS action_app_open native_interstitial: real load, render, countdown, close and fullscreen callbacks")
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
