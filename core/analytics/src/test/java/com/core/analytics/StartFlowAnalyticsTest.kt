package com.core.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class StartFlowAnalyticsTest {
    private val events = mutableListOf<Pair<String, Map<String, String>>>()
    private val tracker = StartFlowAnalytics(object : AnalyticsManager {
        override fun trackScreen(screenName: String) = Unit
        override fun logEvent(eventName: String, params: Map<String, String>) {
            events += eventName to params
        }
        override fun logEvent(eventName: String) = logEvent(eventName, emptyMap())
    })

    @Test
    fun `first launch only logs splash view`() {
        tracker.splashViewed(isFirstOpen = true)
        assertEquals(listOf("Splash_view"), eventNames())
    }

    @Test
    fun `each reopen logs splash view and subsequent session view`() {
        repeat(2) { tracker.splashViewed(isFirstOpen = false) }
        assertEquals(
            listOf("Splash_view", "Splash_ss_view", "Splash_view", "Splash_ss_view"),
            eventNames(),
        )
    }

    @Test
    fun `language flow logs view selection and done in order`() {
        tracker.languageViewed(isFromSetting = false)
        tracker.languageSelected("en", isFromSetting = false)
        tracker.languageDone(isFromSetting = false)
        assertEquals(
            listOf(
                "language_01_view" to emptyMap(),
                "language_02_view" to mapOf("Language02" to "English"),
                "language_02_click_language" to emptyMap(),
                "language_02_click_done" to emptyMap(),
            ),
            events,
        )
    }

    @Test
    fun `every selection reports the chosen language including regional variants`() {
        listOf("en-US", "it-IT", "vi-VN", "en-GB").forEach {
            tracker.languageSelected(it, isFromSetting = false)
        }
        assertEquals(
            listOf("English", "Italy", "Vietnam", "English"),
            events.filter { it.first == "language_02_view" }.map { it.second["Language02"] },
        )
        assertEquals(4, eventNames().count { it == "language_02_click_language" })
    }

    @Test
    fun `other languages use stable English names regardless of app locale`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMAN)
            tracker.languageSelected("fr-FR", isFromSetting = false)
            assertEquals(mapOf("Language02" to "French"), events.first().second)
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `settings language changes do not count as startup funnel events`() {
        tracker.languageViewed(isFromSetting = true)
        tracker.languageSelected("vi", isFromSetting = true)
        tracker.languageDone(isFromSetting = true)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `empty selection does not emit a selection event`() {
        tracker.languageSelected("", isFromSetting = false)
        tracker.languageSelected("  ", isFromSetting = false)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `onboarding views match content screen numbers and include revisits`() {
        listOf(1, 2, 3, 2, 0, 4).forEach(tracker::onboardingViewed)
        assertEquals(
            listOf("Onboard_01_view", "Onboard_02_view", "Onboard_03_view", "Onboard_02_view"),
            eventNames(),
        )
    }

    @Test
    fun `only requested onboarding screens emit next click events`() {
        (0..4).forEach { tracker.onboardingNext(it, isClick = true) }
        assertEquals(listOf("Onboard_02_click_next", "Onboard_03_click_next"), eventNames())
    }

    @Test
    fun `swiping onboarding never emits next click events`() {
        (1..3).forEach { tracker.onboardingNext(it, isClick = false) }
        assertTrue(events.isEmpty())
    }

    private fun eventNames() = events.map { it.first }
}
