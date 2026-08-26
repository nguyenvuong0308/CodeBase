package com.codebasetemplate.features.feature_demo_banner_native.ui

import com.core.config.domain.data.NativeExpandTemplate
import com.core.config.domain.data.NativeTemplateSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
            NativeGalleryPreviewConfig.requiresFullScreen("medium_media_left_cta_right")
        )
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

    @Test
    fun `selector contains every built in template once plus three special formats`() {
        val options = NativeGalleryPreviewConfig.options
        val standardOptions = options.filter { option ->
            !option.arguments.collapsible &&
                option.arguments.templateKey != NativeGalleryPreviewConfig.NATIVE_PIP_KEY
        }

        assertEquals(
            NativeTemplateSize.builtInTemplates.map { it.key },
            standardOptions.map { it.arguments.templateKey },
        )
        assertEquals(NativeTemplateSize.builtInTemplates.size + 3, options.size)
        assertEquals(options.size, options.map { it.id }.distinct().size)
        assertTrue(options.all { it.title.isNotBlank() && it.group.isNotBlank() })
    }

    @Test
    fun `selector exposes medium media cta right as a standard format`() {
        val option = NativeGalleryPreviewConfig.options.single {
            it.arguments.templateKey == NativeTemplateSize.MediumMediaCtaRight.key
        }

        assertEquals("Medium", option.group)
        assertEquals("Medium Media Cta Right", option.title)
        assertFalse(option.arguments.collapsible)
    }

    @Test
    fun `selector exposes large media cta right as an inline standard format`() {
        val option = NativeGalleryPreviewConfig.options.single {
            it.arguments.templateKey == NativeTemplateSize.LargeMediaCtaRight.key
        }

        assertEquals("Large", option.group)
        assertEquals("Large Media Cta Right", option.title)
        assertFalse(option.arguments.collapsible)
        assertFalse(
            NativeGalleryPreviewConfig.requiresFullScreen(option.arguments.templateKey)
        )
    }

    @Test
    fun `special selector options map to collapsible and pip preview arguments`() {
        NativeGalleryPreviewConfig.options
            .filter { it.arguments.collapsible }
            .associateBy { it.arguments.expandTemplateKey }
            .also { collapsibleOptions ->
                assertEquals(
                    setOf(NativeExpandTemplate.V1.key, NativeExpandTemplate.V2.key),
                    collapsibleOptions.keys,
                )
                collapsibleOptions.values.forEach { option ->
                    assertEquals(
                        NativeTemplateSize.MediumCtaBottom.key,
                        option.arguments.templateKey,
                    )
                }
            }

        val pipOption = NativeGalleryPreviewConfig.options.single {
            it.arguments.templateKey == NativeGalleryPreviewConfig.NATIVE_PIP_KEY
        }
        assertFalse(pipOption.arguments.collapsible)
        assertNull(pipOption.arguments.expandTemplateKey)
    }
}
