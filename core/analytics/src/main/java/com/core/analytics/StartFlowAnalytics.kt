package com.core.analytics

import java.util.Locale

/** Events from the Splash / Language / Onboarding tracking specification. */
class StartFlowAnalytics(private val analyticsManager: AnalyticsManager) {
    fun splashViewed(isFirstOpen: Boolean) {
        analyticsManager.logEvent("Splash_view")
        if (!isFirstOpen) analyticsManager.logEvent("Splash_ss_view")
    }

    fun languageViewed(isFromSetting: Boolean) {
        if (!isFromSetting) analyticsManager.logEvent("language_01_view")
    }

    fun languageSelected(languageTag: String, isFromSetting: Boolean) {
        if (isFromSetting || languageTag.isBlank()) return
        analyticsManager.logEvent(
            "language_02_view",
            mapOf("Language02" to languageValue(languageTag)),
        )
        analyticsManager.logEvent("language_02_click_language")
    }

    fun languageDone(isFromSetting: Boolean) {
        if (!isFromSetting) analyticsManager.logEvent("language_02_click_done")
    }

    fun onboardingViewed(screenNumber: Int) {
        val event = when (screenNumber) {
            1 -> "Onboard_01_view"
            2 -> "Onboard_02_view"
            3 -> "Onboard_03_view"
            else -> return
        }
        analyticsManager.logEvent(event)
    }

    fun onboardingNext(screenNumber: Int, isClick: Boolean) {
        if (!isClick) return
        val event = when (screenNumber) {
            2 -> "Onboard_02_click_next"
            3 -> "Onboard_03_click_next"
            else -> return
        }
        analyticsManager.logEvent(event)
    }

    private fun languageValue(languageTag: String): String {
        val locale = Locale.forLanguageTag(languageTag)
        return when (locale.language) {
            "en" -> "English"
            "it" -> "Italy"
            "vi" -> "Vietnam"
            else -> locale.getDisplayLanguage(Locale.ENGLISH).ifBlank { languageTag }
        }
    }
}
