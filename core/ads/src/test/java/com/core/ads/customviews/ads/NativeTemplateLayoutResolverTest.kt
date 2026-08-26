package com.core.ads.customviews.ads

import com.core.ads.R
import com.core.config.domain.data.NativeTemplateSize
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class NativeTemplateLayoutResolverTest {

    @Test
    fun `small banner cta right uses its matching shimmer`() {
        val result = resolveNativeShimmerLayout(NativeTemplateSize.SmallBannerCtaRight) {
            fail("Built-in template must not use the custom layout fallback")
            0
        }

        assertEquals(R.layout.gnt_small_banner_cta_right_shimmer, result)
    }

    @Test
    fun `medium media left cta right uses its matching shimmer`() {
        val result = resolveNativeShimmerLayout(NativeTemplateSize.MediumMediaLeftCtaRight) {
            fail("Built-in template must not use the custom layout fallback")
            0
        }

        assertEquals(R.layout.gnt_medium_media_left_cta_right_shimmer, result)
    }
}
