package com.core.config.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SplashScreenConfigModel(

    @Json(name = "max_time_to_wait_app_open_ad")
    val maxTimeToWaitAppOpenAd: Long?,

    @Json(name = "time_skip_app_open_ad_when_not_available")
    val timeSkipAppOpenAdWhenNotAvailable: Long?,

    @Json(name = "ad_type_first_open")
    val adTypeFirstOpen: String?,

    @Json(name = "ad_type")
    val adType: String?,

    @Json(name = "min_time_wait_progress_before_show_ad")
    val minTimeWaitProgressBeforeShowAd: Long?,

    @Json(name = "is_enable_retry")
    val isEnableRetry: Boolean?,

    @Json(name = "max_retry_count")
    val maxRetryCount: Int?,

    @Json(name = "retry_fixed_delay")
    val retryFixedDelay: Long?,

    @Json(name = "is_load_before_eu_consent")
    val isLoadBeforeEuConsent: Boolean?,

    @Json(name = "is_enable_first_open")
    val isEnableFirstOpen: Boolean? = null,

    @Json(name = "is_enable_open")
    val isEnableOpen: Boolean? = null,

    )