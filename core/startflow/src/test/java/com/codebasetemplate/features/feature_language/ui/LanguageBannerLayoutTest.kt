package com.codebasetemplate.features.feature_language.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.w3c.dom.Element

class LanguageBannerLayoutTest {

    @Test
    fun `step 1 is visible and step 2 is gone initially`() {
        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(findLayout())

        val step1 = document.findElementByAndroidId("layout_banner_native_step1")
        val step2 = document.findElementByAndroidId("layout_banner_native_step2")

        assertNotNull("Step 1 banner container must exist", step1)
        assertNotNull("Step 2 banner container must exist", step2)
        assertEquals("", step1!!.androidAttribute("visibility"))
        assertEquals("gone", step2!!.androidAttribute("visibility"))
    }

    private fun findLayout(): File {
        return listOf(
            File("core/startflow/src/main/res/layout/startflow_activity_language.xml"),
            File("src/main/res/layout/startflow_activity_language.xml")
        ).firstOrNull(File::isFile)
            ?: error("Cannot locate startflow_activity_language.xml")
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
