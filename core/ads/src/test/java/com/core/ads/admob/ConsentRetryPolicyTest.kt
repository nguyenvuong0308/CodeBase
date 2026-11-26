package com.core.ads.admob

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsentRetryPolicyTest {

    private fun decide(
        isPending: Boolean = true,
        isSplashConsentRequesting: Boolean = false,
        isRetrying: Boolean = false,
        hasForegroundActivity: Boolean = true,
        hasInternet: Boolean = true,
        isFullscreenAdShowing: Boolean = false,
        elapsedSinceLastAttemptMs: Long = CONSENT_RETRY_MIN_INTERVAL_MS,
    ) = shouldRetryConsent(
        isPending, isSplashConsentRequesting, isRetrying, hasForegroundActivity,
        hasInternet, isFullscreenAdShowing, elapsedSinceLastAttemptMs,
    )

    @Test
    fun `retries when consent is pending, online, in foreground and interval passed`() {
        assertTrue(decide())
    }

    @Test
    fun `does not retry when nothing is pending`() {
        assertFalse(decide(isPending = false))
    }

    @Test
    fun `does not overlap the splash consent flow or another retry`() {
        assertFalse(decide(isSplashConsentRequesting = true))
        assertFalse(decide(isRetrying = true))
    }

    @Test
    fun `needs a foreground activity and internet`() {
        assertFalse(decide(hasForegroundActivity = false))
        assertFalse(decide(hasInternet = false))
    }

    @Test
    fun `does not show consent over a fullscreen ad`() {
        assertFalse(decide(isFullscreenAdShowing = true))
    }

    @Test
    fun `throttles retries inside the min interval`() {
        assertFalse(decide(elapsedSinceLastAttemptMs = CONSENT_RETRY_MIN_INTERVAL_MS - 1))
    }
}
