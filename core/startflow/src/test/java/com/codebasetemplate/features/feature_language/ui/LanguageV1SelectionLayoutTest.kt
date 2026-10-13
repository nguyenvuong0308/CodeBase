package com.codebasetemplate.features.feature_language.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element

class LanguageV1SelectionLayoutTest {
    @Test
    fun `both confirmation modes start hidden with localized text and check icon available`() {
        val toolbar = element("startflow_activity_language.xml", "toolbar")
        assertEquals("false", toolbar.getAttribute("app:ctv_tv_action_show"))
        assertEquals("false", toolbar.getAttribute("app:ctv_tv_action_enable"))
        assertEquals("@string/core_common_save", toolbar.getAttribute("app:ctv_tv_action_text"))
        assertEquals("@color/startflow_language_v1_toolbar_action", toolbar.getAttribute("app:ctv_tv_action_text_color"))
        assertEquals("false", toolbar.getAttribute("app:ctv_ic_action_show"))
        assertEquals("false", toolbar.getAttribute("app:ctv_ic_action_enable"))
        assertEquals("@drawable/ic_language_v1_check", toolbar.getAttribute("app:ctv_ic_action_icon"))
    }

    @Test
    fun `both row directions have a looping non blocking hand animation`() {
        for (layout in listOf("startflow_item_language_ltr.xml", "startflow_item_language_rtl.xml")) {
            val guide = element(layout, "lottieView")
            assertEquals("@raw/click", guide.getAttribute("app:lottie_rawRes"))
            assertEquals("true", guide.getAttribute("app:lottie_loop"))
            assertEquals("gone", guide.getAttribute("android:visibility"))
            assertEquals("false", guide.getAttribute("android:clickable"))
            assertEquals("false", guide.getAttribute("android:focusable"))
        }
    }

    private fun element(layout: String, id: String): Element {
        val file = listOf(File("src/main/res/layout/$layout"), File("core/startflow/src/main/res/layout/$layout"))
            .first { it.isFile }
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("*")
        return (0 until nodes.length).map { nodes.item(it) as Element }
            .first { it.getAttribute("android:id") == "@+id/$id" }
    }
}
