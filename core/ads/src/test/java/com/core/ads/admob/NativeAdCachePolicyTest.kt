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

    @Test
    fun `failed reload preserves the cached native`() {
        assertTrue(
            shouldPreserveCachedNativeAdOnLoadFailure(
                isReload = true,
                hasCachedNativeAd = true,
            ),
        )
    }

    @Test
    fun `initial failure cannot preserve a missing native`() {
        assertFalse(
            shouldPreserveCachedNativeAdOnLoadFailure(
                isReload = false,
                hasCachedNativeAd = false,
            ),
        )
        assertFalse(
            shouldPreserveCachedNativeAdOnLoadFailure(
                isReload = true,
                hasCachedNativeAd = false,
            ),
        )
    }
}
