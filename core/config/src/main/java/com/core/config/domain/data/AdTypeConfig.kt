package com.core.config.domain.data

class BannerAdTypeConfig(
    val isEnableRetry: Boolean,
    val maxRetryCount: Int,
    val isHideWhenError: Boolean,
    val retryIntervalSecondList: List<Long>,
)

class NativeAdTypeConfig(
    val isEnableRetry: Boolean,
    val maxRetryCount: Int,
    val isHideWhenError: Boolean,
    val expiredTimeSecond: Int,
    val retryIntervalSecondList: List<Long>,
)

data class InterstitialAdTypeConfig(
    val isWaitLoadToShow: Boolean,
    val adsPerSession: Int,
    val timePerSession: Long,
    val timeInterval: Long,
    val timeIntervalAfterShowOpenAd: Long,
    val isEnableRetry: Boolean,
    val maxRetryCount: Int,
    val retryIntervalSecondList: List<Long>,
)

data class RewardedInterstitialAdTypeConfig(
    val isWaitLoadToShow: Boolean,
    val isEnableRetry: Boolean,
    val maxRetryCount: Int,
    val retryIntervalSecondList: List<Long>,
)

data class RewardedAdTypeConfig(
    val timeWaitRetryOnContext: Int,
    val maxRetryOnContext: Int,
    val isWaitLoadToShow: Boolean,
    val isEnableRetry: Boolean,
    val maxRetryCount: Int,
    val retryIntervalSecondList: List<Long>,
)

data class AppOpenAdTypeConfig(
    val timeMillisDelayBeforeShow: Long,
    val timeInterval: Long,
    val isEnableRetry: Boolean,
    val maxRetryCount: Int,
    val reopenMode: String,
    val retryIntervalSecondList: List<Long>,
) {
    companion object {
        const val REOPEN_MODE_CUSTOM_ACTIVITY = "custom_activity"
        const val REOPEN_MODE_APP_OPEN_AD = "app_open_ad"
    }

}
