package com.codebasetemplate.features.feature_language.ui

internal data class LanguageV1ActionState(
    val showIcon: Boolean,
    val showText: Boolean,
) {
    val canConfirm: Boolean get() = showIcon || showText
}

internal fun resolveLanguageV1ActionState(useText: Boolean, ready: Boolean) =
    LanguageV1ActionState(
        showIcon = ready && !useText,
        showText = ready && useText,
    )
