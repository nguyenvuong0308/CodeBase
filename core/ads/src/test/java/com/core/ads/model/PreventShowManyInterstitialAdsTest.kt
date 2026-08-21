package com.core.ads.model

import com.core.config.domain.data.AdType
import com.core.config.domain.data.CoreAdPlaceName
import com.core.config.domain.data.InterstitialAdPlace
import com.core.config.domain.data.InterstitialAdTypeConfig
import com.core.config.domain.data.NativeAfterInterstitialLoadStrategy
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PreventShowManyInterstitialAdsTest {

    @Before
    fun setUp() {
        PreventShowManyInterstitialAds.initIntervalTimeShowInterstitialMillis()
    }

    @After
    fun tearDown() {
        PreventShowManyInterstitialAds.initIntervalTimeShowInterstitialMillis()
    }

    @Test
    fun `first interstitial does not require meaningful actions`() {
        assertFalse(
            PreventShowManyInterstitialAds.isNotValidMeaningfulActionCount(
                interstitialConfig(meaningfulActions = 3)
            )
        )
    }

    @Test
    fun `interstitial is blocked until meaningful action threshold is reached`() {
        val config = interstitialConfig(meaningfulActions = 3)
        PreventShowManyInterstitialAds.resetMeaningfulActionsAfterInterstitial()

        repeat(2) {
            PreventShowManyInterstitialAds.recordMeaningfulAction()
        }
        assertTrue(PreventShowManyInterstitialAds.isNotValidMeaningfulActionCount(config))

        PreventShowManyInterstitialAds.recordMeaningfulAction()
        assertFalse(PreventShowManyInterstitialAds.isNotValidMeaningfulActionCount(config))

        PreventShowManyInterstitialAds.resetMeaningfulActionsAfterInterstitial()
        assertTrue(PreventShowManyInterstitialAds.isNotValidMeaningfulActionCount(config))
    }

    @Test
    fun `non-positive meaningful action config keeps existing behavior`() {
        PreventShowManyInterstitialAds.resetMeaningfulActionsAfterInterstitial()

        assertFalse(
            PreventShowManyInterstitialAds.isNotValidMeaningfulActionCount(
                interstitialConfig(meaningfulActions = 0)
            )
        )
        assertFalse(
            PreventShowManyInterstitialAds.isNotValidMeaningfulActionCount(
                interstitialConfig(meaningfulActions = -1)
            )
        )
    }

    @Test
    fun `ignore interval bypasses meaningful action cap`() {
        PreventShowManyInterstitialAds.resetMeaningfulActionsAfterInterstitial()

        assertFalse(
            PreventShowManyInterstitialAds.isNotValidTimeToShow(
                interstitialConfig(meaningfulActions = 3),
                interstitialPlace(isIgnoreInterval = true)
            )
        )
    }

    private fun interstitialConfig(meaningfulActions: Int) = InterstitialAdTypeConfig(
        isWaitLoadToShow = false,
        adsPerSession = Int.MAX_VALUE,
        timePerSession = Long.MAX_VALUE,
        timeInterval = 0,
        timeIntervalAfterShowOpenAd = 0,
        meaningfulActionsBetweenInterstitial = meaningfulActions,
        isEnableRetry = false,
        maxRetryCount = 0,
        retryIntervalSecondList = emptyList()
    )

    private fun interstitialPlace(isIgnoreInterval: Boolean) = InterstitialAdPlace(
        isTrackingShow = false,
        isTrackingClick = false,
        placeName = CoreAdPlaceName.NONE,
        adId = "test-ad-id",
        highFloorAdIds = emptyList(),
        isEnable = true,
        adType = AdType.Interstitial,
        isAutoLoadAfterDismiss = false,
        isIgnoreInterval = isIgnoreInterval,
        isTutorialFlow = false,
        plusInterval = 0,
        isShowNativeAfter = false,
        nativeAfterLoadStrategy = NativeAfterInterstitialLoadStrategy.WithInterstitial,
        nativeAfterInterstitial = null
    )
}
