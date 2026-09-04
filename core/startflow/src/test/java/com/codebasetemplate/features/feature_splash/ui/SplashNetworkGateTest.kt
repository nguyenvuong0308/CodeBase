package com.codebasetemplate.features.feature_splash.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashNetworkGateTest {

    @Test
    fun `network callbacks before data ready are ignored without poisoning initial state`() {
        val gate = SplashNetworkGate()

        assertEquals(
            SplashNetworkAction.None,
            gate.onNetworkAvailabilityChanged(
                isNetworkAvailable = false,
                shouldGuardAds = true,
                isAdsFlowStarted = false,
            )
        )
        assertEquals(
            SplashNetworkAction.None,
            gate.onNetworkAvailabilityChanged(
                isNetworkAvailable = true,
                shouldGuardAds = true,
                isAdsFlowStarted = false,
            )
        )
        assertFalse(gate.isWaitingForNetwork)

        assertEquals(
            SplashNetworkAction.ShowNetworkPrompt,
            gate.onDataReady(isNetworkAvailable = false)
        )
        assertTrue(gate.isWaitingForNetwork)
    }

    @Test
    fun `data ready with network starts prerequisites only once`() {
        val gate = SplashNetworkGate()

        assertEquals(
            SplashNetworkAction.StartPrerequisites,
            gate.onDataReady(isNetworkAvailable = true)
        )
        assertEquals(
            SplashNetworkAction.None,
            gate.onDataReady(isNetworkAvailable = true)
        )
        assertFalse(gate.isWaitingForNetwork)
    }

    @Test
    fun `initially unavailable network waits then starts prerequisites when enabled`() {
        val gate = SplashNetworkGate()

        assertEquals(
            SplashNetworkAction.ShowNetworkPrompt,
            gate.onDataReady(isNetworkAvailable = false)
        )
        assertTrue(gate.isWaitingForNetwork)
        assertEquals(
            SplashNetworkAction.None,
            gate.onNetworkAvailabilityChanged(
                isNetworkAvailable = false,
                shouldGuardAds = true,
                isAdsFlowStarted = false,
            )
        )
        assertEquals(
            SplashNetworkAction.StartPrerequisites,
            gate.onNetworkAvailabilityChanged(
                isNetworkAvailable = true,
                shouldGuardAds = true,
                isAdsFlowStarted = false,
            )
        )
        assertFalse(gate.isWaitingForNetwork)
    }

    @Test
    fun `duplicate data ready cannot bypass an existing network wait`() {
        val gate = SplashNetworkGate()
        gate.onDataReady(isNetworkAvailable = false)

        assertEquals(
            SplashNetworkAction.None,
            gate.onDataReady(isNetworkAvailable = true)
        )
        assertTrue(gate.isWaitingForNetwork)
        assertEquals(
            SplashNetworkAction.StartPrerequisites,
            gate.onNetworkAvailabilityChanged(
                isNetworkAvailable = true,
                shouldGuardAds = true,
                isAdsFlowStarted = false,
            )
        )
    }

    @Test
    fun `network recovery during prerequisites retries incomplete prerequisites`() {
        val gate = SplashNetworkGate()
        gate.onDataReady(isNetworkAvailable = true)

        assertEquals(
            SplashNetworkAction.ShowNetworkPrompt,
            gate.onNetworkAvailabilityChanged(
                isNetworkAvailable = false,
                shouldGuardAds = true,
                isAdsFlowStarted = false,
            )
        )
        assertEquals(
            SplashNetworkAction.RetryPrerequisites,
            gate.onNetworkAvailabilityChanged(
                isNetworkAvailable = true,
                shouldGuardAds = true,
                isAdsFlowStarted = false,
            )
        )
    }

    @Test
    fun `network loss during ads pauses once and recovery resumes once`() {
        val gate = SplashNetworkGate()
        gate.onDataReady(isNetworkAvailable = true)

        assertEquals(
            SplashNetworkAction.PauseAds,
            gate.onNetworkAvailabilityChanged(
                isNetworkAvailable = false,
                shouldGuardAds = true,
                isAdsFlowStarted = true,
            )
        )
        assertEquals(
            SplashNetworkAction.None,
            gate.onNetworkAvailabilityChanged(
                isNetworkAvailable = false,
                shouldGuardAds = true,
                isAdsFlowStarted = true,
            )
        )
        assertEquals(
            SplashNetworkAction.ResumeAds,
            gate.onNetworkAvailabilityChanged(
                isNetworkAvailable = true,
                shouldGuardAds = true,
                isAdsFlowStarted = true,
            )
        )
        assertEquals(
            SplashNetworkAction.None,
            gate.onNetworkAvailabilityChanged(
                isNetworkAvailable = true,
                shouldGuardAds = true,
                isAdsFlowStarted = true,
            )
        )
    }

    @Test
    fun `network loss is ignored after ads no longer require guarding`() {
        val gate = SplashNetworkGate()
        gate.onDataReady(isNetworkAvailable = true)

        assertEquals(
            SplashNetworkAction.None,
            gate.onNetworkAvailabilityChanged(
                isNetworkAvailable = false,
                shouldGuardAds = false,
                isAdsFlowStarted = true,
            )
        )
        assertFalse(gate.isWaitingForNetwork)
    }

    @Test
    fun `cancel provides explicit fallback for initial and ads waits`() {
        val initialGate = SplashNetworkGate()
        initialGate.onDataReady(isNetworkAvailable = false)
        assertEquals(
            SplashNetworkAction.StartPrerequisites,
            initialGate.continueWithoutNetwork(isAdsFlowStarted = false)
        )
        assertFalse(initialGate.isWaitingForNetwork)

        val adsGate = SplashNetworkGate()
        adsGate.onDataReady(isNetworkAvailable = true)
        adsGate.onNetworkAvailabilityChanged(
            isNetworkAvailable = false,
            shouldGuardAds = true,
            isAdsFlowStarted = true,
        )
        assertEquals(
            SplashNetworkAction.ResumeAds,
            adsGate.continueWithoutNetwork(isAdsFlowStarted = true)
        )
        assertFalse(adsGate.isWaitingForNetwork)
    }

    @Test
    fun `offline fallback prevents repeated pause until network is enabled again`() {
        val gate = SplashNetworkGate()
        gate.onDataReady(isNetworkAvailable = true)
        gate.onNetworkAvailabilityChanged(
            isNetworkAvailable = false,
            shouldGuardAds = true,
            isAdsFlowStarted = true,
        )
        gate.continueWithoutNetwork(isAdsFlowStarted = true)

        assertEquals(
            SplashNetworkAction.None,
            gate.onNetworkAvailabilityChanged(
                isNetworkAvailable = false,
                shouldGuardAds = true,
                isAdsFlowStarted = true,
            )
        )
        assertEquals(
            SplashNetworkAction.None,
            gate.onNetworkAvailabilityChanged(
                isNetworkAvailable = true,
                shouldGuardAds = true,
                isAdsFlowStarted = true,
            )
        )
        assertEquals(
            SplashNetworkAction.PauseAds,
            gate.onNetworkAvailabilityChanged(
                isNetworkAvailable = false,
                shouldGuardAds = true,
                isAdsFlowStarted = true,
            )
        )
    }

    @Test
    fun `cancel during prerequisites retries once and does not loop while still offline`() {
        val gate = SplashNetworkGate()
        gate.onDataReady(isNetworkAvailable = true)
        gate.onNetworkAvailabilityChanged(
            isNetworkAvailable = false,
            shouldGuardAds = true,
            isAdsFlowStarted = false,
        )

        assertEquals(
            SplashNetworkAction.RetryPrerequisites,
            gate.continueWithoutNetwork(isAdsFlowStarted = false)
        )
        assertEquals(
            SplashNetworkAction.None,
            gate.onNetworkAvailabilityChanged(
                isNetworkAvailable = false,
                shouldGuardAds = true,
                isAdsFlowStarted = false,
            )
        )
        assertFalse(gate.isWaitingForNetwork)
    }

    @Test
    fun `multiple offline cycles keep pause and resume actions balanced`() {
        val gate = SplashNetworkGate()
        gate.onDataReady(isNetworkAvailable = true)

        repeat(3) {
            assertEquals(
                SplashNetworkAction.PauseAds,
                gate.onNetworkAvailabilityChanged(
                    isNetworkAvailable = false,
                    shouldGuardAds = true,
                    isAdsFlowStarted = true,
                )
            )
            assertTrue(gate.isWaitingForNetwork)
            assertEquals(
                SplashNetworkAction.ResumeAds,
                gate.onNetworkAvailabilityChanged(
                    isNetworkAvailable = true,
                    shouldGuardAds = true,
                    isAdsFlowStarted = true,
                )
            )
            assertFalse(gate.isWaitingForNetwork)
        }
    }

    @Test
    fun `release network wait prevents stale dialog state after ad starts showing`() {
        val gate = SplashNetworkGate()
        gate.onDataReady(isNetworkAvailable = true)
        gate.onNetworkAvailabilityChanged(
            isNetworkAvailable = false,
            shouldGuardAds = true,
            isAdsFlowStarted = true,
        )

        gate.releaseNetworkWait()

        assertFalse(gate.isWaitingForNetwork)
        assertEquals(
            SplashNetworkAction.None,
            gate.onNetworkAvailabilityChanged(
                isNetworkAvailable = false,
                shouldGuardAds = false,
                isAdsFlowStarted = true,
            )
        )
    }
}
