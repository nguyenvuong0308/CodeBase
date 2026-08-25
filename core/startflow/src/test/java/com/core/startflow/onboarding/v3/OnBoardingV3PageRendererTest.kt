package com.core.startflow.onboarding.v3

import com.core.config.domain.data.OnBoardingConfig
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class OnBoardingV3PageRendererTest {

    @Test
    fun `standard renderer is not selected for full ads page`() {
        val renderer = standardRenderer(priority = 1)

        val selected = setOf(renderer).activeOnBoardingV3Renderer(pageState(isFullAds = true))

        assertNull(selected)
    }

    @Test
    fun `full ads renderer is not selected for standard page`() {
        val renderer = fullAdsRenderer(priority = 1)

        val selected = setOf(renderer)
            .activeOnBoardingV3FullAdsRenderer(pageState(isFullAds = false))

        assertNull(selected)
    }

    @Test
    fun `standard page selects standard renderer`() {
        val renderer = standardRenderer(priority = 1)

        val selected = setOf(renderer).activeOnBoardingV3Renderer(pageState(isFullAds = false))

        assertSame(renderer, selected)
    }

    @Test
    fun `full ads page selects highest priority supported full ads renderer`() {
        val unsupportedRenderer = fullAdsRenderer(priority = 3, isSupported = false)
        val lowerPriorityRenderer = fullAdsRenderer(priority = 1)
        val expectedRenderer = fullAdsRenderer(priority = 2)

        val selected = setOf(
            unsupportedRenderer,
            lowerPriorityRenderer,
            expectedRenderer,
        ).activeOnBoardingV3FullAdsRenderer(pageState(isFullAds = true))

        assertSame(expectedRenderer, selected)
    }

    private fun standardRenderer(priority: Int): OnBoardingV3PageRenderer =
        object : OnBoardingV3PageRenderer {
            override val priority = priority

            override fun render(scope: OnBoardingV3RenderScope): OnBoardingV3RenderedPage {
                error("render must not be called by selector tests")
            }
        }

    private fun fullAdsRenderer(
        priority: Int,
        isSupported: Boolean = true,
    ): OnBoardingV3FullAdsPageRenderer = object : OnBoardingV3FullAdsPageRenderer {
        override val priority = priority

        override fun supports(state: OnBoardingV3PageState) = isSupported

        override fun render(scope: OnBoardingV3RenderScope): OnBoardingV3RenderedPage {
            error("render must not be called by selector tests")
        }
    }

    private fun pageState(isFullAds: Boolean) = OnBoardingV3PageState(
        pageType = OnBoardingV3PageType.STANDARD,
        introductionPosition = 0,
        realPosition = 0,
        pageCount = 1,
        isPageEnd = false,
        isShowAd = true,
        actionPosition = OnBoardingV3ActionPosition.TOP,
        imageRes = 0,
        title = "Title",
        subtitle = null,
        config = OnBoardingConfig(version = OnBoardingConfig.ONBOARDING_VERSION_3),
        isFullAds = isFullAds,
    )
}
