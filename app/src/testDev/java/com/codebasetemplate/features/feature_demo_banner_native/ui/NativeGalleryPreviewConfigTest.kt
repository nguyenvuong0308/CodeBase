package com.codebasetemplate.features.feature_demo_banner_native.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeGalleryPreviewConfigTest {

    @Test
    fun `full and interstitial templates use full screen preview`() {
        listOf(
            "full_cta_bottom",
            "full_cta_bottom_onboarding",
            "full_cta_top",
            "full_cta_right",
            "full_interstitial_v1",
            "full_interstitial_v2",
            "full_interstitial_v3",
        ).forEach { templateKey ->
            assertTrue(templateKey, NativeGalleryPreviewConfig.requiresFullScreen(templateKey))
        }
    }

    @Test
    fun `inline and picture in picture previews do not fill host`() {
        assertFalse(NativeGalleryPreviewConfig.requiresFullScreen("small_banner_cta_right"))
        assertFalse(
            NativeGalleryPreviewConfig.requiresFullScreen(
                NativeGalleryPreviewConfig.NATIVE_PIP_KEY
            )
        )
    }

    @Test
    fun `ready marker is stable for adb capture`() {
        assertEquals(
            "native-gallery-ready:medium_cta_bottom",
            NativeGalleryPreviewConfig.readyDescription("medium_cta_bottom"),
        )
    }
}
