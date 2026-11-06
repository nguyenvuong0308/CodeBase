package com.codebasetemplate.features.feature_language.ui

internal data class LanguageV1ActionState(
    val showIcon: Boolean,
    val showText: Boolean,
    val canConfirm: Boolean,
) {
    val enableIcon: Boolean get() = showIcon && canConfirm
    val enableText: Boolean get() = showText && canConfirm
}

internal fun resolveLanguageV1ActionState(
    useText: Boolean,
    ready: Boolean,
    hasSelectedLanguage: Boolean,
) =
    LanguageV1ActionState(
        showIcon = ready && !useText,
        showText = ready && useText,
        canConfirm = ready && hasSelectedLanguage,
    )
