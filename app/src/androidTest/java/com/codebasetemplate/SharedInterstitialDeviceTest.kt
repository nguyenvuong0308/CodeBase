package com.codebasetemplate

import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.codebasetemplate.features.feature_demo_banner_native.ui.NativeGalleryActivity
import com.core.ads.BaseAdmobApplication
import com.core.ads.domain.AdFullScreenUiResource
import com.core.ads.model.AdHolder
import com.core.ads.model.PreventShowManyInterstitialAds
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.InterstitialAdPlace
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * Nhiều adPlaceName interstitial dùng chung 1 ad unit id, chạy với Next-Gen SDK thật và Google
 * test ad unit (không mock SDK). Mỗi ad đã load phải có cơ hội được show, nếu không show rate
 * của id dùng chung bị kéo xuống.
 *
 * Trạng thái nội bộ của AdmobManager chỉ được đụng tới bằng reflection ở 3 chỗ không thể tạo ra
 * bằng thiết bị thật trong thời gian test: dọn cache giữa các test, lùi thời điểm load của ad
 * (thay cho chờ 1 giờ) và đưa lượt load về trạng thái "đã timeout" (thay cho mạng chậm hơn 30s).
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SharedInterstitialDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun test01_cappedShowKeepsSharedAdForSiblingPlacement() = withSharedPlacements { activity, events, placeA, placeB ->
        val config = activity.remoteConfigRepository.getInterstitialAdConfig()
        assumeTrue(
            "Capping must be active right after an interstitial is shown",
            config.timeInterval > 0 || config.meaningfulActionsBetweenInterstitial >= 2,
        )
        val a = placeA.placeName
        val b = placeB.placeName

        load(activity, events, a)
        show(activity, a)
        val first = awaitShowResult(events, a)
        assertTrue("First show: $first", first is AdFullScreenUiResource.AdSucceedToShow)
        // Prepare the next shared ad before dismissal starts the capping interval.
        // Network latency must not consume that interval before the capped show attempt.
        load(activity, events, b)
        closeTestAd(events, a)
        onMain {
            assertFalse("Closing the test ad must not finish the host", activity.isFinishing)
            assertFalse("Closing the test ad must not destroy the host", activity.isDestroyed)
        }
        log("PASS $a shown and dismissed; capping is now active")

        events.clear()
        show(activity, a)
        val capped = awaitShowResult(events, a)
        assertTrue("Show right after an interstitial must be capped: $capped",
            capped is AdFullScreenUiResource.AdCompleted && !capped.isShown)
        log("PASS $a blocked by capping while a shared ad is cached")

        // Qua interval rồi gọi show đủ số meaningful action: ad dùng chung phải còn cho placement B.
        Thread.sleep((config.timeInterval + placeB.plusInterval + 1) * 1_000L)
        var shown = false
        var attempts = 0
        while (!shown && attempts < config.meaningfulActionsBetweenInterstitial + 2) {
            attempts++
            events.clear()
            show(activity, b)
            shown = awaitShowResult(events, b) is AdFullScreenUiResource.AdSucceedToShow
        }
        assertTrue("Sibling $b must show the shared ad that the capped $a left in the cache", shown)
        closeTestAd(events, b)
        log("PASS $b showed the shared ad after $attempts show calls")
    }

    @Test
    fun test02_waiterAskingToShowDuringSharedLoadIsShown() = withSharedPlacements(sameWaterfall = true) { activity, events, placeA, placeB ->
        val a = placeA.placeName
        val b = placeB.placeName
        // Cùng 1 lượt main: B khởi tạo load, A thành waiter, rồi A gọi show trước khi ad về.
        onMain {
            activity.adsManager.loadFullscreenAd(activity, b, "device-shared-owner")
            activity.adsManager.loadFullscreenAd(activity, a, "device-shared-waiter")
            activity.adsManager.showAd(activity, activity.supportFragmentManager, a, "device-shared-waiter", true)
        }
        val result = awaitShowResult(events, a)
        assertTrue("Waiter $a must be shown when the shared load finishes: $result",
            result is AdFullScreenUiResource.AdSucceedToShow)
        closeTestAd(events, a)
        log("PASS waiter $a shown from the load started by $b")
    }

    @Test
    fun test03_expiredSharedAdIsReplacedBeforeShow() = withSharedPlacements { activity, events, placeA, placeB ->
        val a = placeA.placeName
        val b = placeB.placeName
        load(activity, events, a)
        lateinit var expiredAd: Any
        onMain {
            val adUnitHolder = adUnitHolders(activity).single { readField(it, "interstitialAd") != null }
            expiredAd = requireNotNull(readField(adUnitHolder, "interstitialAd"))
            // AdMob hết hạn interstitial sau 1 giờ: coi như ad này đã load từ 61 phút trước.
            writeField(adUnitHolder, "interstitialLoadedAtMillis", SystemClock.elapsedRealtime() - TimeUnit.MINUTES.toMillis(61))
        }

        events.clear()
        load(activity, events, b)
        onMain {
            val freshAd = adUnitHolders(activity).mapNotNull { readField(it, "interstitialAd") }.single()
            assertNotSame("Expired ad must be replaced by a new request", expiredAd, freshAd)
        }
        show(activity, b)
        val result = awaitShowResult(events, b)
        assertTrue("Fresh ad must show: $result", result is AdFullScreenUiResource.AdSucceedToShow)
        closeTestAd(events, b)
        log("PASS expired shared ad replaced and fresh ad shown for $b")
    }

    @Test
    fun test04_adDeliveredAfterLoadTimeoutIsKeptAndShown() = withSharedPlacements { activity, events, placeA, _ ->
        assumeTrue("Needs a single-tier waterfall", placeA.waterfall().size == 1)
        val a = placeA.placeName
        onMain {
            activity.adsManager.loadFullscreenAd(activity, a, "device-shared-late")
            // Đúng trạng thái sau FULL_SCREEN_LOAD_TIMEOUT_MS: hết loading, waiter đã bị nhả,
            // còn request thật tới SDK vẫn đang chạy và sẽ trả ad về "muộn".
            val adUnitHolder = adUnitHolders(activity).single { readField(it, "isLoading") == true }
            writeField(adUnitHolder, "isLoading", false)
            activity.adsManager.javaClass.declaredMethods.single { it.name == "resumeSiblingWaitersOnFail" }
                .apply { isAccessible = true }
                .invoke(activity.adsManager, adUnitHolder, null, placeA.adId)
        }
        awaitEvent(events, "$a timed out") { it is AdFullScreenUiResource.AdNotValidOrLoadFailed && it.rootAdPlaceName == a }
        awaitEvent(events, "$a late ad kept") { it is AdFullScreenUiResource.AdLoaded && it.rootAdPlaceName == a }
        show(activity, a)
        val result = awaitShowResult(events, a)
        assertTrue("Late ad must still be shown: $result", result is AdFullScreenUiResource.AdSucceedToShow)
        closeTestAd(events, a)
        log("PASS ad delivered after timeout kept and shown for $a")
    }

    private fun withSharedPlacements(
        sameWaterfall: Boolean = false,
        block: (NativeGalleryActivity, LinkedBlockingQueue<AdFullScreenUiResource>, InterstitialAdPlace, InterstitialAdPlace) -> Unit,
    ) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        ActivityScenario.launch(NativeGalleryActivity::class.java).use { scenario ->
            lateinit var activity: NativeGalleryActivity
            scenario.onActivity { activity = it }
            try {
                initializeSdk()
                val (placeA, placeB) = sharedInterstitialPlacements(activity, sameWaterfall)
                val events = LinkedBlockingQueue<AdFullScreenUiResource>()
                awaitNoFullScreenLoadInFlight(activity)
                onMain {
                    resetFullScreenState(activity)
                    scope.launch { activity.adsManager.adFullScreenFlow.collect { events.offer(it) } }
                }
                log("Shared unit ${placeA.adId}: ${placeA.placeName.name} + ${placeB.placeName.name}")
                block(activity, events, placeA, placeB)
            } finally {
                scope.cancel()
            }
        }
    }

    /**
     * Two enabled interstitial placements on the same Google test ad unit, simplest config first.
     * [sameWaterfall] additionally requires identical waterfalls, so the second placement becomes
     * a waiter of the load started by the first one.
     */
    private fun sharedInterstitialPlacements(
        activity: NativeGalleryActivity,
        sameWaterfall: Boolean,
    ): Pair<InterstitialAdPlace, InterstitialAdPlace> {
        var pair: List<InterstitialAdPlace> = emptyList()
        onMain {
            pair = activity.remoteConfigRepository.getAdPlaces()
                .filterIsInstance<InterstitialAdPlace>()
                .filter {
                    // Placement không khai báo trong AppAdPlaceName/CoreAdPlaceName có tên rỗng.
                    it.placeName.name.isNotBlank() && it.isInterstitialType() && !it.isIgnoreInterval && !it.isTutorialFlow &&
                        !it.isShowNativeAfter && it.nativeAfterInterstitial == null &&
                        it.waterfall().all { id -> id.startsWith(GOOGLE_TEST_UNIT_PREFIX) } &&
                        !activity.adsManager.isNotAbleToVisibleAdsToUser(it.placeName)
                }
                .distinctBy { it.placeName.name }
                .groupBy { if (sameWaterfall) it.waterfall() else listOf(it.adId) }.values
                .filter { it.size >= 2 }
                .map { group ->
                    group.sortedWith(compareBy({ it.highFloorAdIds.size }, { it.isAutoLoadAfterDismiss }, { it.plusInterval }))
                }
                .minByOrNull { it[0].highFloorAdIds.size + it[1].highFloorAdIds.size }
                .orEmpty()
        }
        assumeTrue("Configure at least two interstitial placements sharing one Google test ad unit", pair.size >= 2)
        return pair[0] to pair[1]
    }

    /**
     * A previous test can leave a load running (e.g. is_auto_load_after_dismiss). Its callback must
     * land before the cache is cleared, otherwise it would report into the next test's placements.
     */
    private fun awaitNoFullScreenLoadInFlight(activity: NativeGalleryActivity) {
        val deadline = SystemClock.elapsedRealtime() + 60_000
        var idleChecks = 0
        while (idleChecks < 3 && SystemClock.elapsedRealtime() < deadline) {
            var loading = false
            onMain {
                @Suppress("UNCHECKED_CAST")
                val holders = readField(activity.adsManager, "adHolderFullScreenMap") as Map<IAdPlaceName, AdHolder>
                loading = holders.values.any { it.isLoading } ||
                    adUnitHolders(activity).any { readField(it, "isLoading") == true }
            }
            idleChecks = if (loading) 0 else idleChecks + 1
            Thread.sleep(500)
        }
        assertTrue("Full-screen loads from the previous test must settle", idleChecks >= 3)
    }

    /** Each test starts with no cached full-screen ad and no capping carried over. */
    private fun resetFullScreenState(activity: NativeGalleryActivity) {
        val manager = activity.adsManager
        @Suppress("UNCHECKED_CAST")
        (readField(manager, "adHolderFullScreenMap") as Map<IAdPlaceName, AdHolder>).values.forEach { it.reset() }
        (readField(manager, "fullScreenAdUnitHolderMap") as MutableMap<*, *>).clear()
        PreventShowManyInterstitialAds.initIntervalTimeShowInterstitialMillis()
    }

    // Non-tutorial placements (the only ones selected here) load high floors first, then adId.
    private fun InterstitialAdPlace.waterfall(): List<String> = highFloorAdIds + adId

    private fun adUnitHolders(activity: NativeGalleryActivity): List<Any> =
        (readField(activity.adsManager, "fullScreenAdUnitHolderMap") as Map<*, *>).values.filterNotNull()

    private fun readField(target: Any, name: String): Any? =
        target.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(target)

    private fun writeField(target: Any, name: String, value: Any) =
        target.javaClass.getDeclaredField(name).apply { isAccessible = true }.set(target, value)

    private fun load(
        activity: NativeGalleryActivity,
        events: LinkedBlockingQueue<AdFullScreenUiResource>,
        placeName: IAdPlaceName,
    ) {
        activity.adsManager.loadFullscreenAd(activity, placeName, "device-shared")
        val loaded = awaitEvent(events, "${placeName.name} load") {
            it.rootAdPlaceName == placeName &&
                (it is AdFullScreenUiResource.AdLoaded || it is AdFullScreenUiResource.AdNotValidOrLoadFailed)
        }
        assertTrue("Interstitial load ${placeName.name}: $loaded", loaded is AdFullScreenUiResource.AdLoaded)
    }

    private fun show(activity: NativeGalleryActivity, placeName: IAdPlaceName) {
        onMain { assertFalse("No full-screen ad may be showing before show", activity.adsManager.isHasFullscreenAdShowing()) }
        activity.adsManager.showAd(activity, activity.supportFragmentManager, placeName, "device-shared")
    }

    /** First AdSucceedToShow or AdCompleted of [placeName]. */
    private fun awaitShowResult(
        events: LinkedBlockingQueue<AdFullScreenUiResource>,
        placeName: IAdPlaceName,
    ): AdFullScreenUiResource = awaitEvent(events, "${placeName.name} show") {
        it.rootAdPlaceName == placeName &&
            (it is AdFullScreenUiResource.AdSucceedToShow || it is AdFullScreenUiResource.AdCompleted)
    }

    private fun closeTestAd(events: LinkedBlockingQueue<AdFullScreenUiResource>, placeName: IAdPlaceName) {
        val start = SystemClock.elapsedRealtime()
        var lastBack = start
        while (SystemClock.elapsedRealtime() - start < 60_000) {
            val event = events.poll(500, TimeUnit.MILLISECONDS)
            if (event is AdFullScreenUiResource.AdCompleted && event.rootAdPlaceName == placeName) {
                assertTrue("Dismissed interstitial must complete as shown", event.isShown)
                instrumentation.waitForIdleSync()
                return
            }
            findClose(instrumentation.uiAutomation.rootInActiveWindow)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            val now = SystemClock.elapsedRealtime()
            if (now - start > 8_000 && now - lastBack > 4_000) {
                // A global Back can reach the host after the close click dismisses the ad.
                // Deliver the fallback only to a still-resumed SDK ad activity.
                onMain {
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                        .filter { it.javaClass.name == "com.google.android.libraries.ads.mobile.sdk.common.AdActivity" }
                        .forEach { adActivity ->
                            @Suppress("DEPRECATION")
                            adActivity.onBackPressed()
                        }
                }
                lastBack = now
            }
        }
        fail("Interstitial ${placeName.name} did not dismiss within 60 seconds")
    }

    private fun findClose(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        val labels = setOf("close", "close ad", "dismiss ad", "đóng", "đóng quảng cáo")
        if (node.isClickable && (node.contentDescription?.toString()?.lowercase() in labels || node.text?.toString()?.lowercase() in labels)) return node
        for (i in 0 until node.childCount) findClose(node.getChild(i))?.let { return it }
        return null
    }

    private fun initializeSdk() {
        if (MobileAds.isInitialized) return
        val app = instrumentation.targetContext.applicationContext as BaseAdmobApplication
        val config = InitializationConfig.Builder("ca-app-pub-3940256099942544~3347511713")
            .setRequestConfiguration(app.createAdsRequestConfiguration()).build()
        assertNotSame(Looper.getMainLooper(), Looper.myLooper())
        MobileAds.initialize(app, config)
        assertTrue(MobileAds.isInitialized)
    }

    private fun <T : Any> awaitEvent(queue: LinkedBlockingQueue<T>, label: String, accept: (T) -> Boolean): T {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(55)
        while (System.nanoTime() < deadline) {
            val event = queue.poll(1, TimeUnit.SECONDS) ?: continue
            log("$label: $event")
            if (accept(event)) return event
        }
        throw AssertionError("Timed out waiting for $label")
    }

    private fun onMain(block: () -> Unit) {
        val failure = AtomicReference<Throwable>()
        instrumentation.runOnMainSync { try { block() } catch (error: Throwable) { failure.set(error) } }
        failure.get()?.let {
            if (it is org.junit.AssumptionViolatedException) throw it
            throw AssertionError("Main-thread test assertion failed", it)
        }
    }

    private fun log(message: String) = Log.i("SharedInterstitialDevice", message)

    private companion object {
        const val GOOGLE_TEST_UNIT_PREFIX = "ca-app-pub-3940256099942544/"
    }
}
