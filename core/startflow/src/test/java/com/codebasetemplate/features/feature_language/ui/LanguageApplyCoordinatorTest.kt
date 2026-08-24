package com.codebasetemplate.features.feature_language.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageApplyCoordinatorTest {

    @Test
    fun `continues immediately when selected language is already applied`() {
        val events = mutableListOf<String>()

        applyLanguageAndContinue(
            selectedLanguageCode = "vi",
            currentLanguageCode = "vi",
            changeLanguage = { events += "change_language" },
            continueAfterLanguageApplied = { events += "navigate" },
        )

        assertEquals(listOf("navigate"), events)
    }

    @Test
    fun `changes language then continues without waiting for activity recreation`() {
        val events = mutableListOf<String>()

        applyLanguageAndContinue(
            selectedLanguageCode = "vi",
            currentLanguageCode = "en",
            changeLanguage = { events += "change_language" },
            continueAfterLanguageApplied = { events += "navigate" },
        )

        assertEquals(listOf("change_language", "navigate"), events)
    }
}
