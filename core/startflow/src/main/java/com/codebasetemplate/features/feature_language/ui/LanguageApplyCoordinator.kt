package com.codebasetemplate.features.feature_language.ui

internal fun applyLanguageAndContinue(
    selectedLanguageCode: String,
    currentLanguageCode: String,
    changeLanguage: () -> Unit,
    continueAfterLanguageApplied: () -> Unit,
) {
    if (selectedLanguageCode != currentLanguageCode) {
        changeLanguage()
    }
    continueAfterLanguageApplied()
}
