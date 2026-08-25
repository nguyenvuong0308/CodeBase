package com.codebasetemplate.features.feature_onboarding.ui.v2

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.w3c.dom.Element

class OnBoardingV2LayoutBindingTest {

    @Test
    fun nextButtonBindingUsesTextViewBaseTypeForAppOverrides() {
        val tvNext = parseLayout().findElementByAndroidId("tvNext")

        assertNotNull("The V2 next button must exist", tvNext)
        assertEquals(
            "com.codebasetemplate.features.feature_onboarding.ui.OutlineTextView",
            tvNext!!.tagName
        )
        assertEquals(
            "android.widget.TextView",
            tvNext.toolsAttribute("viewBindingType")
        )
    }

    private fun parseLayout() = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder().parse(findLayout())

    private fun findLayout(): File {
        return listOf(
            File("core/startflow/src/main/res/layout/startflow_fragment_onboarding_v2.xml"),
            File("src/main/res/layout/startflow_fragment_onboarding_v2.xml")
        ).firstOrNull(File::isFile)
            ?: error("Cannot locate startflow_fragment_onboarding_v2.xml")
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

    private fun Element.toolsAttribute(name: String): String =
        getAttributeNS(TOOLS_NAMESPACE, name)

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val TOOLS_NAMESPACE = "http://schemas.android.com/tools"
    }
}