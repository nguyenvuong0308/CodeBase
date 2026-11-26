package com.core.ads.admob

import android.app.Activity
import android.app.Application
import android.os.Looper
import com.core.ads.AdsSdkInitializer
import com.core.ads.domain.AdFullScreenUiResource
import com.core.ads.model.AdHolder
import com.core.ads.model.InterstitialAdHolder
import com.core.ads.model.PreventShowManyInterstitialAds
import com.core.analytics.AdjustAnalytics
import com.core.analytics.AnalyticsManager
import com.core.config.domain.RemoteConfigRepository
import com.core.config.domain.data.AdType
import com.core.config.domain.data.AppConfig
import com.core.config.domain.data.CoreAdPlaceName
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.InterstitialAdPlace
import com.core.config.domain.data.InterstitialAdTypeConfig
import com.core.config.domain.data.NativeAfterInterstitialLoadStrategy
import com.core.preference.AppPreferences
import com.core.preference.PurchasePreferences
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Nhiều adPlaceName dùng chung 1 ad unit id: mỗi ad đã load (matched request) phải có cơ hội
 * được show, nếu không show rate của id đó bị kéo xuống.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [28], application = Application::class,
    shadows = [ShadowAdsReadiness::class, ShadowInterstitialLoader::class],
)
@LooperMode(LooperMode.Mode.PAUSED)
class SharedInterstitialAdUnitTest {
    private val nameA = CoreAdPlaceName.ACTION_OPEN_APP
    private val nameB = CoreAdPlaceName.ACTION_OPEN_APP_FIRST_OPEN
    private lateinit var manager: AdmobManager
    private lateinit var activity: Activity
    private lateinit var holderA: InterstitialAdHolder
    private lateinit var holderB: InterstitialAdHolder
    private val scope = CoroutineScope(Dispatchers.Main)
    private val events = mutableListOf<AdFullScreenUiResource>()

    @Before
    fun setUp() {
        ShadowAdsReadiness.ready = true
        ShadowInterstitialLoader.callbacks.clear()
        PreventShowManyInterstitialAds.initIntervalTimeShowInterstitialMillis()
        val config = mock(RemoteConfigRepository::class.java)
        val app = mock(AppConfig::class.java)
        `when`(app.isEnableAds).thenReturn(true)
        `when`(config.getAppConfig()).thenReturn(app)
        `when`(config.getAdsDisableByCountry()).thenReturn(emptyList())
        `when`(config.getInterstitialAdConfig()).thenReturn(
            InterstitialAdTypeConfig(
                isWaitLoadToShow = false,
                adsPerSession = 100,
                timePerSession = 3600,
                timeInterval = 0,
                timeIntervalAfterShowOpenAd = 0,
                meaningfulActionsBetweenInterstitial = MEANINGFUL_ACTIONS,
                isEnableRetry = false,
                maxRetryCount = 0,
                retryIntervalSecondList = emptyList(),
            )
        )
        manager = AdmobManager(
            RuntimeEnvironment.getApplication(), config,
            mock(PurchasePreferences::class.java), mock(AnalyticsManager::class.java),
            mock(AppPreferences::class.java), mock(AdsSdkInitializer::class.java),
            mock(AdjustAnalytics::class.java)
        )
        manager.canRequestAdsOverride = true
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        holderA = InterstitialAdHolder(adPlace = place(nameA))
        holderB = InterstitialAdHolder(adPlace = place(nameB))
        fullScreenHolders()[nameA] = holderA
        fullScreenHolders()[nameB] = holderB
        scope.launch(start = CoroutineStart.UNDISPATCHED) { manager.adFullScreenFlow.collect { events += it } }
    }

    @After
    fun tearDown() {
        scope.cancel()
        AdmobManager::class.java.getDeclaredField("applicationScope").let {
            it.isAccessible = true
            (it.get(manager) as CoroutineScope).cancel()
        }
        PreventShowManyInterstitialAds.initIntervalTimeShowInterstitialMillis()
        activity.finish()
    }

    @Test
    fun `show blocked by capping leaves the ad in the shared cache for a sibling placement`() {
        val ad = loadSharedAd()
        blockByCapping()

        show(holderA)

        verify(ad, never()).show(activity)
        assertNull("Capped placement must not take the shared ad", holderA.interstitialAd)
        assertFalse(holderA.isShowing)
        assertEquals(1, events.filterIsInstance<AdFullScreenUiResource.AdCompleted>().size)

        unblockCapping()
        show(holderB)

        verify(ad).show(activity)
        assertEquals("Sibling must reuse the cached ad", 1, ShadowInterstitialLoader.callbacks.size)
    }

