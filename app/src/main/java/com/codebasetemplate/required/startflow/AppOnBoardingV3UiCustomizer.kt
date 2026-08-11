package com.codebasetemplate.required.startflow

import android.content.Context
import com.core.startflow.onboarding.v3.OnBoardingV3PageState
import com.core.startflow.onboarding.v3.OnBoardingV3UiCustomizer
import com.core.startflow.onboarding.v3.OnBoardingV3UiSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-level branding for the core V3 layout.
 * Return current.copy(...) to change only the properties owned by this app.
 */
@Singleton
class AppOnBoardingV3UiCustomizer @Inject constructor() : OnBoardingV3UiCustomizer {
    override fun customize(
        context: Context,
        state: OnBoardingV3PageState,
        current: OnBoardingV3UiSpec,
    ): OnBoardingV3UiSpec {
        return current
    }
}
