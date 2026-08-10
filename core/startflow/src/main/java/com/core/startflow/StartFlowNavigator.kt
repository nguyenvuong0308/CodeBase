package com.core.startflow

import android.content.Context
import com.core.config.domain.data.IAdPlaceName

/**
 * Cầu nối điều hướng từ module core:startflow sang các màn hình cụ thể của app.
 *
 * Module core không biết class Activity thật của từng app, nên app module sẽ implement interface
 * này trong RequiredModule và cung cấp qua Hilt.
 */
interface StartFlowNavigator {
    /**
     * Trả về màn chính của app để Splash, Language hoặc Onboarding mở sau khi hoàn tất start flow.
     */
    fun mainClass(): Class<*>

    /**
     * Trả về màn đặc biệt cần mở sau khi chọn ngôn ngữ, nếu app có case riêng ngoài onboarding/main.
     */
    fun otherLanguageEnd(): Class<*>?

    /**
     * Trả về màn uninstall flow, dùng khi app được mở từ shortcut uninstall.
     */
    fun uninstallClass(): Class<*>? = null

    /**
     * Tạo hoặc tắt shortcut ngoài launcher theo cấu hình remote config.
     */
    fun setUpShortCut(context: Context, isEnable: Boolean, isEnableUninstall: Boolean) = Unit

    /**
     * Cung cấp danh sách vị trí quảng cáo cần preload trước khi vào màn main.
     */
    fun mainPreloadAdPlaceNames(): List<IAdPlaceName> = emptyList()

    /**
     * Cung cấp danh sách vị trí quảng cáo cần preload trước khi vào uninstall flow.
     */
    fun uninstallPreloadAdPlaceNames(): List<IAdPlaceName> = emptyList()
}