    @Test
    fun `ad delivered after the load timeout is kept and shown later`() {
        load(holderA)
        val callback = ShadowInterstitialLoader.callbacks.single()
        shadowOf(Looper.getMainLooper()).idleFor(30, TimeUnit.SECONDS)
        assertFalse(holderA.isLoading)
        events.clear()

        val lateAd = mock(InterstitialAd::class.java)
        callback.onAdLoaded(lateAd)
        idle()

        assertEquals(1, events.filterIsInstance<AdFullScreenUiResource.AdLoaded>().size)
        show(holderB)
        verify(lateAd).show(activity)
        assertEquals(1, ShadowInterstitialLoader.callbacks.size)
    }

    @Test
    fun `cached ad is reused before it expires and replaced after`() {
        val ad = loadSharedAd()

        shadowOf(Looper.getMainLooper()).idleFor(INTERSTITIAL_AD_TTL_MS - 1_000L, TimeUnit.MILLISECONDS)
        load(holderB)
        assertEquals("Fresh ad must not trigger a new request", 1, ShadowInterstitialLoader.callbacks.size)

        shadowOf(Looper.getMainLooper()).idleFor(1_000L, TimeUnit.MILLISECONDS)
        show(holderA)
        verify(ad, never()).show(activity)
        assertNull(holderA.interstitialAd)

        load(holderB)
        assertEquals("Expired ad must be replaced", 2, ShadowInterstitialLoader.callbacks.size)
    }

    @Test
    fun `sibling waiter that asks to show during the shared load is shown when it finishes`() {
        load(holderB)
        load(holderA)
        assertEquals(1, ShadowInterstitialLoader.callbacks.size)
        assertTrue(holderA.isLoading)

        // showAd(A, isWaitLoadToShow = true) khi ad chưa về.
        holderA.isWaitLoadToShow = true
        AdmobManager::class.java.getDeclaredMethod(
            "handleWhenRequestShowAdIfAdUnavailable", Activity::class.java, AdHolder::class.java
        ).apply { isAccessible = true }.invoke(manager, activity, holderA)

        val ad = mock(InterstitialAd::class.java)
        ShadowInterstitialLoader.callbacks.single().onAdLoaded(ad)
        idle()

        verify(ad).show(activity)
        assertTrue(holderA.isShowing)
        assertFalse(holderA.isWaitLoadToShow)
    }

    @Test
    fun `late high floor ad satisfies waiting show when final fallback fails`() {
        val lateAd = prepareLateHighFloorAd()

        failFallback()
        // The fallback's scheduled timeout must not complete the recovered show again.
        shadowOf(Looper.getMainLooper()).idleFor(30, TimeUnit.SECONDS)

        assertRecoveredWaitingShow(lateAd)
    }

    @Test
    fun `late high floor ad satisfies waiting show when final fallback times out`() {
        val lateAd = prepareLateHighFloorAd()

        shadowOf(Looper.getMainLooper()).idleFor(30, TimeUnit.SECONDS)
        // An SDK failure arriving after timeout must not fail the recovered show.
        failFallback()

        assertRecoveredWaitingShow(lateAd)
    }

    @Test
    fun `preload keeps late high floor ad without reporting fallback failure or showing it`() {
        val lateAd = prepareLateHighFloorAd(waitToShow = false)

        failFallback()

        assertFalse(holderA.isLoading)
        assertFalse(holderA.isShowing)
        verify(lateAd, never()).show(activity)
        assertEquals(1, events.filterIsInstance<AdFullScreenUiResource.AdLoaded>().size)
        assertTrue(events.filterIsInstance<AdFullScreenUiResource.AdNotValidOrLoadFailed>().isEmpty())
        assertTrue(events.filterIsInstance<AdFullScreenUiResource.AdCompleted>().isEmpty())
        show(holderA)
        verify(lateAd).show(activity)
        assertEquals(2, ShadowInterstitialLoader.callbacks.size)
    }

    @Test
    fun `shared waiters show late ad only once when final fallback fails`() {
        val lateAd = prepareLateHighFloorAd(sharedWaiter = true)

        failFallback()

        assertOneSharedWaiterRecovered(lateAd)
        assertTrue("Sibling must recover from the owner's failed request", holderB.isShowing)
    }

