package com.codebasetemplate.features.feature_language.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageApplyCoordinatorTest {

    @Test
    fun `continues immediately when selected language is already applied`() {
        val events = applyAndRecord(selected = "vi", current = "vi")

        assertEquals(listOf("navigate"), events)
    }

    @Test
    fun `treats underscore and case differences as the same applied language`() {
        val events = applyAndRecord(selected = "pt_br", current = "pt-BR")

        assertEquals(listOf("navigate"), events)
    }

    @Test
    fun `changes language then schedules fallback instead of navigating immediately`() {
        val events = applyAndRecord(selected = "vi", current = "en")

        assertEquals(listOf("change_language", "schedule_fallback"), events)
    }

    @Test
    fun `changes language when only the region differs`() {
        val events = applyAndRecord(selected = "en-GB", current = "en-US")

        assertEquals(listOf("change_language", "schedule_fallback"), events)
    }

    @Test
    fun `changes language when no language has been applied yet`() {
        val events = applyAndRecord(selected = "en", current = "")

        assertEquals(listOf("change_language", "schedule_fallback"), events)
    }

    @Test
    fun `language tags exactly match normalizes separators and case only`() {
        assertTrue(languageTagsExactlyMatch("zh_TW", "zh-tw"))
        assertFalse(languageTagsExactlyMatch("zh-TW", "zh-CN"))
        assertFalse(languageTagsExactlyMatch("en", "en-US"))
    }

    private fun applyAndRecord(selected: String, current: String): List<String> {
        val events = mutableListOf<String>()
        applyLanguageAndContinue(
            selectedLanguageCode = selected,
            currentLanguageCode = current,
            changeLanguage = { events += "change_language" },
            continueAfterLanguageApplied = { events += "navigate" },
            scheduleNavigationFallback = { events += "schedule_fallback" },
        )
        return events
    }
}
