package com.core.ads.admob

import android.app.Activity
import android.app.Application
import android.os.Looper
import android.os.SystemClock
import androidx.fragment.app.FragmentManager
import com.core.ads.AdsSdkInitializer
import com.core.ads.domain.AdFullScreenUiResource
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.ads.domain.DialogNativeFakeInterstitial
import com.core.ads.model.NativeAdHolder
import com.core.ads.model.PreventShowManyInterstitialAds
import com.core.analytics.AdjustAnalytics
import com.core.analytics.AnalyticsManager
import com.core.config.domain.RemoteConfigRepository
import com.core.config.domain.data.*
import com.core.preference.AppPreferences
import com.core.preference.PurchasePreferences
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import java.util.concurrent.TimeUnit
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class,
    shadows = [ShadowAdsReadiness::class, ShadowNativeLoader::class])
@LooperMode(LooperMode.Mode.PAUSED)
class NativeInterstitialTest {
    private val name = CoreAdPlaceName.ACTION_OPEN_APP
    private lateinit var manager: AdmobManager
    private lateinit var activity: Activity
    private lateinit var place: NativeAdPlace
    private lateinit var config: RemoteConfigRepository
    private lateinit var fragments: FragmentManager
    private val scope = CoroutineScope(Dispatchers.Main)
    private val events = mutableListOf<AdFullScreenUiResource>()
    private val nativeEvents = mutableListOf<AdLoadBannerNativeUiResource>()

