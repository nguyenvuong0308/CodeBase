package com.codebasetemplate.required.startflow

import androidx.fragment.app.Fragment
import com.core.config.domain.data.OnBoardingConfig
import com.core.startflow.databinding.CoreFragmentOnboardingBinding
import com.core.startflow.databinding.CoreFragmentOnboardingV2Binding
import com.core.startflow.databinding.FragmentOnboardingV3Binding
import com.core.startflow.databinding.FragmentOnboardingV3EndTabBinding
import com.core.startflow.onboarding.OnBoardingUiCustomizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppOnBoardingUiCustomizer @Inject constructor() : OnBoardingUiCustomizer {
    override fun customizeOnBoardingV1(
        fragment: Fragment,
        binding: CoreFragmentOnboardingBinding,
        position: Int,
        isLastPage: Boolean
    ) {
    }

    override fun customizeOnBoardingV2(
        fragment: Fragment,
        binding: CoreFragmentOnboardingV2Binding,
        position: Int,
        isLastPage: Boolean
    ) {
    }

    override fun customizeOnBoardingV3(
        fragment: Fragment,
        binding: FragmentOnboardingV3Binding,
        introductionPosition: Int,
        realPosition: Int,
        isPageEnd: Boolean,
        isShowAd: Boolean,
        onBoardingConfig: OnBoardingConfig
    ) {
    }

    override fun customizeOnBoardingV3EndTab(
        fragment: Fragment,
        binding: FragmentOnboardingV3EndTabBinding,
        position: Int
    ) {
    }
}
