package com.core.ads.customviews.ads

import com.core.ads.R
import com.core.config.domain.data.NativeTemplateSize
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun `large media cta right uses matching shimmer and 200dp media`() {
        val result = resolveNativeShimmerLayout(NativeTemplateSize.LargeMediaCtaRight) {
            fail("Built-in template must not use the custom layout fallback")
            0
        }

        assertEquals(R.layout.gnt_large_media_cta_right_shimmer, result)
        listOf(
            "gnt_large_media_cta_right.xml",
            "gnt_large_media_cta_right_shimmer.xml",
        ).forEach { layoutName ->
            val mediaView = parseLayout(layoutName)
                .getElementsByTagName("*")
                .asElements()
                .single { element ->
                    element.androidAttribute("id") == "@+id/media_view"
                }

            assertEquals(
                "$layoutName must keep the requested media height",
                "@dimen/_200dp",
                mediaView.androidAttribute("layout_height"),
            )
        }
    }

    @Test
    fun `small advertiser cta bottom uses matching shimmer and places cta below body`() {
        assertSmallAdvertiserTemplate(
            size = NativeTemplateSize.SmallAdvertiserCtaBottom,
            expectedShimmer = R.layout.gnt_small_advertiser_cta_bottom_template_view_shimmer,
            layoutPrefix = "gnt_small_advertiser_cta_bottom_template_view",
        ) { layoutName, byId ->
            assertEquals(
                "$layoutName must keep CTA below the body",
                "@+id/body",
                byId("@+id/layout_cta").androidAttribute("layout_below"),
            )
        }
    }

    @Test
    fun `small advertiser cta top uses matching shimmer and places cta above icon`() {
        assertSmallAdvertiserTemplate(
            size = NativeTemplateSize.SmallAdvertiserCtaTop,
            expectedShimmer = R.layout.gnt_small_advertiser_cta_top_template_view_shimmer,
            layoutPrefix = "gnt_small_advertiser_cta_top_template_view",
        ) { layoutName, byId ->
            val cta = byId("@+id/layout_cta")
            assertEquals("$layoutName must pin CTA to the top", "true", cta.androidAttribute("layout_alignParentTop"))
            assertEquals("", cta.androidAttribute("layout_below"))
            assertEquals(
                "$layoutName must place the icon below the CTA",
                "@+id/layout_cta",
                byId("@+id/icon").androidAttribute("layout_below"),
            )
            assertEquals(
                "$layoutName must keep body below the advertiser row",
                "@+id/layout_advertiser",
                byId("@+id/body").androidAttribute("layout_below"),
            )
        }
    }

    private fun assertSmallAdvertiserTemplate(
        size: NativeTemplateSize,
        expectedShimmer: Int,
        layoutPrefix: String,
        assertPosition: (layoutName: String, byId: (String) -> Element) -> Unit,
    ) {
        val result = resolveNativeShimmerLayout(size) {
            fail("Built-in template must not use the custom layout fallback")
            0
        }

        assertEquals(expectedShimmer, result)
        val requiredIds = listOf(
            "@+id/native_ad_view",
            "@+id/background",
            "@+id/icon",
            "@+id/primary",
            "@+id/ad_notification_view",
            "@+id/advertiser",
            "@+id/body",
            "@+id/layout_cta",
            "@+id/cta",
        )
        listOf("$layoutPrefix.xml", "${layoutPrefix}_shimmer.xml").forEach { layoutName ->
            val elements = parseLayout(layoutName).getElementsByTagName("*").asElements()
            val ids = elements.map { it.androidAttribute("id") }.toSet()
            requiredIds.forEach { id ->
                assertTrue("$layoutName must contain $id", id in ids)
            }
            val byId: (String) -> Element = { id -> elements.single { it.androidAttribute("id") == id } }

            assertEquals("match_parent", byId("@+id/layout_cta").androidAttribute("layout_width"))
            val advertiserRow = byId("@+id/ad_notification_view").parentNode as Element
            assertEquals(
                "$layoutName must place the Ad badge next to the advertiser",
                "@+id/layout_advertiser",
                advertiserRow.androidAttribute("id"),
            )
            assertPosition(layoutName, byId)
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
