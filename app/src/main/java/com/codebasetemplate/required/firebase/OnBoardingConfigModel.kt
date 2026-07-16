package com.codebasetemplate.required.firebase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OnBoardingConfigModel (
    @Json(name = "version")
    var version: Int?,
    @Json(name = "position_next")
    var positionNext: String?,
    @Json(name = "is_show_close")
    var isShowClose: Boolean?,
    @Json(name = "is_show_swipe")
    var isShowSwipe: Boolean?,
    @Json(name = "delay_show_close_swipe_seconds")
    var delayShowCloseSwipeSeconds: Long?,
)
