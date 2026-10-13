package com.codebasetemplate.features.feature_language.ui

import com.core.baseui.supportedlanguage.SupportedLanguage
import java.util.Locale

internal const val LANGUAGE_GUIDE_INDEX = 2

/** Keeps the existing third row as the fallback when the device language is unsupported. */
internal fun orderLanguagesForDevice(
    languages: List<SupportedLanguage>,
    deviceLanguageTag: String,
): List<SupportedLanguage> {
    if (languages.isEmpty()) return emptyList()
    val device = Locale.forLanguageTag(deviceLanguageTag.replace('_', '-'))
    val exactMatch = languages.find {
        Locale.forLanguageTag(it.languageCode) == device
    }
    val traditionalChinese = device.language == "zh" &&
        (device.script == "Hant" || device.country in listOf("TW", "HK", "MO"))
    val deviceLanguage = exactMatch
        ?: languages.find { traditionalChinese && it.languageCode == "zh-TW" }
        ?: languages.find {
            device.language.isNotBlank() &&
                Locale.forLanguageTag(it.languageCode).language == device.language
        }
        ?: return languages.toList()
    return languages.toMutableList().apply {
        remove(deviceLanguage)
        add(LANGUAGE_GUIDE_INDEX.coerceAtMost(size), deviceLanguage)
    }
}
