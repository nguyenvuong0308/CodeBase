package com.core.config.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OnBoardingConfigModel(
    @Json(name = "version")
    val version: Int?,
    @Json(name = "position_next")
    val positionNext: String?,
    @Json(name = "is_show_close")
    val isShowClose: Boolean?,
    @Json(name = "is_show_swipe")
    val isShowSwipe: Boolean?,
    @Json(name = "delay_show_close_swipe_seconds")
    val delayShowCloseSwipeSeconds: Long?,
)
