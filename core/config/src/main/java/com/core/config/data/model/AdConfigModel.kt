package com.core.config.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
internal class BannerAdConfigModel(
    @Json(name = "is_enable_retry")
    val isEnableRetry: Boolean?,
    @Json(name = "max_retry_count")
    val maxRetryCount: Int?,
    @Json(name = "is_hide_when_error")
    val isHideWhenError: Boolean?,
    @Json(name = "retry_interval_second_list")
    val retryIntervalSecondList: List<Long>?,
)

@JsonClass(generateAdapter = true)
internal class NativeAdConfigModel(
    @Json(name = "is_enable_retry")
    val isEnableRetry: Boolean?,
    @Json(name = "max_retry_count")
    val maxRetryCount: Int?,
    @Json(name = "is_hide_when_error")
    val isHideWhenError: Boolean?,
    @Json(name = "expired_time_second")
    val expiredTimeSecond: Int?,
    @Json(name = "retry_interval_second_list")
    val retryIntervalSecondList: List<Long>?,
)

@JsonClass(generateAdapter = true)
internal data class InterstitialAdConfigModel(
    @Json(name = "is_wait_load_to_show")
    val isWaitLoadToShow: Boolean?,
    @Json(name = "ads_per_session")
    val adsPerSession: Int?,
    @Json(name = "time_per_session")
    val timePerSession: Long?,
    @Json(name = "time_interval")
    val timeInterval: Long?,
    @Json(name = "time_interval_after_show_open_ad")
    val timeIntervalAfterShowOpenAd: Long?,

    @Json(name = "is_enable_retry")
    val isEnableRetry: Boolean?,
    @Json(name = "max_retry_count")
    val maxRetryCount: Int?,
    @Json(name = "retry_interval_second_list")
    val retryIntervalSecondList: List<Long>?,
)

@JsonClass(generateAdapter = true)
internal data class RewardedInterstitialAdConfigModel(
    @Json(name = "is_wait_load_to_show")
    val isWaitLoadToShow: Boolean?,

    @Json(name = "is_enable_retry")
    val isEnableRetry: Boolean?,
    @Json(name = "max_retry_count")
    val maxRetryCount: Int?,
    @Json(name = "retry_interval_second_list")
    val retryIntervalSecondList: List<Long>?,
)

@JsonClass(generateAdapter = true)
internal data class RewardedAdConfigModel(
    @Json(name = "is_wait_load_to_show")
    val isWaitLoadToShow: Boolean?,
    @Json(name = "is_enable_retry")
    val isEnableRetry: Boolean?,
    @Json(name = "max_retry_count")
    val maxRetryCount: Int?,
    @Json(name = "retry_interval_second_list")
    val retryIntervalSecondList: List<Long>?,
    @Json(name = "max_retry_on_context")
    val maxRetryOnContext: Int?,
    @Json(name = "time_wait_retry_on_context")
    val timeWaitRetryOnContext: Int?,
)

@JsonClass(generateAdapter = true)
internal data class AppOpenAdConfigModel(
    @Json(name = "time_millis_delay_before_show")
    val timeMillisDelayBeforeShow: Long?,
    @Json(name = "time_interval")
    val timeInterval: Long?,

    @Json(name = "is_enable_retry")
    val isEnableRetry: Boolean?,
    @Json(name = "reopen_mode")
    val reopenMode: String?,
    @Json(name = "max_retry_count")
    val maxRetryCount: Int?,
    @Json(name = "retry_interval_second_list")
    val retryIntervalSecondList: List<Long>?,
)
