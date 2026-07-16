package com.codebasetemplate.required.ads

import android.util.Log
import com.core.config.domain.data.IAdPlaceName

private const val TAG = "AppAdPlaceName"
sealed class AppAdPlaceName(override val name: String): IAdPlaceName {

    /**Tạo mới cần add thêm vào list APP_AD_PLACE_LIST bên dưới*/

    object ANCHORED_NATIVE_TEST : AppAdPlaceName("anchored_native_test")
    object ANCHORED_BOTTOM_HOME : AppAdPlaceName("anchored_bottom_home")
    object ANCHORED_NATIVE_IN_LIST_TEST : AppAdPlaceName("anchored_native_in_list_test")
    object ANCHORED_BANNER_TEST : AppAdPlaceName("anchored_banner_test")
    object ANCHORED_EXIT : AppAdPlaceName("anchored_exit")
    object FULLSCREEN_TEST : AppAdPlaceName("fullscreen_test")
    object FULLSCREEN_TEST_LAZY_LOAD : AppAdPlaceName("fullscreen_test_lazy_load")
    object REWARD_TEST : AppAdPlaceName("reward_test")
    object FULLSCREEN_BACK_LANGUAGE_SETTING : AppAdPlaceName("fullscreen_back_language_setting")

    object ACTION_NEXT_IN_INTRODUCTION : AppAdPlaceName("action_next_in_introduction")
    object ACTION_SKIP_IN_INTRODUCTION : AppAdPlaceName("action_skip_in_introduction")

    object ANCHORED_ONBOARDING_BOTTOM : AppAdPlaceName("anchored_onboarding_bottom")
    object ANCHORED_ONBOARDING_BOTTOM_v2 : AppAdPlaceName("anchored_onboarding_bottom_v2")
    object ANCHORED_FULL_ONBOARDING : AppAdPlaceName("anchored_full_onboarding")
    object ANCHORED_FULL_ONBOARDING_v2 : AppAdPlaceName("anchored_full_onboarding_v2")

    object ANCHORED_CHANGE_LANGUAGE_BOTTOM : AppAdPlaceName("anchored_change_language_bottom")
    object ANCHORED_CHANGE_LANGUAGE_V2_FROM_SETTING_NATIVE : AppAdPlaceName("anchored_change_language_v2_from_setting_native")
 
    object ANCHORED_UNINSTALL_BOTTOM_STEP_1 : AppAdPlaceName("anchored_uninstall_bottom_step_1")
    object ANCHORED_UNINSTALL_BOTTOM_STEP_2 : AppAdPlaceName("anchored_uninstall_bottom_step_2")


    object ANCHORED_BOTTOM_SPLASH: AppAdPlaceName("flow_tutorial_101_splash")
    object ANCHORED_CHANGE_LANGUAGE_V2_NATIVE_1 : AppAdPlaceName("flow_tutorial_201_language_1_n_native")
    object ANCHORED_CHANGE_LANGUAGE_V2_NATIVE_2 : AppAdPlaceName("flow_tutorial_201_language_2_n_native")
    object ANCHORED_CHANGE_LANGUAGE_V2_NATIVE_3 : AppAdPlaceName("flow_tutorial_201_language_3_n_native")
    object ANCHORED_ONBOARDING_BOTTOM_V3_1 : AppAdPlaceName("flow_tutorial_301_onb1_n_native")
    object ANCHORED_ONBOARDING_BOTTOM_V3_2 : AppAdPlaceName("flow_tutorial_302_onb2_n_native")
    object ANCHORED_ONBOARDING_BOTTOM_V3_3 : AppAdPlaceName("flow_tutorial_303_onb3_n_native")
    object ANCHORED_ONBOARDING_BOTTOM_V3_4 : AppAdPlaceName("flow_tutorial_303_onb4_n_native")
    object ANCHORED_ONBOARDING_BOTTOM_V3_5 : AppAdPlaceName("flow_tutorial_303_onb5_n_native")

    object ACTION_OPEN_APP_FIRST_OPEN : AppAdPlaceName("action_app_open_first_open")
    object ACTION_OPEN_APP : AppAdPlaceName("action_app_open")
    object APP_OPEN_FIRST_OPEN : AppAdPlaceName("open_app_first_open")
    object APP_OPEN : AppAdPlaceName("open_app")

    companion object {
        /**Cần add thêm vào đây nếu tạo thêm AdPlaceName*/
        val APP_AD_PLACE_LIST: List<AppAdPlaceName> by lazy {
            AppAdPlaceName::class.sealedSubclasses.mapNotNull { it.objectInstance }
        }
        // Hàm lấy ad theo key string
        fun fromKey(key: String): AppAdPlaceName? {
            Log.d(TAG, "fromKey: ${APP_AD_PLACE_LIST}")

            return APP_AD_PLACE_LIST.find {
                it.name == key
            }
        }
    }
}
