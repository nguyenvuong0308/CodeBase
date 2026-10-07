package com.core.config.domain.data


data class SplashScreenConfig(

    val maxTimeToWaitAppOpenAd: Long,

    val timeSkipAppOpenAdWhenNotAvailable: Long,

    val adTypeFirstOpen: AdType,

    val adType: AdType,

    val minTimeWaitProgressBeforeShowAd: Long,

    val isEnableRetry: Boolean,

    val maxRetryCount: Int,

    val retryFixedDelay: Long,

    val isLoadBeforeEuConsent: Boolean,

    // Ghi đè is_enable của action_app_open_first_open và open_app_first_open; null = dùng config ad place.
    val isEnableFirstOpen: Boolean? = null,

    // Ghi đè is_enable của action_app_open và open_app; null = dùng config ad place.
    val isEnableOpen: Boolean? = null,
    )