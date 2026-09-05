package com.codebasetemplate.features.feature_language.ui.v2

import androidx.recyclerview.widget.RecyclerView
import com.codebasetemplate.features.feature_language.ui.v2.adapter.LanguageV2Adapter
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class LanguageGroupClickGuideTest {

    @Test
    fun `v21 points click guide at the third language group`() {
        assertEquals(2, LanguageActivityV21.CLICK_GUIDE_GROUP_INDEX)
        assertEquals(2, LanguageActivityV21.resolveClickGuideGroupIndex(isEnabled = true))
        assertTrue(
            LanguageV2Adapter.shouldShowClickGuide(
                groupIndex = 2,
                clickGuideGroupIndex = LanguageActivityV21.CLICK_GUIDE_GROUP_INDEX
            )
        )
        assertFalse(
            LanguageV2Adapter.shouldShowClickGuide(
                groupIndex = 1,
                clickGuideGroupIndex = LanguageActivityV21.CLICK_GUIDE_GROUP_INDEX
            )
        )
    }

    @Test
    fun `v21 disables click guide when remote config is false`() {
        assertEquals(
            RecyclerView.NO_POSITION,
            LanguageActivityV21.resolveClickGuideGroupIndex(isEnabled = false)
        )
    }

    @Test
    fun `click guide stays hidden when a screen does not configure it`() {
        assertFalse(
            LanguageV2Adapter.shouldShowClickGuide(
                groupIndex = 2,
                clickGuideGroupIndex = RecyclerView.NO_POSITION
            )
        )
    }

    @Test
    fun `group layout contains non blocking click lottie`() {
        val clickGuide = parseLayout().findElementByAndroidId("languageGroupClickGuide")

        assertNotNull("The language group click guide must exist", clickGuide)
        assertEquals("@raw/click", clickGuide!!.appAttribute("lottie_rawRes"))
        assertEquals("true", clickGuide.appAttribute("lottie_loop"))
        assertEquals("gone", clickGuide.androidAttribute("visibility"))
        assertEquals("false", clickGuide.androidAttribute("clickable"))
        assertEquals("false", clickGuide.androidAttribute("focusable"))
    }

    private fun parseLayout() = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder().parse(findLayout())

    private fun findLayout(): File {
        return listOf(
            File("core/startflow/src/main/res/layout/startflow_item_language_group_v2.xml"),
            File("src/main/res/layout/startflow_item_language_group_v2.xml")
        ).firstOrNull(File::isFile)
            ?: error("Cannot locate startflow_item_language_group_v2.xml")
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

    private fun Element.appAttribute(name: String): String =
        getAttributeNS(APP_NAMESPACE, name)

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val APP_NAMESPACE = "http://schemas.android.com/apk/res-auto"
    }
}
