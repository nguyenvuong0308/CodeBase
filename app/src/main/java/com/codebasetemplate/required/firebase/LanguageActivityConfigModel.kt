package com.codebasetemplate.required.firebase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LanguageActivityConfigModel(
    @Json(name = "version")
    val version: Int?,
    @Json(name = "time_show_loading_lfo")
    val time_show_loading_lfo: Int?,
)
