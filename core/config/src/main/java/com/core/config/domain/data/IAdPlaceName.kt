package com.core.config.domain.data

interface IAdPlaceName {
    val name: String
}

// Dùng cho native_config lồng trong interstitial khi place_name chỉ tồn tại trên Firebase.
data class RemoteAdPlaceName(
    override val name: String
) : IAdPlaceName

sealed class CoreAdPlaceName(
    override val name: String
) : IAdPlaceName {
    object NONE : CoreAdPlaceName("")
    object APP_REOPEN : CoreAdPlaceName("reopen_app")
    object FULLSCREEN_BACK_LANGUAGE_SETTING : CoreAdPlaceName("fullscreen_back_language_setting")
    object ACTION_OPEN_APP_FIRST_OPEN : CoreAdPlaceName("action_app_open_first_open")
    object ACTION_OPEN_APP : CoreAdPlaceName("action_app_open")
    object APP_OPEN_FIRST_OPEN : CoreAdPlaceName("open_app_first_open")
    object APP_OPEN : CoreAdPlaceName("open_app")

    object ACTION_NEXT_IN_INTRODUCTION : CoreAdPlaceName("action_next_in_introduction")
    object ACTION_SKIP_IN_INTRODUCTION : CoreAdPlaceName("action_skip_in_introduction")
    object ANCHORED_ONBOARDING_BOTTOM : CoreAdPlaceName("anchored_onboarding_bottom")
    object ANCHORED_ONBOARDING_BOTTOM_V2 : CoreAdPlaceName("anchored_onboarding_bottom_v2")
    object ANCHORED_FULL_ONBOARDING : CoreAdPlaceName("anchored_full_onboarding")
    object ANCHORED_FULL_ONBOARDING_V2 : CoreAdPlaceName("anchored_full_onboarding_v2")
    object ANCHORED_CHANGE_LANGUAGE_V1_STEP_1 : CoreAdPlaceName("flow_tutorial_v1_201_language_1_n_native")
    object ANCHORED_CHANGE_LANGUAGE_V1_STEP_2 : CoreAdPlaceName("flow_tutorial_v1_201_language_2_n_native")

    object ANCHORED_BOTTOM_SPLASH : CoreAdPlaceName("flow_tutorial_101_splash")

    object ANCHORED_CHANGE_LANGUAGE_V2_FROM_SETTING_NATIVE :
        CoreAdPlaceName("anchored_change_language_v2_from_setting_native")
    object ANCHORED_CHANGE_LANGUAGE_V2_NATIVE_1 :
        CoreAdPlaceName("flow_tutorial_201_language_1_n_native")
    object ANCHORED_CHANGE_LANGUAGE_V2_NATIVE_2 :
        CoreAdPlaceName("flow_tutorial_201_language_2_n_native")
    object ANCHORED_CHANGE_LANGUAGE_V2_NATIVE_3 :
        CoreAdPlaceName("flow_tutorial_201_language_3_n_native")
    object ANCHORED_ONBOARDING_BOTTOM_V3_1 :
        CoreAdPlaceName("flow_tutorial_301_onb1_n_native")
    object ANCHORED_ONBOARDING_BOTTOM_V3_2 :
        CoreAdPlaceName("flow_tutorial_302_onb2_n_native")
    object ANCHORED_ONBOARDING_BOTTOM_V3_3 :
        CoreAdPlaceName("flow_tutorial_303_onb3_n_native")
    object ANCHORED_ONBOARDING_BOTTOM_V3_4 :
        CoreAdPlaceName("flow_tutorial_303_onb4_n_native")
    object ANCHORED_ONBOARDING_BOTTOM_V3_5 :
        CoreAdPlaceName("flow_tutorial_303_onb5_n_native")

    companion object {
        val ALL: List<CoreAdPlaceName> by lazy {
            CoreAdPlaceName::class.sealedSubclasses.mapNotNull { it.objectInstance }
        }

        fun fromKey(key: String): CoreAdPlaceName {
            return ALL.find { it.name == key } ?: NONE
        }
    }
}