    @Test
    fun `shared waiters show late ad only once when final fallback times out`() {
        val lateAd = prepareLateHighFloorAd(sharedWaiter = true)

        shadowOf(Looper.getMainLooper()).idleFor(30, TimeUnit.SECONDS)

        assertOneSharedWaiterRecovered(lateAd)
    }

    @Test
    fun `final fallback still completes as failed when no late ad is cached`() {
        holderA.adPlace = place(nameA).copy(highFloorAdIds = listOf("high-unit"))
        holderA.isWaitLoadToShow = true
        load(holderA)
        shadowOf(Looper.getMainLooper()).idleFor(30, TimeUnit.SECONDS)
        events.clear()

        failFallback()

        assertFalse(holderA.isLoading)
        assertFalse(holderA.isWaitLoadToShow)
        assertFalse(holderA.isShowing)
        assertEquals(1, events.filterIsInstance<AdFullScreenUiResource.AdNotValidOrLoadFailed>().size)
        val completed = events.filterIsInstance<AdFullScreenUiResource.AdCompleted>().single()
        assertFalse(completed.isShown)
        assertEquals(2, ShadowInterstitialLoader.callbacks.size)
    }

    @Test
    fun `distinct late ads do not overlap when their shared fallback fails`() {
        assertDistinctLateAdsDoNotOverlap(fallbackTimesOut = false)
    }

    @Test
    fun `distinct late ads do not overlap when their shared fallback times out`() {
        assertDistinctLateAdsDoNotOverlap(fallbackTimesOut = true)
    }

    private fun assertDistinctLateAdsDoNotOverlap(fallbackTimesOut: Boolean) {
        holderA.adPlace = place(nameA).copy(highFloorAdIds = listOf("high-a"))
        holderB.adPlace = place(nameB).copy(highFloorAdIds = listOf("high-b"))
        holderA.isWaitLoadToShow = true
        holderB.isWaitLoadToShow = true
        load(holderA)
        load(holderB)
        val highCallbacks = ShadowInterstitialLoader.callbacks.toList()
        shadowOf(Looper.getMainLooper()).idleFor(30, TimeUnit.SECONDS)
        assertEquals(3, ShadowInterstitialLoader.callbacks.size)
        val adA = mock(InterstitialAd::class.java)
        val adB = mock(InterstitialAd::class.java)
        highCallbacks[0].onAdLoaded(adA)
        highCallbacks[1].onAdLoaded(adB)
        idle()
        events.clear()

        if (fallbackTimesOut) {
            shadowOf(Looper.getMainLooper()).idleFor(30, TimeUnit.SECONDS)
        } else {
            failFallback()
        }

        assertEquals(1, listOf(holderA, holderB).count { it.isShowing })
        assertFalse(holderA.isLoading || holderB.isLoading)
        assertFalse(holderA.isWaitLoadToShow || holderB.isWaitLoadToShow)
        val showingAd = if (holderA.isShowing) adA else adB
        val waitingAd = if (holderA.isShowing) adB else adA
        val waitingHolder = if (holderA.isShowing) holderB else holderA
        verify(showingAd).show(activity)
        verify(waitingAd, never()).show(activity)
        val completed = events.filterIsInstance<AdFullScreenUiResource.AdCompleted>().single()
        assertEquals(waitingHolder.adPlace.placeName, completed.rootAdPlaceName)
        assertFalse(completed.isShown)
        assertTrue(events.filterIsInstance<AdFullScreenUiResource.AdNotValidOrLoadFailed>().isEmpty())

        val callback = ArgumentCaptor.forClass(InterstitialAdEventCallback::class.java)
        verify(showingAd).adEventCallback = callback.capture()
        callback.value.onAdDismissedFullScreenContent()
        idle()
        unblockCapping()
        show(waitingHolder)
        verify(waitingAd).show(activity)
        assertEquals("The second ad must stay cached for the next show", 3, ShadowInterstitialLoader.callbacks.size)
    }

