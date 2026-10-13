package com.codebasetemplate.features.feature_language.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.w3c.dom.Element

class LanguageV1SelectionLayoutTest {
    @Test
    fun `both confirmation modes start hidden with localized text and check icon available`() {
        val toolbar = element("startflow_activity_language.xml", "toolbar")
        assertEquals("@style/LanguageToolbarStyleV1", toolbar.getAttribute("style"))
        val file = listOf(
            File("src/main/res/values/startflow_language_config.xml"),
            File("core/startflow/src/main/res/values/startflow_language_config.xml"),
        ).first { it.isFile }
        val styles = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            .getElementsByTagName("style")
        val style = (0 until styles.length).map { styles.item(it) as Element }
            .first { it.getAttribute("name") == "LanguageToolbarStyleV1" }
        val items = style.getElementsByTagName("item")
        val values = (0 until items.length).map { items.item(it) as Element }
            .associate { it.getAttribute("name") to it.textContent.trim() }
        val expected = mapOf(
            "ctv_ic_back_show" to "false",
            "ctv_tv_action_show" to "false",
            "ctv_tv_action_enable" to "false",
            "ctv_tv_action_text" to "@string/core_common_save",
            "ctv_tv_action_text_color" to "@color/startflow_language_v1_toolbar_action",
            "ctv_ic_action_show" to "false",
            "ctv_ic_action_enable" to "false",
            "ctv_ic_action_icon" to "@drawable/ic_language_v1_check",
        )
        expected.forEach { (name, value) ->
            assertEquals(name, value, values[name])
            assertFalse("Layout must allow style overrides for $name", toolbar.hasAttribute("app:$name"))
        }
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
