package com.core.config.data

import android.content.Context
import android.content.pm.ApplicationInfo
import com.core.config.DebugRemoteConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class DebugTestAdProtectionTest {
    private val context = mock<Context>()
    private val applicationInfo = mock<ApplicationInfo>()
    private val service = mock<RemoteConfigService>()
    private lateinit var repository: RemoteConfigRepositoryImpl

    @Before
    fun setUp() {
        DebugRemoteConfig.isTurnOnAdPlacesDisabledWhenDetectTestAd = null
        applicationInfo.flags = ApplicationInfo.FLAG_DEBUGGABLE
        Mockito.`when`(context.applicationInfo).thenReturn(applicationInfo)
        repository = RemoteConfigRepositoryImpl(
            applicationContext = context,
            analyticsManager = mock(),
            appPreferences = mock(),
            appConfigModelMapper = mock(),
            iapConfigModelMapper = mock(),
            preventAdClickConfigModelMapper = mock(),
            splashScreenConfigModelMapper = mock(),
            adPlaceModelMapper = mock(),
            bannerAdConfigModelMapper = mock(),
            nativeAdConfigModelMapper = mock(),
            interstitialAdConfigModelMapper = mock(),
            interstitialRewardedAdConfigModelMapper = mock(),
            rewardedAdConfigModelMapper = mock(),
            appOpenAdConfigModelMapper = mock(),
            requestConsentConfigModelMapper = mock(),
            tutorialConfigModelMapper = mock(),
            remoteConfigService = service,
            getRemoteConfigUseCase = mock(),
        )
    }

    @After
    fun tearDown() {
        DebugRemoteConfig.isTurnOnAdPlacesDisabledWhenDetectTestAd = null
    }

    @Test
    fun `debug override supports both values without reading Firebase`() {
        for (override in listOf(true, false)) {
            DebugRemoteConfig.isTurnOnAdPlacesDisabledWhenDetectTestAd = override
            assertEquals(override, repository.isTurnOnAdPlacesDisabledWhenDetectTestAd())
        }
        Mockito.verifyNoInteractions(service)
    }

    @Test
    fun `debug override takes priority over cached remote value`() {
        for (override in listOf(true, false)) {
            setCachedValue(!override)
            DebugRemoteConfig.isTurnOnAdPlacesDisabledWhenDetectTestAd = override
            assertEquals(override, repository.isTurnOnAdPlacesDisabledWhenDetectTestAd())
        }
        Mockito.verifyNoInteractions(service)
    }

    @Test
    fun `null override uses Firebase when cache is empty`() {
        for (remote in listOf(true, false)) {
            Mockito.`when`(service.isTurnOnAdPlacesDisabledWhenDetectTestAd()).thenReturn(remote)
            assertEquals(remote, repository.isTurnOnAdPlacesDisabledWhenDetectTestAd())
        }
    }

    @Test
    fun `clearing override restores cached remote value`() {
        for (cached in listOf(true, false)) {
            setCachedValue(cached)
            DebugRemoteConfig.isTurnOnAdPlacesDisabledWhenDetectTestAd = !cached
            assertEquals(!cached, repository.isTurnOnAdPlacesDisabledWhenDetectTestAd())
            DebugRemoteConfig.isTurnOnAdPlacesDisabledWhenDetectTestAd = null
            assertEquals(cached, repository.isTurnOnAdPlacesDisabledWhenDetectTestAd())
        }
        Mockito.verifyNoInteractions(service)
    }

    @Test
    fun `release ignores every debug override and uses Firebase`() {
        applicationInfo.flags = ApplicationInfo.FLAG_HAS_CODE
        for (override in listOf(true, false, null)) {
            DebugRemoteConfig.isTurnOnAdPlacesDisabledWhenDetectTestAd = override
            for (remote in listOf(true, false)) {
                Mockito.`when`(service.isTurnOnAdPlacesDisabledWhenDetectTestAd()).thenReturn(remote)
                assertEquals(remote, repository.isTurnOnAdPlacesDisabledWhenDetectTestAd())
            }
        }
    }

    @Test
    fun `release ignores debug override when remote value is cached`() {
        applicationInfo.flags = 0
        for (cached in listOf(true, false)) {
            setCachedValue(cached)
            DebugRemoteConfig.isTurnOnAdPlacesDisabledWhenDetectTestAd = !cached
            assertEquals(cached, repository.isTurnOnAdPlacesDisabledWhenDetectTestAd())
        }
        Mockito.verifyNoInteractions(service)
    }

    private fun setCachedValue(value: Boolean) {
        RemoteConfigRepositoryImpl::class.java
            .getDeclaredField("isTurnOnAdPlacesDisabledWhenDetectTestAdCache")
            .apply { isAccessible = true }
            .set(repository, value)
    }

    private inline fun <reified T> mock(): T = Mockito.mock(T::class.java)
}
