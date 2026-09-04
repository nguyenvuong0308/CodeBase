package com.core.baseui

import com.core.config.domain.data.RemoteAdPlaceName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeRefreshTransitionStateTest {

    private val adPlaceName = RemoteAdPlaceName("native_refresh_test")
    private val state = NativeRefreshTransitionState()

    @Test
    fun `old native remains for at least the configured transition time`() {
        state.start(adPlaceName, nowMs = 1_000L)

        assertEquals(
            1_500L,
            state.remainingOldAdDisplayMs(
                adPlaceName = adPlaceName,
                nowMs = 1_500L,
                minimumDisplayMs = 2_000L,
            ),
        )
        assertEquals(
            0L,
            state.remainingOldAdDisplayMs(
                adPlaceName = adPlaceName,
                nowMs = 3_500L,
                minimumDisplayMs = 2_000L,
            ),
        )
    }

    @Test
    fun `starting another refresh resets the old native display window`() {
        state.start(adPlaceName, nowMs = 1_000L)
        state.start(adPlaceName, nowMs = 2_500L)

        assertEquals(
            1_500L,
            state.remainingOldAdDisplayMs(
                adPlaceName = adPlaceName,
                nowMs = 3_000L,
                minimumDisplayMs = 2_000L,
            ),
        )
    }

    @Test
    fun `finishing reports whether a result belongs to an active refresh`() {
        assertFalse(state.finish(adPlaceName))

        state.start(adPlaceName, nowMs = 1_000L)

        assertTrue(state.isRefreshing(adPlaceName))
        assertTrue(state.finish(adPlaceName))
        assertFalse(state.isRefreshing(adPlaceName))
        assertFalse(state.finish(adPlaceName))
    }

    @Test
    fun `clear removes every active refresh`() {
        val secondPlaceName = RemoteAdPlaceName("native_refresh_test_2")
        state.start(adPlaceName, nowMs = 1_000L)
        state.start(secondPlaceName, nowMs = 2_000L)

        state.clear()

        assertFalse(state.isRefreshing(adPlaceName))
        assertFalse(state.isRefreshing(secondPlaceName))
    }
}
