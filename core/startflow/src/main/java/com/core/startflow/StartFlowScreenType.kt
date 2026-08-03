package com.core.startflow

import com.core.baseui.fragment.ScreenType

sealed class StartFlowScreenType(override val screenName: String) : ScreenType {
    object Uninstall : StartFlowScreenType("Uninstall")
    object OnBoarding : StartFlowScreenType("OnBoarding")
    object Main : StartFlowScreenType("Main")
    object Screen1 : StartFlowScreenType("Screen1")
    object Screen2 : StartFlowScreenType("Screen2")
    object None : StartFlowScreenType("")
}
