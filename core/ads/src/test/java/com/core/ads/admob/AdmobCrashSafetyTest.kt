package com.core.ads.admob

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Looper
import android.os.SystemClock
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.core.ads.AdsSdkInitializer
import com.core.ads.domain.AdsManager
import com.core.ads.domain.ConsentFormUiResource
import com.core.ads.model.AdHolder
import com.core.ads.model.BannerAdHolder
import com.core.ads.model.NativeAdHolder
import com.core.ads.model.InterstitialAdHolder
import com.core.ads.model.RewardedAdHolder
import com.core.analytics.AdjustAnalytics
import com.core.analytics.AnalyticsManager
import com.core.config.domain.RemoteConfigRepository
import com.core.config.domain.data.*
import com.core.preference.AppPreferences
import com.core.preference.PurchasePreferences
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.initialization.OnAdapterInitializationCompleteListener
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import java.util.Optional
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [28], application = Application::class,
    shadows = [ShadowAdsReadiness::class, ShadowNativeLoader::class, ShadowInterstitialLoader::class],
)
@LooperMode(LooperMode.Mode.PAUSED)
class AdmobCrashSafetyTest {
    private lateinit var config: RemoteConfigRepository
    private lateinit var preferences: AppPreferences
    private lateinit var manager: AdmobManager
    private lateinit var activity: Activity
    private lateinit var initializer: AdsSdkInitializer
    private val placeName = CoreAdPlaceName.NONE

