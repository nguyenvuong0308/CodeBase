package com.codebasetemplate.features.feature_language.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageActivityManifestTest {

    @Test
    fun `handles locale and layout direction changes without activity recreation`() {
        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(findManifest())

        val activities = document.getElementsByTagName("activity")
        val targetActivity = (0 until activities.length)
            .map { activities.item(it) }
            .firstOrNull {
                it.attributes
                    .getNamedItemNS(ANDROID_NAMESPACE, "name")
                    ?.nodeValue == ACTIVITY_NAME
            }

        assertNotNull("LanguageActivity must be declared", targetActivity)

        val handledChanges = targetActivity!!
            .attributes
            .getNamedItemNS(ANDROID_NAMESPACE, "configChanges")
            ?.nodeValue
            .orEmpty()
            .split('|')
            .toSet()

        assertTrue("Activity must handle locale changes", "locale" in handledChanges)
        assertTrue(
            "Activity must handle layout direction changes for RTL locales",
            "layoutDirection" in handledChanges
        )
    }

    private fun findManifest(): File {
        return listOf(
            File("core/startflow/src/main/AndroidManifest.xml"),
            File("src/main/AndroidManifest.xml")
        ).firstOrNull(File::isFile)
            ?: error("Cannot locate core/startflow AndroidManifest.xml")
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val ACTIVITY_NAME =
            "com.codebasetemplate.features.feature_language.ui.LanguageActivity"
    }
}
