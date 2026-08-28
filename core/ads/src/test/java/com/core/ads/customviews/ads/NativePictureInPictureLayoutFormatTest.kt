package com.core.ads.customviews.ads

import com.core.dimens.R as DimenR
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.w3c.dom.Element

class NativePictureInPictureLayoutFormatTest {

    @Test
    fun `layout format key is case insensitive and rejects unknown values`() {
        assertEquals(
            NativePictureInPicture.LayoutFormat.MediaCard,
            NativePictureInPicture.LayoutFormat.fromKey("MEDIA_CARD"),
        )
        assertEquals(
            NativePictureInPicture.LayoutFormat.Compact,
            NativePictureInPicture.LayoutFormat.fromKey("compact"),
        )
        assertNull(NativePictureInPicture.LayoutFormat.fromKey("large"))
        assertNull(NativePictureInPicture.LayoutFormat.fromKey(null))
    }

    @Test
    fun `remote layout format overrides config and invalid values use fallback`() {
        assertEquals(
            NativePictureInPicture.LayoutFormat.MediaCard,
            NativePictureInPicture.LayoutFormat.resolve(
                key = "media_card",
                fallback = NativePictureInPicture.LayoutFormat.Compact,
            ),
        )
        assertEquals(
            NativePictureInPicture.LayoutFormat.Compact,
            NativePictureInPicture.LayoutFormat.resolve(
                key = "unsupported",
                fallback = NativePictureInPicture.LayoutFormat.Compact,
            ),
        )
        assertEquals(
            NativePictureInPicture.LayoutFormat.MediaCard,
            NativePictureInPicture.LayoutFormat.resolve(
                key = null,
                fallback = NativePictureInPicture.LayoutFormat.MediaCard,
            ),
        )
    }

    @Test
    fun `compact format stays square when height is not provided`() {
        val widthResId = 123

        val resolvedWidthResId = NativePictureInPicture.LayoutFormat.Compact
            .resolveWidthResId(widthResId)
        val heightResId = NativePictureInPicture.LayoutFormat.Compact
            .resolveHeightResId(widthResId, null)

        assertEquals(widthResId, resolvedWidthResId)
        assertEquals(widthResId, heightResId)
    }

    @Test
    fun `media card uses square default and honors explicit height`() {
        assertEquals(
            DimenR.dimen._208dp,
            NativePictureInPicture.LayoutFormat.MediaCard.resolveWidthResId(
                sizeResId = DimenR.dimen._180dp,
            ),
        )
        assertEquals(
            DimenR.dimen._208dp,
            NativePictureInPicture.LayoutFormat.MediaCard.resolveHeightResId(
                sizeResId = DimenR.dimen._180dp,
                heightResId = null,
            ),
        )
        assertEquals(
            DimenR.dimen._220dp,
            NativePictureInPicture.LayoutFormat.MediaCard.resolveHeightResId(
                sizeResId = DimenR.dimen._180dp,
                heightResId = DimenR.dimen._220dp,
            ),
        )
    }

    @Test
    fun `media card enforces minimum media width and height`() {
        val mediaCard = NativePictureInPicture.LayoutFormat.MediaCard
        val minimumMediaSize = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(projectFile("src/main/res/values/dimen.xml"))
            .getElementsByTagName("dimen")
            .asElements()
            .single { it.getAttribute("name") == "native_picture_in_picture_media_min_size" }

        assertEquals("@dimen/_120dp", minimumMediaSize.textContent.trim())
        assertEquals(
            123,
            mediaCard.coerceWidthPx(
                requestedWidthPx = 100,
                minimumMediaSizePx = 120,
                mediaHorizontalMarginsPx = 3,
            ),
        )
        assertEquals(
            209,
            mediaCard.coerceHeightPx(
                requestedHeightPx = 190,
                minimumMediaSizePx = 120,
                nonMediaContentHeightPx = 89,
            ),
        )
        assertEquals(
            240,
            mediaCard.coerceHeightPx(
                requestedHeightPx = 240,
                minimumMediaSizePx = 120,
                nonMediaContentHeightPx = 86,
            ),
        )
    }

    @Test
    fun `compact format does not apply media card minimums`() {
        val compact = NativePictureInPicture.LayoutFormat.Compact

        assertEquals(100, compact.coerceWidthPx(100, 120, 3))
        assertEquals(100, compact.coerceHeightPx(100, 120, 86))
    }

    @Test
    fun `compact and media card use separate layouts`() {
        val compactElements = parseLayout("gnt_picture_in_picture_template_view.xml")
        val mediaCardElements = parseLayout(
            "gnt_picture_in_picture_media_card_template_view.xml"
        )
        val compactElementsById = compactElements.associateBy { it.androidAttribute("id") }
        val mediaCardElementsById = mediaCardElements.associateBy {
            it.androidAttribute("id")
        }
        val mediaCardRoot = mediaCardElements.first()

        assertFalse(compactElementsById.containsKey("@+id/media_view"))
        assertEquals("@dimen/_208dp", mediaCardRoot.androidAttribute("layout_width"))
        assertEquals("@dimen/_208dp", mediaCardRoot.androidAttribute("layout_height"))
        assertEquals(
            1,
            compactElements.count { it.tagName == NATIVE_AD_VIEW_TAG },
        )
        assertEquals(
            1,
            mediaCardElements.count { it.tagName == NATIVE_AD_VIEW_TAG },
        )
        assertEquals(
            "@dimen/_120dp",
            mediaCardElementsById.getValue("@+id/media_view")
                .androidAttribute("layout_height"),
        )
        assertEquals(
            "@dimen/native_picture_in_picture_media_min_size",
            mediaCardElementsById.getValue("@+id/media_view")
                .androidAttribute("minHeight"),
        )
        assertEquals(
            "@drawable/bg_native_picture_in_picture_media_card_cta",
            mediaCardElementsById.getValue("@+id/layout_cta").androidAttribute("background"),
        )
        assertEquals(
            "@dimen/_3dp",
            mediaCardElementsById.getValue("@+id/layout_cta")
                .androidAttribute("layout_marginBottom"),
        )
        listOf(
            "@+id/media_view",
            "@+id/icon",
            "@+id/ad_notification_view",
            "@+id/primary",
            "@+id/cta",
            "@+id/close_button",
        ).forEach { requiredId ->
            mediaCardElementsById.getValue(requiredId)
        }
    }

    private fun parseLayout(layoutName: String): List<Element> =
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(projectFile("src/main/res/layout/$layoutName"))
            .getElementsByTagName("*")
            .asElements()

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
        const val NATIVE_AD_VIEW_TAG = "com.google.android.gms.ads.nativead.NativeAdView"
    }
}
