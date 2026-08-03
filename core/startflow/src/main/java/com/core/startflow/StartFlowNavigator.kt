package com.core.startflow

import android.content.Context
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.OnBoardingConfig

interface StartFlowNavigator {
    fun mainClass(): Class<*>
    fun uninstallClass(): Class<*>? = null
    fun onBoardingClass(onBoardingConfig: OnBoardingConfig): Class<*>
    fun setUpShortCut(context: Context, isEnable: Boolean, isEnableUninstall: Boolean) = Unit
    fun mainPreloadAdPlaceNames(): List<IAdPlaceName> = emptyList()
    fun uninstallPreloadAdPlaceNames(): List<IAdPlaceName> = emptyList()
}
