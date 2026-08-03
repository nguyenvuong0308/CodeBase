package com.codebasetemplate.required.ads

import android.util.Log
import com.core.config.domain.data.IAdPlaceName

private const val TAG = "AppAdPlaceName"
sealed class AppAdPlaceName(override val name: String): IAdPlaceName {

    /**Tạo mới cần add thêm vào list APP_AD_PLACE_LIST bên dưới*/

    object ANCHORED_NATIVE_TEST : AppAdPlaceName("anchored_native_test")
    object ANCHORED_BOTTOM_HOME : AppAdPlaceName("anchored_bottom_home")
    object ANCHORED_NATIVE_PIP_HOME : AppAdPlaceName("anchored_native_pip_home")
    object ANCHORED_NATIVE_IN_LIST_TEST : AppAdPlaceName("anchored_native_in_list_test")
    object ANCHORED_BANNER_TEST : AppAdPlaceName("anchored_banner_test")
    object ANCHORED_EXIT : AppAdPlaceName("anchored_exit")
    object FULLSCREEN_TEST : AppAdPlaceName("fullscreen_test")
    object FULLSCREEN_TEST_LAZY_LOAD : AppAdPlaceName("fullscreen_test_lazy_load")
    object FULLSCREEN_NATIVE_INTERSTITIAL : AppAdPlaceName("fullscreen_native_interstitial")
    object REWARD_TEST : AppAdPlaceName("reward_test")
    object ANCHORED_UNINSTALL_BOTTOM_STEP_1 : AppAdPlaceName("anchored_uninstall_bottom_step_1")
    object ANCHORED_UNINSTALL_BOTTOM_STEP_2 : AppAdPlaceName("anchored_uninstall_bottom_step_2")

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
