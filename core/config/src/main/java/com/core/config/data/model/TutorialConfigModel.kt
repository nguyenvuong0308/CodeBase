package com.core.config.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class TutorialConfigModel(
    @Json(name = "enable_all_ads")
    val enableAllAds: Boolean?,
    @Json(name = "enable_ad_1")
    val enableAd1: Boolean?,
    @Json(name = "enable_ad_2")
    val enableAd2: Boolean?,
)
