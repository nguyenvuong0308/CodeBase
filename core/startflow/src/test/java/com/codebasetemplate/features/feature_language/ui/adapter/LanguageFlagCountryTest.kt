package com.codebasetemplate.features.feature_language.ui.adapter

import com.codebasetemplate.features.feature_language.ui.v2.adapter.languageFlagResourceName
import com.core.baseui.supportedlanguage.SupportedLanguage
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class LanguageFlagCountryTest {

    @Test
    fun `every v1 language has a country flag drawable`() {
        val missing = V1_LANGUAGES.filter { language ->
            val countryCode = LanguageFlagCountry.of(language.languageCode)
            countryCode == null || !hasFlagDrawable(countryCode)
        }

        assertTrue("Missing flag for: ${missing.map { it.languageCode }}", missing.isEmpty())
    }

    @Test
    fun `maps languages to the same default countries as v2`() {
        assertEquals("US", LanguageFlagCountry.of(SupportedLanguage.ENGLISH.languageCode))
        assertEquals("BR", LanguageFlagCountry.of(SupportedLanguage.PORTUGUESE.languageCode))
        assertEquals("CN", LanguageFlagCountry.of(SupportedLanguage.CHINA_SIMPLIFIED.languageCode))
        assertEquals("TW", LanguageFlagCountry.of(SupportedLanguage.CHINA_TRADITIONAL.languageCode))
        assertEquals("IN", LanguageFlagCountry.of(SupportedLanguage.TAMIL.languageCode))
    }

    @Test
    fun `normalizes separator and case and returns null for unknown language`() {
        assertEquals("TW", LanguageFlagCountry.of("ZH_tw"))
        assertNull(LanguageFlagCountry.of("xx"))
        assertNull(LanguageFlagCountry.of(""))
    }

    @Test
    fun `item layouts contain a hidden flag view`() {
        listOf("startflow_item_language_ltr.xml", "startflow_item_language_rtl.xml").forEach { name ->
            val flag = parse(findResFile("layout/$name")).findElementByAndroidId("tvFlag")

            assertNotNull("$name must contain tvFlag", flag)
            assertEquals("gone", flag!!.androidAttribute("visibility"))
            assertEquals("@dimen/_32dp", flag.androidAttribute("layout_width"))
            assertEquals("@dimen/_32dp", flag.androidAttribute("layout_height"))
        }
    }

    @Test
    fun `flag toggle is enabled by default`() {
        val bools = parse(findResFile("values/startflow_language_config.xml"))
            .getElementsByTagName("bool")
        val toggle = (0 until bools.length)
            .map { bools.item(it) as Element }
            .firstOrNull { it.getAttribute("name") == "startflow_language_v1_show_flag" }

        assertNotNull("startflow_language_v1_show_flag must exist", toggle)
        assertEquals("true", toggle!!.textContent.trim())
    }

    private fun hasFlagDrawable(countryCode: String): Boolean {
        val resDir = findResFile("drawable").parentFile!!
        val fileName = languageFlagResourceName(countryCode)
        return resDir.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("drawable") }
            .any { dir -> dir.listFiles().orEmpty().any { it.nameWithoutExtension == fileName } }
    }

    private fun findResFile(path: String): File {
        return listOf(
            File("core/startflow/src/main/res/$path"),
            File("src/main/res/$path")
        ).firstOrNull(File::exists)
            ?: error("Cannot locate res/$path")
    }

    private fun parse(file: File) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder().parse(file)

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

        // Same list as LanguageActivity.displayFirstData.
        val V1_LANGUAGES = listOf(
            SupportedLanguage.ENGLISH,
            SupportedLanguage.SPANISH,
            SupportedLanguage.PORTUGUESE,
            SupportedLanguage.FRENCH,
            SupportedLanguage.GERMAN,
            SupportedLanguage.HINDI,
            SupportedLanguage.ITALIAN,
            SupportedLanguage.INDONESIAN,
            SupportedLanguage.VIETNAMESE,
            SupportedLanguage.TURKISH,
            SupportedLanguage.MALAY,
            SupportedLanguage.THAI,
            SupportedLanguage.BENGALI,
            SupportedLanguage.FINNISH,
            SupportedLanguage.JAPAN,
            SupportedLanguage.CATALAN,
            SupportedLanguage.ESTONIAN,
            SupportedLanguage.ICELANDIC,
            SupportedLanguage.LATVIAN,
            SupportedLanguage.LITHUANIAN,
            SupportedLanguage.FILIPINO,
            SupportedLanguage.KAZAKH,
            SupportedLanguage.KOREAN,
            SupportedLanguage.DUTCH,
            SupportedLanguage.POLISH,
            SupportedLanguage.GREEK,
            SupportedLanguage.BULGARIAN,
            SupportedLanguage.RUSSIAN,
            SupportedLanguage.CZECH,
            SupportedLanguage.DANMARK,
            SupportedLanguage.GUJARATI,
            SupportedLanguage.KANNADA,
            SupportedLanguage.MALAYALAM,
            SupportedLanguage.MARATHI,
            SupportedLanguage.BURMESE,
            SupportedLanguage.HUNGARIAN,
            SupportedLanguage.CROATIAN,
            SupportedLanguage.NORWEGIAN,
            SupportedLanguage.PUNJABI,
            SupportedLanguage.RUMANU,
            SupportedLanguage.SWEDISH,
            SupportedLanguage.SWAHILI,
            SupportedLanguage.SERBIAN,
            SupportedLanguage.SLOVAK,
            SupportedLanguage.UZBEK,
            SupportedLanguage.UKRAINA,
            SupportedLanguage.TAMIL,
            SupportedLanguage.TELUGU,
            SupportedLanguage.CHINA_SIMPLIFIED,
            SupportedLanguage.CHINA_TRADITIONAL,
            SupportedLanguage.ARABIC,
            SupportedLanguage.PERSIAN,
            SupportedLanguage.URDU,
            SupportedLanguage.YIDDISH,
        )
    }
}