    @Before
    fun setUp() {
        ShadowAdsReadiness.ready = true
        ShadowNativeLoader.callbacks.clear()
        PreventShowManyInterstitialAds.initIntervalTimeShowInterstitialMillis()
        config = mock(RemoteConfigRepository::class.java)
        val app = mock(AppConfig::class.java)
        `when`(app.isEnableAds).thenReturn(true)
        `when`(config.getAppConfig()).thenReturn(app)
        `when`(config.getAdsDisableByCountry()).thenReturn(emptyList())
        `when`(config.getNativeAdConfig()).thenReturn(NativeAdTypeConfig(false, 0, false, 3600, emptyList()))
        `when`(config.getInterstitialAdConfig()).thenReturn(
            InterstitialAdTypeConfig(false, Int.MAX_VALUE, 3600L, 0, 0, 0, false, 0, emptyList()))
        place = mock(NativeAdPlace::class.java)
        `when`(place.placeName).thenReturn(name)
        `when`(place.adType).thenReturn(AdType.NativeInterstitial)
        `when`(place.isNativeInterstitialType()).thenReturn(true)
        `when`(place.adId).thenReturn("native-unit")
        `when`(place.highFloorAdIds).thenReturn(emptyList())
        `when`(place.expiredTimeSecond).thenReturn(null)
        `when`(place.nativeTemplateSize).thenReturn(NativeTemplateSize.FullInterstitialV3)
        `when`(config.getAdPlaceBy(name)).thenReturn(place)
        manager = AdmobManager(RuntimeEnvironment.getApplication(), config,
            mock(PurchasePreferences::class.java), mock(AnalyticsManager::class.java),
            mock(AppPreferences::class.java), mock(AdsSdkInitializer::class.java),
            mock(AdjustAnalytics::class.java))
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        fragments = mock(FragmentManager::class.java)
        scope.launch(start = CoroutineStart.UNDISPATCHED) { manager.adFullScreenFlow.collect { events += it } }
        scope.launch(start = CoroutineStart.UNDISPATCHED) { manager.adLoadBannerNativeFlow.collect { nativeEvents += it } }
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
    fun `fullscreen load uses native SDK and emits fullscreen loaded only including cache hits`() {
        val ad = loadAd()
        assertSame(ad, holder().nativeAd)
        assertEquals(1, events.filterIsInstance<AdFullScreenUiResource.AdLoaded>().size)
        assertTrue(nativeEvents.isEmpty())

        manager.loadFullscreenAd(activity, name, "cached")
        idle()
        assertEquals(1, ShadowNativeLoader.callbacks.size)
        assertEquals(2, events.filterIsInstance<AdFullScreenUiResource.AdLoaded>().size)
    }

    @Test
    fun `load failure emits fullscreen failure even when native hide on error is disabled`() {
        manager.loadFullscreenAd(activity, name, "load")
        ShadowNativeLoader.callbacks.single().onAdFailedToLoad(mock(LoadAdError::class.java))
        idle()
        assertEquals(1, events.filterIsInstance<AdFullScreenUiResource.AdNotValidOrLoadFailed>().size)
        assertTrue(nativeEvents.isEmpty())
        assertFalse(holder().isLoading)
    }

    @Test
    fun `show without cached ad completes false when wait is disabled`() {
        manager.showAd(activity, fragments, name, "show")
        idle()
        assertEquals(listOf(false), completions())
        assertTrue(ShadowNativeLoader.callbacks.isEmpty())
    }

    @Test
    fun `show joins preload and completes only after dialog closes consuming cached ad once`() {
        manager.loadFullscreenAd(activity, name, "preload")
        val callbacks = DialogCallbacks()
        mockDialogs(callbacks).use { dialogs ->
            manager.showAd(activity, fragments, name, "show", true)
            assertTrue(holder().isWaitLoadToShow)
            assertEquals(1, ShadowNativeLoader.callbacks.size)
            val ad = mock(NativeAd::class.java)
            ShadowNativeLoader.callbacks.single().onNativeAdLoaded(ad)
            idle()
            assertEquals(1, dialogs.constructed().size)
            verify(dialogs.constructed().single()).show(fragments, "DialogNativeInterstitial_show")
            assertTrue(completions().isEmpty())
            callbacks.shown!!.invoke()
            idle()
            assertTrue(manager.isHasFullscreenAdShowing())
            assertEquals(1, events.filterIsInstance<AdFullScreenUiResource.AdSucceedToShow>().size)
            callbacks.close!!.invoke()
            callbacks.close!!.invoke()
            idle()
            assertEquals(listOf(true), completions())
            assertEquals(1, events.filterIsInstance<AdFullScreenUiResource.AdDismissed>().size)
            assertFalse(manager.isHasFullscreenAdShowing())
            assertNull(holder().nativeAd)
            verify(ad, times(1)).destroy()
        }
    }

    @Test
    fun `wait load failure releases callback without needing banner native events`() {
        manager.showAd(activity, fragments, name, "wait", true)
        ShadowNativeLoader.callbacks.single().onAdFailedToLoad(mock(LoadAdError::class.java))
        idle()
        assertEquals(listOf(false), completions())
        assertFalse(holder().isWaitLoadToShow)
        assertNull(holder().onFullscreenLoadFinished)
        assertTrue(nativeEvents.isEmpty())
    }

    @Test
    fun `wait timeout completes once and destroys late SDK result`() {
        manager.showAd(activity, fragments, name, "wait", true)
        val callback = ShadowNativeLoader.callbacks.single()
        shadowOf(Looper.getMainLooper()).idleFor(30, TimeUnit.SECONDS)
        assertEquals(listOf(false), completions())
        assertFalse(holder().isLoading)
        val late = mock(NativeAd::class.java)
        callback.onNativeAdLoaded(late)
        idle()
        verify(late).destroy()
        assertEquals(listOf(false), completions())
        assertNull(holder().nativeAd)
    }

    @Test
    fun `expired native cannot be shown`() {
        val ad = loadAd()
        holder().loadedAtMs = SystemClock.elapsedRealtime() - 3_600_001L
        manager.showAd(activity, fragments, name, "expired")
        idle()
        verify(ad).destroy()
        assertNull(holder().nativeAd)
        assertEquals(listOf(false), completions())
    }

    @Test
    fun `saved fragment state after load skips pending show`() {
        manager.showAd(activity, fragments, name, "wait", true)
        `when`(fragments.isStateSaved).thenReturn(true)
        ShadowNativeLoader.callbacks.single().onNativeAdLoaded(mock(NativeAd::class.java))
        idle()
        assertEquals(listOf(false), completions())
        assertFalse(holder().isShowing)
    }

    @Test
    fun `closing with auto load enabled requests a fresh native via fullscreen API`() {
        `when`(place.isAutoLoadAfterDismiss).thenReturn(true)
        loadAd()
        val callbacks = DialogCallbacks()
        mockDialogs(callbacks).use {
            manager.showAd(activity, fragments, name, "show")
            callbacks.shown!!.invoke()
            callbacks.close!!.invoke()
            idle()
            assertEquals(2, ShadowNativeLoader.callbacks.size)
            assertNull(holder().nativeAd)
            assertTrue(holder().isLoading)
            assertEquals(listOf(true), completions())
        }
    }

    @Test
    fun `dialog show exception clears holder and completes false`() {
        loadAd()
        mockConstruction(DialogNativeFakeInterstitial::class.java) { dialog, _ ->
            doThrow(IllegalStateException("state saved")).`when`(dialog).show(fragments, "DialogNativeInterstitial_show")
        }.use {
            manager.showAd(activity, fragments, name, "show")
            idle()
            assertEquals(listOf(false), completions())
            assertNull(holder().nativeAd)
            assertFalse(holder().isShowing)
        }
    }

    @Test
    fun `session cap blocks native interstitial like regular interstitial`() {
        loadAd()
        `when`(config.getInterstitialAdConfig()).thenReturn(
            InterstitialAdTypeConfig(false, 0, 3600, 0, 0, 0, false, 0, emptyList()))
        manager.showAd(activity, fragments, name, "capped")
        idle()
        assertEquals(listOf(false), completions())
        assertFalse(holder().isShowing)
    }

    private class DialogCallbacks {
        var shown: (() -> Unit)? = null
        var close: (() -> Unit)? = null
    }

    private fun mockDialogs(callbacks: DialogCallbacks) =
        mockConstruction(DialogNativeFakeInterstitial::class.java) { dialog, _ ->
            doAnswer { callbacks.shown = it.getArgument(0); null }.`when`(dialog).onShown = anyArg()
            doAnswer { callbacks.close = it.getArgument(0); null }.`when`(dialog).onClose = anyArg()
        }

    private fun loadAd(): NativeAd {
        manager.loadFullscreenAd(activity, name, "load")
        val ad = mock(NativeAd::class.java)
        ShadowNativeLoader.callbacks.last().onNativeAdLoaded(ad)
        idle()
        return ad
    }

    private fun holder() = requireNotNull(manager.getNativeHolder(activity, name))
    private fun completions() = events.filterIsInstance<AdFullScreenUiResource.AdCompleted>().map { it.isShown }
    private fun idle() = shadowOf(Looper.getMainLooper()).idle()
    private fun <T> anyArg(): T = any<T>()
}
