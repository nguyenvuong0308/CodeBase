package com.codebasetemplate.features.feature_onboarding.ui.v1

import com.core.baseui.navigator.NavigatorEvent

sealed class OnBoardingEvent: NavigatorEvent {
    object BackEvent: OnBoardingEvent()
    object NextEvent: OnBoardingEvent()
    object FinishStep: OnBoardingEvent()
    data class NextAction(val actionMethod: String) : OnBoardingEvent()
    data class FinishAction(val actionMethod: String) : OnBoardingEvent()
}
