package com.codebasetemplate.required.firebase

data class LanguageActivityConfig(
    val version: Int = LANGUAGE_ACTIVITY_VERSION_2,
    val time_show_loading_lfo: Int? = null
) {
    val isV2: Boolean
        get() = version == LANGUAGE_ACTIVITY_VERSION_2

    companion object {
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
                time_show_loading_lfo = model?.time_show_loading_lfo
            )
        }
    }
}
