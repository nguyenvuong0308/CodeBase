package com.codebasetemplate.features.feature_language.ui.adapter

import java.util.Locale

/**
 * Country shown as flag for each V1 language. Uses the same default countries as the
 * first option of each language group in V2.
 */
internal object LanguageFlagCountry {

    private val countryByLanguageCode = mapOf(
        "en" to "US",
        "es" to "ES",
        "pt" to "BR",
        "fr" to "FR",
        "de" to "DE",
        "hi" to "IN",
        "it" to "IT",
        "id" to "ID",
        "vi" to "VN",
        "tr" to "TR",
        "ms" to "MY",
        "th" to "TH",
        "bn" to "BD",
        "fi" to "FI",
        "ja" to "JP",
        "ca" to "ES",
        "et" to "EE",
        "is" to "IS",
        "lv" to "LV",
        "lt" to "LT",
        "fil" to "PH",
        "kk" to "KZ",
        "ko" to "KR",
        "nl" to "NL",
        "pl" to "PL",
        "el" to "GR",
        "bg" to "BG",
        "ru" to "RU",
        "cs" to "CZ",
        "da" to "DK",
        "gu" to "IN",
        "kn" to "IN",
        "ml" to "IN",
        "mr" to "IN",
        "my" to "MM",
        "hu" to "HU",
        "hr" to "HR",
        "no" to "NO",
        "pa" to "IN",
        "ro" to "RO",
        "sv" to "SE",
        "sw" to "KE",
        "sr" to "RS",
        "sk" to "SK",
        "uz" to "UZ",
        "uk" to "UA",
        "ta" to "IN",
        "te" to "IN",
        "zh" to "CN",
        "zh-tw" to "TW",
        "ar" to "SA",
        "fa" to "IR",
        "ur" to "PK",
        "he" to "IL",
    )

    fun of(languageCode: String): String? {
        return countryByLanguageCode[languageCode.replace('_', '-').lowercase(Locale.ROOT)]
    }
}
