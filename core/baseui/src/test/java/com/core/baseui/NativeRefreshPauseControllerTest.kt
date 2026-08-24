package com.core.baseui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeRefreshPauseControllerTest {

    private val controller = NativeRefreshPauseController()

    @Test
    fun `pause blocks refresh and resume allows it again`() {
        assertTrue(
            controller.canRefresh(
                isWaitingLoad = false,
                isLifecycleStarted = true
            )
        )

        controller.pause()

        assertFalse(
            controller.canRefresh(
                isWaitingLoad = false,
                isLifecycleStarted = true
            )
        )

        controller.resume()

        assertTrue(
            controller.canRefresh(
                isWaitingLoad = false,
                isLifecycleStarted = true
            )
        )
    }

    @Test
    fun `resume does not bypass stopped lifecycle`() {
        controller.pause()
        controller.resume()

        assertFalse(
            controller.canRefresh(
                isWaitingLoad = false,
                isLifecycleStarted = false
            )
        )
    }

    @Test
    fun `resume does not bypass waiting load state`() {
        controller.pause()
        controller.resume()

        assertFalse(
            controller.canRefresh(
                isWaitingLoad = true,
                isLifecycleStarted = true
            )
        )
    }
}
