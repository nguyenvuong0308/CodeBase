package com.core.config.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AdPlacesVersionConfigModel(
    @Json(name = "version_code")
    val versionCodes: List<Long>?,
    @Json(name = "key")
    val key: AdPlacesVersionKeyModel?,
)

@JsonClass(generateAdapter = true)
internal data class AdPlacesVersionKeyModel(
    @Json(name = "banner_native")
    val bannerNative: String?,
    @Json(name = "app_open")
    val appOpen: String?,
    @Json(name = "reward_inter")
    val rewardInter: String?,
)
