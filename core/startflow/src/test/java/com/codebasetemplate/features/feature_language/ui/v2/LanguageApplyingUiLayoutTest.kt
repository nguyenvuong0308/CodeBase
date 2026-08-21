package com.codebasetemplate.features.feature_language.ui.v2

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.w3c.dom.Element

class LanguageApplyingUiLayoutTest {

    @Test
    fun `applying language uses a blocking full screen overlay`() {
        val document = parseLayout()
        val overlay = document.findElementByAndroidId("lnApplyLoading")

        assertNotNull("The applying-language overlay must exist", overlay)
        assertSame(
            "The overlay must cover the complete language screen",
            document.documentElement,
            overlay!!.parentNode
        )
        assertEquals("match_parent", overlay.androidAttribute("layout_width"))
        assertEquals("match_parent", overlay.androidAttribute("layout_height"))
        assertEquals("true", overlay.androidAttribute("clickable"))
        assertEquals("true", overlay.androidAttribute("focusable"))
        assertEquals(
            "@color/startflow_language_v2_applying_scrim",
            overlay.androidAttribute("background")
        )
        assertFalse(
            "The applying-language UI must not fall back to the old spinner",
            overlay.getElementsByTagName("ProgressBar").length > 0
        )
    }

    @Test
    fun `applying language shows translation icon and setup message`() {
        val document = parseLayout()
        val icon = document.findElementByAndroidId("applyingLanguageIcon")
        val message = document.findElementByAndroidId("applyingLanguageMessage")

        assertNotNull("The translation icon must exist", icon)
        assertNotNull("The applying-language message must exist", message)
        assertEquals(
            "@string/core_setting_up_language",
            message!!.androidAttribute("text")
        )
        assertEquals("center", message.androidAttribute("gravity"))
        assertEquals(
            "@color/startflow_language_v2_applying_text",
            message.androidAttribute("textColor")
        )
    }

    private fun parseLayout() = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder().parse(findLayout())

    private fun findLayout(): File {
        return listOf(
            File("core/startflow/src/main/res/layout/startflow_activity_language_v2.xml"),
            File("src/main/res/layout/startflow_activity_language_v2.xml")
        ).firstOrNull(File::isFile)
            ?: error("Cannot locate startflow_activity_language_v2.xml")
    }

    private fun org.w3c.dom.Document.findElementByAndroidId(id: String): Element? {
        val nodes = getElementsByTagName("*")
        return (0 until nodes.length)
            .mapNotNull { nodes.item(it) as? Element }
            .firstOrNull {
                it.androidAttribute("id") == "@+id/$id" ||
                    it.androidAttribute("id") == "@id/$id"
            }
    }

    private fun Element.androidAttribute(name: String): String =
        getAttributeNS(ANDROID_NAMESPACE, name)

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
