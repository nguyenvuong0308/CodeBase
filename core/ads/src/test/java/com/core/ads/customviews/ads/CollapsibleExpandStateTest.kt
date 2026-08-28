package com.core.ads.customviews.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollapsibleExpandStateTest {

    @Test
    fun `manual expand bypasses cooldown and previous expanded state`() {
        val state = CollapsibleExpandState()

        state.setExpanded(true)

        assertTrue(
            state.shouldExpand(
                isSameShowingExpandedNativeAd = false,
                isExpandCooldownActive = true,
                hasNativeAdShownExpanded = true,
            ),
        )
    }

    @Test
    fun `automatic state keeps the popup when the same expanded ad is rebound`() {
        val state = CollapsibleExpandState()

        assertTrue(
            state.shouldExpand(
                isSameShowingExpandedNativeAd = true,
                isExpandCooldownActive = true,
                hasNativeAdShownExpanded = true,
            ),
        )
    }

    @Test
    fun `manual collapse wins when the same ad popup is showing`() {
        val state = CollapsibleExpandState()

        state.setExpanded(false)

        assertFalse(
            state.shouldExpand(
                isSameShowingExpandedNativeAd = true,
                isExpandCooldownActive = false,
                hasNativeAdShownExpanded = false,
            ),
        )
    }

    @Test
    fun `latest manual request wins after toggling repeatedly`() {
        val state = CollapsibleExpandState()

        state.setExpanded(true)
        state.setExpanded(false)
        state.setExpanded(true)

        assertTrue(
            state.shouldExpand(
                isSameShowingExpandedNativeAd = false,
                isExpandCooldownActive = true,
                hasNativeAdShownExpanded = true,
            ),
        )
    }

    @Test
    fun `manual request stops applying once it has been reset`() {
        val state = CollapsibleExpandState()

        state.setExpanded(true)
        state.reset()

        assertFalse(
            state.shouldExpand(
                isSameShowingExpandedNativeAd = false,
                isExpandCooldownActive = false,
                hasNativeAdShownExpanded = true,
            ),
        )
    }

    @Test
    fun `automatic state expands a new ad when no blocker is active`() {
        val state = CollapsibleExpandState()

        assertTrue(
            state.shouldExpand(
                isSameShowingExpandedNativeAd = false,
                isExpandCooldownActive = false,
                hasNativeAdShownExpanded = false,
            ),
        )
    }

    @Test
    fun `automatic state expands a new ad even while the cooldown is active`() {
        val state = CollapsibleExpandState()

        assertTrue(
            state.shouldExpand(
                isSameShowingExpandedNativeAd = false,
                isExpandCooldownActive = true,
                hasNativeAdShownExpanded = false,
            ),
        )
    }

    @Test
    fun `automatic state keeps an already expanded ad collapsed`() {
        val state = CollapsibleExpandState()

        assertFalse(
            state.shouldExpand(
                isSameShowingExpandedNativeAd = false,
                isExpandCooldownActive = false,
                hasNativeAdShownExpanded = true,
            ),
        )
    }
}
