package com.codebasetemplate.required.startflow

import android.content.Context
import com.core.startflow.onboarding.v1.OnBoardingV1PageState
import com.core.startflow.onboarding.v1.OnBoardingV1UiCustomizer
import com.core.startflow.onboarding.v1.OnBoardingV1UiSpec
import javax.inject.Inject
import javax.inject.Singleton

/** App-level branding for the core V1 layout. */
@Singleton
class AppOnBoardingV1UiCustomizer @Inject constructor() : OnBoardingV1UiCustomizer {
    override fun customize(
        context: Context,
        state: OnBoardingV1PageState,
        current: OnBoardingV1UiSpec,
    ): OnBoardingV1UiSpec = current
}
