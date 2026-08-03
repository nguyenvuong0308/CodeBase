package com.codebasetemplate.features.feature_language.ui.v2.adapter

import com.core.baseui.supportedlanguage.SupportedLanguage

data class LanguageGroup(
    val id: String,
    val title: String,
    val nativeName: String,
    val language: SupportedLanguage,
    val options: List<LanguageOption>,
)

data class LanguageOption(
    val id: String,
    val title: String,
    val languageTag: String,
    val countryCode: String,
    val language: SupportedLanguage,
)

sealed class LanguageRow {
    data class Group(
        val group: LanguageGroup,
        val isExpanded: Boolean,
        val isSelected: Boolean,
    ) : LanguageRow()

    data class Option(
        val groupId: String,
        val option: LanguageOption,
        val isFirst: Boolean,
        val isLast: Boolean,
        val isSelected: Boolean,
    ) : LanguageRow()
}