    @Before
    fun setUp() {
        ShadowAdsReadiness.ready = false
        ShadowAdsReadiness.failInitialization = false
        ShadowAdsReadiness.initializationEntered = CountDownLatch(1)
        ShadowNativeLoader.callbacks.clear()
        ShadowInterstitialLoader.callbacks.clear()
        config = mock(RemoteConfigRepository::class.java)
        preferences = mock(AppPreferences::class.java)
        initializer = mock(AdsSdkInitializer::class.java)
        `when`(initializer.admobAppId()).thenReturn("ca-app-pub-3940256099942544~3347511713")
        manager = AdmobManager(
            RuntimeEnvironment.getApplication(), config,
            mock(PurchasePreferences::class.java), mock(AnalyticsManager::class.java),
            preferences, initializer, mock(AdjustAnalytics::class.java),
        )
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    @After
    fun tearDown() {
        AdmobManager::class.java.getDeclaredField("applicationScope").let {
            it.isAccessible = true
            (it.get(manager) as CoroutineScope).cancel()
        }
    }

    @Test
    fun `SDK initialization exception completes consent without marking SDK ready`() {
        ShadowAdsReadiness.failInitialization = true
        val events = mutableListOf<ConsentFormUiResource>()
        val collector = CoroutineScope(Dispatchers.Main).launch(start = CoroutineStart.UNDISPATCHED) {
            manager.requestConsentFlow.collect { events += it }
        }
        try {
            AdmobManager::class.java.getDeclaredField("consentFlowStartElapsedRealtime").let {
                it.isAccessible = true
                it.setLong(manager, SystemClock.elapsedRealtime())
            }
            AdmobManager::class.java.getDeclaredMethod("onEuConsentComplete").let {
                it.isAccessible = true
                it.invoke(manager)
            }
            assertTrue(ShadowAdsReadiness.initializationEntered.await(5, TimeUnit.SECONDS))
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (events.isEmpty() && System.nanoTime() < deadline) {
                shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(10)
            }
            assertEquals(listOf(ConsentFormUiResource.Complete), events)
            assertFalse(ShadowAdsReadiness.ready)
            verify(initializer, never()).onAdInitCompleted()
            manager.loadBannerNativeAd(activity, placeName, "after failure", false, false)
            verifyNoInteractions(config)
        } finally {
            collector.cancel()
        }
    }

    @Test
    fun `timeout can continue splash without allowing an uninitialized ad request`() {
        manager.loadFullscreenAd(activity, placeName, "test")
        manager.loadBannerNativeAd(activity, placeName, "test", false, false)

        // Fail before config, holder creation or any SDK load (which would throw before init).
        verifyNoInteractions(config)
        assertTrue(holders().isEmpty())
        assertTrue(ShadowNativeLoader.callbacks.isEmpty())
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `app open also skips load and show before SDK initialization`() {
        val adsManager = mock(AdsManager::class.java)
        val appOpen = AppOpenAdManager(
            RuntimeEnvironment.getApplication(), config, adsManager,
            ReOpenShowCondition(), mock(AdjustAnalytics::class.java), Optional.empty(),
        )
        appOpen.fetchAd(activity, placeName)
        appOpen.showAdIfAvailable(activity, placeName)

        verifyNoInteractions(config, adsManager)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `release from worker destroys banner only on Main and invalidates pending load`() {
        val banner = mock(AdView::class.java)
        val holder = BannerAdHolder(NoneAdPlace(), bannerAd = banner)
        val generation = holder.beginLoad()
        holders()[placeName] = holder
        val destroyedOn = AtomicReference<Looper>()
        doAnswer { destroyedOn.set(Looper.myLooper()); null }.`when`(banner).destroy()

        onWorker { manager.releaseBannerNative(placeName) }
        verify(banner, never()).destroy()
        shadowOf(Looper.getMainLooper()).idle()

        assertSame(Looper.getMainLooper(), destroyedOn.get())
        assertFalse(holder.acceptsLoadCallback(generation))
        assertFalse(holder.needRetry)
        assertNull(holder.bannerAd)
    }

    @Test
    fun `release on Main completes immediately without waiting for another loop turn`() {
        val banner = mock(AdView::class.java)
        val holder = BannerAdHolder(NoneAdPlace(), bannerAd = banner)
        val generation = holder.beginLoad()
        holders()[placeName] = holder

        manager.releaseBannerNative(placeName)

        // No idle(): callers already on Main must observe the reset before returning.
        verify(banner).destroy()
        assertNull(holder.bannerAd)
        assertFalse(holder.acceptsLoadCallback(generation))
        assertFalse(holder.needRetry)
    }

    @Test
    fun `queued worker request rechecks Activity before loading on Main`() {
        ShadowAdsReadiness.ready = true

        onWorker { manager.loadBannerNativeAd(activity, placeName, "queued", false, false) }
        verifyNoInteractions(config)
        activity.finish()
        shadowOf(Looper.getMainLooper()).idle()

        verifyNoInteractions(config)
        assertTrue(holders().isEmpty())
        assertTrue(ShadowNativeLoader.callbacks.isEmpty())
    }

    @Test
    fun `timer requested from worker does not read preferences until Main runs`() {
        onWorker { manager.startDisableAdCountDownTimer() }
        verifyNoInteractions(preferences)
        shadowOf(Looper.getMainLooper()).idle()
        verify(preferences).timeOfFirstAdClicked
    }

    @Test
    fun `banner reset destroys both loaded and in flight resources even if detach fails`() {
        val loaded = mock(AdView::class.java)
        val pending = mock(AdView::class.java)
        val parent = mock(ViewGroup::class.java)
        `when`(loaded.parent).thenReturn(parent)
        doThrow(IllegalStateException("detached parent")).`when`(parent).removeView(loaded)
        val holder = BannerAdHolder(NoneAdPlace(), loaded)
        holder.loadingBannerAd = pending
        val generation = holder.beginLoad()

        holder.reset()
        holder.reset()

        verify(loaded, times(1)).destroy()
        verify(pending, times(1)).destroy()
        assertFalse(holder.acceptsLoadCallback(generation))
        assertNull(holder.loadingBannerAd)
        assertNull(holder.bannerAd)
    }

    @Test
    fun `native callback queued before release is destroyed rather than cached`() {
        prepareNative()
        manager.loadBannerNativeAd(activity, placeName, "test", false, false)
        val holder = holders()[placeName] as NativeAdHolder
        val callback = ShadowNativeLoader.callbacks.single()
        val ad = mock(NativeAd::class.java)

        onWorker { callback.onNativeAdLoaded(ad) }
        manager.releaseBannerNative(placeName)
        shadowOf(Looper.getMainLooper()).idle()

        verify(ad).destroy()
        assertNull(holder.nativeAd)
        assertFalse(holder.isLoading)
    }

    @Test
    fun `old native failure cannot clear a newer successful load`() {
        prepareNative()
        manager.loadBannerNativeAd(activity, placeName, "old", false, false)
        val old = ShadowNativeLoader.callbacks.single()
        manager.releaseBannerNative(placeName)
        manager.loadBannerNativeAd(activity, placeName, "new", false, false)
        val newAd = mock(NativeAd::class.java)
        ShadowNativeLoader.callbacks.last().onNativeAdLoaded(newAd)

        old.onAdFailedToLoad(mock(LoadAdError::class.java))
        shadowOf(Looper.getMainLooper()).idle()

        assertSame(newAd, (holders()[placeName] as NativeAdHolder).nativeAd)
        verify(newAd, never()).destroy()
    }

    @Test
    fun `native arriving after Activity destruction is disposed`() {
        prepareNative()
        manager.loadBannerNativeAd(activity, placeName, "test", false, false)
        activity.finish()
        val ad = mock(NativeAd::class.java)

        ShadowNativeLoader.callbacks.single().onNativeAdLoaded(ad)

        verify(ad).destroy()
        assertNull((holders()[placeName] as NativeAdHolder).nativeAd)
    }

    @Test
    fun `banner waterfall destroys old request and ignores its late success`() {
        prepareAds()
        val place = BannerAdPlace(false, false, BannerSize.StandardMedium, false, false,
            placeName, "fallback", listOf("high"), true, AdType.Banner, false, false, false)
        `when`(config.getAdPlaceBy(placeName)).thenReturn(place)
        val callbacks = mutableListOf<AdLoadCallback<BannerAd>>()
        mockConstruction(AdView::class.java) { view, _ ->
            doAnswer { invocation ->
                callbacks += invocation.getArgument<AdLoadCallback<BannerAd>>(1)
                null
            }.`when`(view).loadAd(anyArg(), anyArg())
        }.use { views ->
            manager.loadBannerNativeAd(activity, placeName, "test", false, false)
            val oldView = views.constructed().single()
            callbacks[0].onAdFailedToLoad(mock(LoadAdError::class.java))
            verify(oldView).destroy()
            assertEquals(2, callbacks.size)

            callbacks[0].onAdLoaded(mock(BannerAd::class.java))
            val holder = holders()[placeName] as BannerAdHolder
            assertTrue(holder.isLoading)
            assertNull(holder.bannerAd)

            callbacks[1].onAdLoaded(mock(BannerAd::class.java))
            assertSame(views.constructed()[1], holder.bannerAd)
            assertNull(holder.loadingBannerAd)
        }
    }

    @Test
    fun `synchronous rewarded show exception clears showing and cached ad`() {
        val place = NoneAdPlace()
        val ad = mock(RewardedAd::class.java)
        val holder = RewardedAdHolder(place, ad)
        doThrow(IllegalStateException("Activity no longer valid")).`when`(ad).show(anyArg(), anyArg())

        val show = AdmobManager::class.java.getDeclaredMethod(
            "showRewardedVideo", Activity::class.java, RewardedAdHolder::class.java)
        show.isAccessible = true
        show.invoke(manager, activity, holder)

        assertFalse(holder.isShowing)
        assertNull(holder.rewardedAd)
    }

    @Test
    fun `destroyed Activity is rejected before showing a native dialog`() {
        activity.finish()
        val fragments = mock(FragmentManager::class.java)
        manager.showAd(activity, fragments, placeName, "test")
        verifyNoInteractions(config, fragments)
    }

    @Test
    fun `retry scheduled before release cannot start another request in a new session`() {
        prepareNative()
        `when`(config.getNativeAdConfig()).thenReturn(NativeAdTypeConfig(true, 1, true, 3600, listOf(5L)))
        manager.loadBannerNativeAd(activity, placeName, "old", false, false)
        ShadowNativeLoader.callbacks.single().onAdFailedToLoad(mock(LoadAdError::class.java))
        shadowOf(Looper.getMainLooper()).idle() // Start the old retry's delay.
        manager.releaseBannerNative(placeName)

        `when`(config.getNativeAdConfig()).thenReturn(NativeAdTypeConfig(false, 0, true, 3600, emptyList()))
        manager.loadBannerNativeAd(activity, placeName, "new", false, false)
        ShadowNativeLoader.callbacks.last().onAdFailedToLoad(mock(LoadAdError::class.java))
        shadowOf(Looper.getMainLooper()).idleFor(5, TimeUnit.SECONDS)

        assertEquals(2, ShadowNativeLoader.callbacks.size)
    }

    @Test
    fun `fullscreen callback after timeout cannot overwrite a replacement load`() {
        ShadowAdsReadiness.ready = true
        val holder = InterstitialAdHolder(NoneAdPlace(adId = "unit", adType = AdType.Interstitial))
        AdmobManager::class.java.getDeclaredField("adHolderFullScreenMap").let {
            it.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val cache = it.get(manager) as MutableMap<IAdPlaceName, AdHolder>
            cache[placeName] = holder
        }
        val load = AdmobManager::class.java.getDeclaredMethod(
            "loadInterstitialIfNeed", Activity::class.java, InterstitialAdHolder::class.java,
            Int::class.javaPrimitiveType)
        load.isAccessible = true
        load.invoke(manager, activity, holder, 0)
        val oldCallback = ShadowInterstitialLoader.callbacks.single()
        shadowOf(Looper.getMainLooper()).idleFor(30, TimeUnit.SECONDS)
        assertFalse(holder.isLoading)
        load.invoke(manager, activity, holder, 0)
        val oldAd = mock(InterstitialAd::class.java)
        onWorker { oldCallback.onAdLoaded(oldAd) }
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(holder.isLoading)
        verify(oldAd, never()).setImmersiveMode(true)
        val newAd = mock(InterstitialAd::class.java)
        val configuredOn = AtomicReference<Looper>()
        doAnswer { configuredOn.set(Looper.myLooper()); null }.`when`(newAd).setImmersiveMode(true)
        onWorker { ShadowInterstitialLoader.callbacks.last().onAdLoaded(newAd) }
        assertTrue(holder.isLoading)
        verify(newAd, never()).setImmersiveMode(true)
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(holder.isLoading)
        verify(newAd).setImmersiveMode(true)
        assertSame(Looper.getMainLooper(), configuredOn.get())
    }

    @Test
    fun `ad format change replaces incompatible fullscreen holder`() {
        val getHolder = AdmobManager::class.java.getDeclaredMethod(
            "getOrCreateAdHolderFullScreenBy", AdPlace::class.java, Boolean::class.javaPrimitiveType)
        getHolder.isAccessible = true
        val previous = getHolder.invoke(manager, NoneAdPlace(adType = AdType.Interstitial), true) as AdHolder
        val replacement = getHolder.invoke(manager, NoneAdPlace(adType = AdType.RewardedVideo), true)
        assertTrue(replacement is RewardedAdHolder)
        assertNotSame(previous, replacement)
    }

    private fun prepareAds() {
        ShadowAdsReadiness.ready = true
        val appConfig = mock(AppConfig::class.java)
        `when`(appConfig.isEnableAds).thenReturn(true)
        `when`(config.getAppConfig()).thenReturn(appConfig)
        `when`(config.getAdsDisableByCountry()).thenReturn(emptyList())
    }

    private fun prepareNative() {
        prepareAds()
        val place = mock(NativeAdPlace::class.java)
        `when`(place.placeName).thenReturn(placeName)
        `when`(place.adType).thenReturn(AdType.Native)
        `when`(place.isNativeType()).thenReturn(true)
        `when`(place.adId).thenReturn("native")
        `when`(place.highFloorAdIds).thenReturn(emptyList())
        `when`(place.nativeTemplateSize).thenReturn(NativeTemplateSize.Small)
        `when`(config.getAdPlaceBy(placeName)).thenReturn(place)
        `when`(config.getNativeAdConfig()).thenReturn(NativeAdTypeConfig(false, 0, true, 3600, emptyList()))
    }

    @Suppress("UNCHECKED_CAST")
    private fun holders(): MutableMap<IAdPlaceName, AdHolder> =
        AdmobManager::class.java.getDeclaredField("adHolderBannerNativeMap").let {
            it.isAccessible = true
            it.get(manager) as MutableMap<IAdPlaceName, AdHolder>
        }

    private fun <T> anyArg(): T = any<T>()

    private fun onWorker(block: () -> Unit) {
        val failure = AtomicReference<Throwable>()
        Thread { try { block() } catch (error: Throwable) { failure.set(error) } }
            .apply { start(); join(5_000); assertFalse("Worker did not finish", isAlive) }
        failure.get()?.let { throw AssertionError("Worker failed", it) }
    }
}

@Implements(className = "com.google.android.libraries.ads.mobile.sdk.MobileAds\$Companion", isInAndroidSdk = false)
class ShadowAdsReadiness {
    companion object {
        @Volatile var ready = false
        @Volatile var failInitialization = false
        var initializationEntered = CountDownLatch(1)
    }
    @Implementation fun isInitialized(): Boolean = ready
    @Implementation
    fun initialize(context: Context, config: InitializationConfig, listener: OnAdapterInitializationCompleteListener?) {
        check(Looper.myLooper() != Looper.getMainLooper()) { "Initialization must run off Main" }
        initializationEntered.countDown()
        if (failInitialization) throw IllegalStateException("SDK initialization failure")
        ready = true
    }
}

@Implements(className = "com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader\$Companion", isInAndroidSdk = false)
class ShadowNativeLoader {
    companion object { val callbacks = mutableListOf<NativeAdLoaderCallback>() }
    @Implementation
    fun load(request: NativeAdRequest, callback: NativeAdLoaderCallback) {
        callbacks += callback
    }
}

@Implements(className = "com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd\$Companion", isInAndroidSdk = false)
class ShadowInterstitialLoader {
    companion object { val callbacks = mutableListOf<AdLoadCallback<InterstitialAd>>() }
    @Implementation
    fun load(request: AdRequest, callback: AdLoadCallback<InterstitialAd>) {
        callbacks += callback
    }
}
