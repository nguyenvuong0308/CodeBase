package com.codebasetemplate.features.feature_language.ui

/**
 * Same flow as language V2: navigate right away when the selected tag is already applied,
 * otherwise change the locale (activity is recreated and navigates in onResume) and schedule
 * a fallback navigation in case the recreation does not happen.
 */
internal fun applyLanguageAndContinue(
    selectedLanguageCode: String,
    currentLanguageCode: String,
    changeLanguage: () -> Unit,
    continueAfterLanguageApplied: () -> Unit,
    scheduleNavigationFallback: () -> Unit,
) {
    if (languageTagsExactlyMatch(selectedLanguageCode, currentLanguageCode)) {
        continueAfterLanguageApplied()
    } else {
        changeLanguage()
        scheduleNavigationFallback()
    }
}

internal fun languageTagsExactlyMatch(left: String, right: String): Boolean {
    return left.replace('_', '-').equals(right.replace('_', '-'), ignoreCase = true)
}
