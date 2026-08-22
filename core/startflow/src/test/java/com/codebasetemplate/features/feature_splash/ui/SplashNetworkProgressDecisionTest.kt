package com.codebasetemplate.features.feature_splash.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashNetworkProgressDecisionTest {

    @Test
    fun `enters app when network turns off after online prerequisites started`() {
        assertTrue(
            shouldEnterAppWhenNetworkLost(
                isNetworkConnected = false,
                enterAppOnNetworkLost = true,
                isProgressRunning = false
            )
        )
    }

    @Test
    fun `enters app when network turns off during progress`() {
        assertTrue(
            shouldEnterAppWhenNetworkLost(
                isNetworkConnected = false,
                enterAppOnNetworkLost = false,
                isProgressRunning = true
            )
        )
    }

    @Test
    fun `keeps current handling when network is off before progress starts`() {
        assertFalse(
            shouldEnterAppWhenNetworkLost(
                isNetworkConnected = false,
                enterAppOnNetworkLost = false,
                isProgressRunning = false
            )
        )
    }

    @Test
    fun `keeps waiting while network is connected during progress`() {
        assertFalse(
            shouldEnterAppWhenNetworkLost(
                isNetworkConnected = true,
                enterAppOnNetworkLost = true,
                isProgressRunning = true
            )
        )
    }
}