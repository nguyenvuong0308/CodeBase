package com.codebasetemplate.features.feature_language.ui

internal enum class LanguageBannerStep {
    STEP_1,
    STEP_2,
}

internal enum class LanguageBannerAdState {
    PENDING,
    AVAILABLE,
    UNAVAILABLE,
}

internal fun resolveLanguageBannerStep(
    hasUserSelectedLanguage: Boolean,
    step1AdState: LanguageBannerAdState,
    step2AdState: LanguageBannerAdState,
): LanguageBannerStep? {
    val preferredStep = if (hasUserSelectedLanguage) {
        LanguageBannerStep.STEP_2
    } else {
        LanguageBannerStep.STEP_1
    }
    val fallbackStep = if (preferredStep == LanguageBannerStep.STEP_1) {
        LanguageBannerStep.STEP_2
    } else {
        LanguageBannerStep.STEP_1
    }

    return when {
        preferredStep.adState(step1AdState, step2AdState) != LanguageBannerAdState.UNAVAILABLE -> preferredStep
        fallbackStep.adState(step1AdState, step2AdState) != LanguageBannerAdState.UNAVAILABLE -> fallbackStep
        else -> null
    }
}

private fun LanguageBannerStep.adState(
    step1AdState: LanguageBannerAdState,
    step2AdState: LanguageBannerAdState,
): LanguageBannerAdState = when (this) {
    LanguageBannerStep.STEP_1 -> step1AdState
    LanguageBannerStep.STEP_2 -> step2AdState
}
