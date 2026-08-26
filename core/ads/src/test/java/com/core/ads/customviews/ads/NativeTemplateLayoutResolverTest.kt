package com.core.ads.customviews.ads

import com.core.ads.R
import com.core.config.domain.data.NativeTemplateSize
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.w3c.dom.Element

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

    @Test
    fun `medium media cta right uses matching shimmer and 124dp media`() {
        val result = resolveNativeShimmerLayout(NativeTemplateSize.MediumMediaCtaRight) {
            fail("Built-in template must not use the custom layout fallback")
            0
        }

        assertEquals(R.layout.gnt_medium_media_cta_right_shimmer, result)
        listOf(
            "gnt_medium_media_cta_right.xml",
            "gnt_medium_media_cta_right_shimmer.xml",
        ).forEach { layoutName ->
            val mediaView = parseLayout(layoutName)
                .getElementsByTagName("*")
                .asElements()
                .single { element ->
                    element.androidAttribute("id") == "@+id/media_view"
                }

            assertEquals(
                "$layoutName must keep the requested media height",
                "@dimen/_124dp",
                mediaView.androidAttribute("layout_height"),
            )
        }
    }

    private fun parseLayout(layoutName: String) =
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(projectFile("src/main/res/layout/$layoutName"))

    private fun projectFile(relativePath: String): File =
        sequenceOf(File(relativePath), File("core/ads/$relativePath"))
            .firstOrNull(File::isFile)
            ?: error("Cannot find $relativePath")

    private fun org.w3c.dom.NodeList.asElements(): List<Element> =
        (0 until length).mapNotNull { index -> item(index) as? Element }

    private fun Element.androidAttribute(name: String): String =
        getAttributeNS(ANDROID_NAMESPACE, name)

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
