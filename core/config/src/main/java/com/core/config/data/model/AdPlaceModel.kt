package com.core.config.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AdPlaceModel(

    @Json(name = "place_name")
    val adPlace: String?,

    @Json(name = "ad_id")
    val adId: String?,

    // Optional higher-priority ad unit ids. They are tried before adId.
    @Json(name = "high_floor_ad_ids")
    val highFloorAdIds: List<String>?,

    @Json(name = "is_tutorial_flow")
    val isTutorialFlow: Boolean?,

    @Json(name = "ad_type")
    val adType: String?,

    @Json(name = "is_enable")
    val isEnable: Boolean?,

    @Json(name = "is_auto_load_after_dismiss")
    val isAutoLoadAfterDismiss: Boolean?,

    @Json(name = "is_ignore_interval")
    val isIgnoreInterval: Boolean?,

    // For BannerType (anchored or inline)
    @Json(name = "banner_size")
    val bannerSize: String?,

    @Json(name = "is_collapsible")
    val isCollapsible: Boolean?,

    @Json(name = "auto_reload_collapsible")
    val autoReloadCollapsible: Boolean?,

    // For NativeAd (small or medium)
    @Json(name = "native_template_size")
    val nativeTemplateSize: String?,

    // For NativeAd
    @Json(name = "expired_time_second")
    val expiredTimeSecond: Int?,

    @Json(name = "count_down_timer")
    val countDownTimer: Int?,

    @Json(name = "background_cta")
    val backgroundCta: String?,

    @Json(name = "cta_text_color")
    val ctaTextColor: String?,

    @Json(name = "cta_border_color")
    val ctaBorderColor: String?,

    @Json(name = "media_background_color")
    val mediaBackgroundColor: String?,

    @Json(name = "border_color")
    val borderColor: String?,

    @Json(name = "background_color")
    val backgroundColor: String?,

    @Json(name = "background_full_color")
    val backgroundFullColor: String?,

    @Json(name = "background_radius")
    val backgroundRadius: Int?,

    @Json(name = "primary_text_color")
    val primaryTextColor: String?,

    @Json(name = "body_text_color")
    val bodyTextColor: String?,

    // For AppOpenAD
    @Json(name = "limit_show")
    val limitShow: Int?,

    @Json(name = "is_tracking_click")
    val isTrackingClick: Boolean?,

    @Json(name = "is_tracking_show")
    val isTrackingShow: Boolean?,

    @Json(name = "cta_radius_dp")
    val ctaRadius: Int?,

    // For Fullscreen Native Ad
    @Json(name = "is_enable_fullscreen_immersive")
    val isEnableFullScreenImmersive: Boolean?,

    @Json(name = "plus_interval")
    val plusInterval: Int?,

    @Json(name = "hide_text_skip_count_down")
    val hideTextSkipCountDown: Boolean?,

    @Json(name = "hide_text_count_down")
    val hideTextCountDown: Boolean?,

    @Json(name = "hide_progress_count_down")
    val hideProgressCountDown: Boolean?,

    @Json(name = "progress_bar_tint")
    val progressBarTint: String?,

     @Json(name = "text_color_ads_notify_view")
    val textColorAdsNotifyView: String?,

    @Json(name = "background_color_ads_notify_view")
    val backgroundColorAdsNotifyView: String?,

)
