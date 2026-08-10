package com.core.config.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LanguageActivityConfigModel(
    @Json(name = "version")
    val version: Int?,
    @Json(name = "time_show_loading_lfo")
    val timeShowLoadingLfo: Int?,
)
