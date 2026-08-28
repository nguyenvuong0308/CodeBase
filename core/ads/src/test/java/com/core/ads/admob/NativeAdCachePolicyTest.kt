package com.core.ads.admob

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAdCachePolicyTest {

    @Test
    fun `valid cached native is reused for a regular load`() {
        assertTrue(
            shouldUseCachedNativeAd(
                isExpired = false,
                isReload = false,
            ),
        )
    }

    @Test
    fun `cached native is not emitted during reload`() {
        assertFalse(
            shouldUseCachedNativeAd(
                isExpired = false,
                isReload = true,
            ),
        )
    }

    @Test
    fun `expired cached native is not emitted`() {
        assertFalse(
            shouldUseCachedNativeAd(
                isExpired = true,
                isReload = false,
            ),
        )
    }
}