    private fun prepareLateHighFloorAd(
        waitToShow: Boolean = true,
        sharedWaiter: Boolean = false,
    ): InterstitialAd {
        holderA.adPlace = place(nameA).copy(highFloorAdIds = listOf("high-unit"))
        holderA.isWaitLoadToShow = waitToShow
        load(holderA)
        if (sharedWaiter) {
            holderB.adPlace = place(nameB).copy(highFloorAdIds = listOf("high-unit"))
            holderB.isWaitLoadToShow = waitToShow
            load(holderB)
        }
        val highCallback = ShadowInterstitialLoader.callbacks.single()
        shadowOf(Looper.getMainLooper()).idleFor(30, TimeUnit.SECONDS)
        assertEquals("High tier timeout starts one shared fallback", 2, ShadowInterstitialLoader.callbacks.size)
        val lateAd = mock(InterstitialAd::class.java)
        highCallback.onAdLoaded(lateAd)
        idle()
        assertTrue("Fallback is still loading when the high tier arrives", holderA.isLoading)
        events.clear()
        return lateAd
    }

    private fun failFallback() {
        ShadowInterstitialLoader.callbacks.last().onAdFailedToLoad(mock(LoadAdError::class.java))
        idle()
    }

    private fun assertRecoveredWaitingShow(ad: InterstitialAd) {
        verify(ad).show(activity)
        assertTrue(holderA.isShowing)
        assertFalse(holderA.isLoading)
        assertFalse(holderA.isWaitLoadToShow)
        assertTrue(events.filterIsInstance<AdFullScreenUiResource.AdNotValidOrLoadFailed>().isEmpty())
        assertTrue(events.filterIsInstance<AdFullScreenUiResource.AdCompleted>().isEmpty())
        assertEquals(2, ShadowInterstitialLoader.callbacks.size)
    }

    private fun assertOneSharedWaiterRecovered(ad: InterstitialAd) {
        verify(ad).show(activity)
        assertEquals(1, listOf(holderA, holderB).count { it.isShowing })
        assertFalse(holderA.isLoading || holderB.isLoading)
        assertFalse(holderA.isWaitLoadToShow || holderB.isWaitLoadToShow)
        val completed = events.filterIsInstance<AdFullScreenUiResource.AdCompleted>().single()
        assertFalse(completed.isShown)
        val showingPlace = if (holderA.isShowing) nameA else nameB
        assertTrue(completed.rootAdPlaceName != showingPlace)
        assertTrue(events.filterIsInstance<AdFullScreenUiResource.AdNotValidOrLoadFailed>()
            .none { it.rootAdPlaceName == showingPlace })
        assertEquals(2, ShadowInterstitialLoader.callbacks.size)
    }

    private fun loadSharedAd(): InterstitialAd {
        load(holderA)
        val ad = mock(InterstitialAd::class.java)
        ShadowInterstitialLoader.callbacks.single().onAdLoaded(ad)
        idle()
        return ad
    }

    private fun load(holder: InterstitialAdHolder) {
        AdmobManager::class.java.getDeclaredMethod(
            "loadInterstitialIfNeed", Activity::class.java, InterstitialAdHolder::class.java,
            Int::class.javaPrimitiveType
        ).apply { isAccessible = true }.invoke(manager, activity, holder, 0)
        idle()
    }

    private fun show(holder: InterstitialAdHolder) {
        AdmobManager::class.java.getDeclaredMethod(
            "showInterstitial", Activity::class.java, InterstitialAdHolder::class.java
        ).apply { isAccessible = true }.invoke(manager, activity, holder)
        idle()
    }

    private fun blockByCapping() = PreventShowManyInterstitialAds.resetMeaningfulActionsAfterInterstitial()

    private fun unblockCapping() = repeat(MEANINGFUL_ACTIONS) {
        PreventShowManyInterstitialAds.recordMeaningfulAction()
    }

    private fun place(name: IAdPlaceName) = InterstitialAdPlace(
        isTrackingShow = false,
        isTrackingClick = false,
        placeName = name,
        adId = SHARED_UNIT,
        highFloorAdIds = emptyList(),
        isEnable = true,
        adType = AdType.Interstitial,
        isAutoLoadAfterDismiss = false,
        isIgnoreInterval = false,
        isTutorialFlow = false,
        plusInterval = 0,
        isShowNativeAfter = false,
        nativeAfterLoadStrategy = NativeAfterInterstitialLoadStrategy.WithInterstitial,
        nativeAfterInterstitial = null,
    )

    @Suppress("UNCHECKED_CAST")
    private fun fullScreenHolders(): MutableMap<IAdPlaceName, AdHolder> =
        AdmobManager::class.java.getDeclaredField("adHolderFullScreenMap").let {
            it.isAccessible = true
            it.get(manager) as MutableMap<IAdPlaceName, AdHolder>
        }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private companion object {
        const val SHARED_UNIT = "shared-unit"
        const val MEANINGFUL_ACTIONS = 2
    }
}
