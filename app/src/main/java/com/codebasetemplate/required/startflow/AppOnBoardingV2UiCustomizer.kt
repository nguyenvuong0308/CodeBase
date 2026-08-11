package com.codebasetemplate.required.startflow

import android.content.Context
import com.core.startflow.onboarding.v2.OnBoardingV2PageState
import com.core.startflow.onboarding.v2.OnBoardingV2UiCustomizer
import com.core.startflow.onboarding.v2.OnBoardingV2UiSpec
import javax.inject.Inject
import javax.inject.Singleton

/** App-level branding for the core V2 layout. */
@Singleton
class AppOnBoardingV2UiCustomizer @Inject constructor() : OnBoardingV2UiCustomizer {
    override fun customize(
        context: Context,
        state: OnBoardingV2PageState,
        current: OnBoardingV2UiSpec,
    ): OnBoardingV2UiSpec = current
}
