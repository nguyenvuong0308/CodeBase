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

    @Test
    fun `medium media left cta right key resolves to built in template`() {
        val result = NativeTemplateSize.getSizeBy("medium_media_left_cta_right")

        assertSame(NativeTemplateSize.MediumMediaLeftCtaRight, result)
        assertEquals("medium_media_left_cta_right", result.key)
    }

    @Test
    fun `similar unknown key remains custom`() {
        val result = NativeTemplateSize.getSizeBy("medium_media_left_cta_right_custom")

        assertEquals("custom", result.key)
        assertEquals(
            "medium_media_left_cta_right_custom",
            (result as NativeTemplateSize.CustomKey).customKey,
        )
    }

    @Test
    fun `built in templates have unique keys and resolve to the same instances`() {
        val templates = NativeTemplateSize.builtInTemplates

        assertEquals(templates.size, templates.map { it.key }.distinct().size)
        templates.forEach { template ->
            assertSame(template, NativeTemplateSize.getSizeBy(template.key))
        }
    }
}
