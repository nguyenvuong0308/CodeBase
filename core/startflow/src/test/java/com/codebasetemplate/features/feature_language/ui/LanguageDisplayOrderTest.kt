package com.codebasetemplate.features.feature_language.ui

import com.core.baseui.supportedlanguage.SupportedLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageDisplayOrderTest {
    private val languages = listOf(
        SupportedLanguage.ENGLISH,
        SupportedLanguage.SPANISH,
        SupportedLanguage.PORTUGUESE,
        SupportedLanguage.FRENCH,
        SupportedLanguage.VIETNAMESE,
        SupportedLanguage.CHINA_SIMPLIFIED,
        SupportedLanguage.CHINA_TRADITIONAL,
        SupportedLanguage.ARABIC,
    )

    @Test
    fun `device language is third without duplicates or changes to other ordering`() {
        for (language in languages) {
            val result = orderLanguagesForDevice(languages, language.languageCode)
            assertEquals(language, result[2])
            assertEquals(languages.size, result.size)
            assertEquals(languages.filter { it != language }, result.filter { it != language })
        }
        assertEquals(SupportedLanguage.PORTUGUESE, languages[2])
    }

    @Test
    fun `regional device locale matches supported base language`() {
        for (tag in listOf("en-US", "en_GB", "EN-au")) {
            assertEquals(SupportedLanguage.ENGLISH, orderLanguagesForDevice(languages, tag)[2])
        }
    }

    @Test
    fun `traditional Chinese region and script take precedence over generic Chinese`() {
        for (tag in listOf("zh-TW", "zh-Hant", "zh-HK", "zh-Hant-TW")) {
            assertEquals(SupportedLanguage.CHINA_TRADITIONAL, orderLanguagesForDevice(languages, tag)[2])
        }
        assertEquals(SupportedLanguage.CHINA_SIMPLIFIED, orderLanguagesForDevice(languages, "zh-CN")[2])
    }

    @Test
    fun `unsupported or missing device locale preserves an available third row`() {
        for (tag in listOf("", "xx-YY", "de-DE")) {
            assertEquals(languages, orderLanguagesForDevice(languages, tag))
            assertEquals(SupportedLanguage.PORTUGUESE, orderLanguagesForDevice(languages, tag)[LANGUAGE_GUIDE_INDEX])
        }
    }

    @Test
    fun `empty and short lists do not crash`() {
        assertTrue(orderLanguagesForDevice(emptyList(), "en").isEmpty())
        assertEquals(listOf(SupportedLanguage.ENGLISH), orderLanguagesForDevice(languages.take(1), "en"))
        assertEquals(listOf(SupportedLanguage.SPANISH, SupportedLanguage.ENGLISH), orderLanguagesForDevice(languages.take(2), "en"))
    }
}
