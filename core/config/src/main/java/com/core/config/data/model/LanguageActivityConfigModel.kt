package com.core.config.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LanguageActivityConfigModel(
    @Json(name = "version")
    val version: Int?,
    @Json(name = "time_show_loading_lfo")
    val timeShowLoadingLfo: Int?,
    @Json(name = "is_show_click_guide")
    val isShowClickGuide: Boolean?,
    @Json(name = "v1_confirm_delay_ms")
    val v1ConfirmDelayMs: Long? = null,
    @Json(name = "v1_action_use_text")
    val v1ActionUseText: Boolean? = null,
)
