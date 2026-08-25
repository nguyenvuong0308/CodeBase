package com.codebasetemplate.required.startflow

import com.core.config.domain.data.OnBoardingConfig
import com.core.startflow.onboarding.v3.OnBoardingV3ActionPosition
import com.core.startflow.onboarding.v3.OnBoardingV3PageState
import com.core.startflow.onboarding.v3.OnBoardingV3PageType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOnBoardingV3PageRendererTest {

    private val normalRenderer = AppOnBoardingV3PageRenderer()
    private val fullAdsRenderer = AppOnBoardingV3FullAdsPageRenderer()

    @Test
    fun `normal renderer supports normal ad page`() {
        assertTrue(normalRenderer.supports(pageState(isShowAd = true, isFullAds = false)))
    }

    @Test
    fun `normal renderer rejects no-ad and full-ad pages`() {
        assertFalse(normalRenderer.supports(pageState(isShowAd = false, isFullAds = false)))
        assertFalse(normalRenderer.supports(pageState(isShowAd = true, isFullAds = true)))
    }

    @Test
    fun `full ads renderer only supports full-ad page`() {
        assertTrue(fullAdsRenderer.supports(pageState(isShowAd = true, isFullAds = true)))
        assertFalse(fullAdsRenderer.supports(pageState(isShowAd = true, isFullAds = false)))
    }

    private fun pageState(
        isShowAd: Boolean,
        isFullAds: Boolean,
    ) = OnBoardingV3PageState(
        pageType = OnBoardingV3PageType.STANDARD,
        introductionPosition = 0,
        realPosition = 0,
        pageCount = 4,
        isPageEnd = false,
        isShowAd = isShowAd,
        actionPosition = OnBoardingV3ActionPosition.TOP,
        imageRes = 0,
        title = "Title",
        subtitle = null,
        config = OnBoardingConfig(version = OnBoardingConfig.ONBOARDING_VERSION_3),
        isFullAds = isFullAds,
    )
}
