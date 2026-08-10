package com.core.config.domain.data

data class StartFlowConfig(
    val splashScreenConfig: SplashScreenConfig,
    val languageActivityConfig: LanguageActivityConfig = LanguageActivityConfig(),
    val onBoardingConfig: OnBoardingConfig = OnBoardingConfig(),
) {
    val isLanguageV2: Boolean
        get() = languageActivityConfig.isV2

    val isOnBoardingV3: Boolean
        get() = onBoardingConfig.version == OnBoardingConfig.ONBOARDING_VERSION_3
}
