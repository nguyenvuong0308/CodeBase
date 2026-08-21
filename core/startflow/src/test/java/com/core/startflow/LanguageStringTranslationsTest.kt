package com.core.startflow

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class LanguageStringTranslationsTest {

    @Test
    fun `setting up language is translated for every locale`() {
        val resourceDirectory = findResourceDirectory()
        val defaultText = readString(
            File(resourceDirectory, "values/strings.xml"),
            SETTING_UP_LANGUAGE_KEY
        )
        val localeDirectories = resourceDirectory.listFiles { file ->
            file.isDirectory && file.name.startsWith("values-")
        }.orEmpty().sortedBy { it.name }

        assertTrue("Expected localized values directories", localeDirectories.isNotEmpty())
        localeDirectories.forEach { localeDirectory ->
            val stringsFile = File(localeDirectory, "strings.xml")
            val localizedText = readString(stringsFile, SETTING_UP_LANGUAGE_KEY)

            assertTrue(
                "${localeDirectory.name} must provide a non-blank translation",
                localizedText.isNotBlank()
            )
            assertFalse(
                "${localeDirectory.name} must not fall back to the English text",
                localizedText == defaultText
            )
            assertTrue(
                "${localeDirectory.name} must preserve the intended line break",
                localizedText.contains("\\n")
            )
        }
    }

    private fun readString(stringsFile: File, name: String): String {
        assertTrue("Missing resource file: $stringsFile", stringsFile.isFile)
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(stringsFile)
        val matchingResources = document.getElementsByTagName("string")
            .let { nodes ->
                (0 until nodes.length)
                    .mapNotNull { nodes.item(it) as? Element }
                    .filter { it.getAttribute("name") == name }
            }

        assertEquals("$stringsFile must define $name exactly once", 1, matchingResources.size)
        return matchingResources.single().textContent
    }

    private fun findResourceDirectory(): File = listOf(
        File("core/startflow/src/main/res"),
        File("src/main/res")
    ).firstOrNull(File::isDirectory)
        ?: error("Cannot locate core/startflow resource directory")

    private companion object {
        const val SETTING_UP_LANGUAGE_KEY = "core_setting_up_language"
    }
}
