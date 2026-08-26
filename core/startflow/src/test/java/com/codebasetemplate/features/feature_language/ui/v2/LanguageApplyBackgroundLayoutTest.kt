package com.codebasetemplate.features.feature_language.ui.v2

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.w3c.dom.Element

class LanguageApplyBackgroundLayoutTest {

    @Test
    fun `apply button uses an app overrideable drawable background`() {
        val layout = parseXml(findFile("src/main/res/layout/startflow_activity_language_v2.xml"))
        val applyButton = layout.findElementByAndroidId("languageApply")

        assertNotNull("The language Apply button must exist", applyButton)
        assertEquals(
            "@drawable/startflow_language_v2_apply_background",
            applyButton!!.androidAttribute("background")
        )

        val config = parseXml(findFile("src/main/res/values/startflow_language_config.xml"))
        val backgroundResource = config.getElementsByTagName("item")
            .asElementSequence()
            .firstOrNull { it.getAttribute("name") == "startflow_language_v2_apply_background" }

        assertNotNull("The Apply background drawable alias must be declared", backgroundResource)
        assertEquals("drawable", backgroundResource!!.getAttribute("type"))
    }

    private fun parseXml(file: File) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder().parse(file)

    private fun findFile(moduleRelativePath: String): File {
        return listOf(
            File("core/startflow/$moduleRelativePath"),
            File(moduleRelativePath)
        ).firstOrNull(File::isFile)
            ?: error("Cannot locate $moduleRelativePath")
    }

    private fun org.w3c.dom.Document.findElementByAndroidId(id: String): Element? =
        getElementsByTagName("*")
            .asElementSequence()
            .firstOrNull {
                it.androidAttribute("id") == "@+id/$id" ||
                    it.androidAttribute("id") == "@id/$id"
            }

    private fun org.w3c.dom.NodeList.asElementSequence(): Sequence<Element> =
        (0 until length).asSequence().mapNotNull { item(it) as? Element }

    private fun Element.androidAttribute(name: String): String =
        getAttributeNS(ANDROID_NAMESPACE, name)

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
