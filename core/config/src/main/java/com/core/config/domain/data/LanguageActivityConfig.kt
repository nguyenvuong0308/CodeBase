package com.core.config.domain.data

import com.core.config.data.model.LanguageActivityConfigModel

data class LanguageActivityConfig(
    val version: Int = LANGUAGE_ACTIVITY_VERSION_2,
    val timeShowLoadingLfo: Int? = null,
    val isShowClickGuide: Boolean = true,
    val v1ConfirmDelayMs: Long = DEFAULT_V1_CONFIRM_DELAY_MS,
    val v1ActionUseText: Boolean = false,
) {
    val isV2: Boolean
        get() = version == LANGUAGE_ACTIVITY_VERSION_2

    companion object {
        const val DEFAULT_V1_CONFIRM_DELAY_MS = 3_000L
        const val LANGUAGE_ACTIVITY_VERSION_1 = 1
        const val LANGUAGE_ACTIVITY_VERSION_2 = 2

        fun from(model: LanguageActivityConfigModel?): LanguageActivityConfig {
            val version = when (model?.version) {
                LANGUAGE_ACTIVITY_VERSION_1 -> LANGUAGE_ACTIVITY_VERSION_1
                LANGUAGE_ACTIVITY_VERSION_2 -> LANGUAGE_ACTIVITY_VERSION_2
                else -> LANGUAGE_ACTIVITY_VERSION_2
            }
            return LanguageActivityConfig(
                version = version,
                timeShowLoadingLfo = model?.timeShowLoadingLfo,
                isShowClickGuide = model?.isShowClickGuide ?: true,
                v1ConfirmDelayMs = model?.v1ConfirmDelayMs?.takeIf { it >= 0 }
                    ?: DEFAULT_V1_CONFIRM_DELAY_MS,
                v1ActionUseText = model?.v1ActionUseText ?: false,
            )
        }
    }
}
