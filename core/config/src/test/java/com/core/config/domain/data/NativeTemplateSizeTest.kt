package com.core.config.domain.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class NativeTemplateSizeTest {

    @Test
    fun `small banner cta right key resolves to built in template`() {
        val result = NativeTemplateSize.getSizeBy("small_banner_cta_right")

        assertSame(NativeTemplateSize.SmallBannerCtaRight, result)
        assertEquals("small_banner_cta_right", result.key)
    }
}
