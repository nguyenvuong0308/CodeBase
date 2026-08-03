package com.core.startflow.onboarding

import androidx.fragment.app.Fragment
import com.core.config.domain.data.OnBoardingConfig
import com.core.startflow.databinding.CoreFragmentOnboardingBinding
import com.core.startflow.databinding.CoreFragmentOnboardingV2Binding
import com.core.startflow.databinding.FragmentOnboardingV3Binding
import com.core.startflow.databinding.FragmentOnboardingV3EndTabBinding

interface OnBoardingUiCustomizer {
    fun customizeOnBoardingV1(
        fragment: Fragment,
        binding: CoreFragmentOnboardingBinding,
        position: Int,
        isLastPage: Boolean
    ) = Unit

    fun customizeOnBoardingV2(
        fragment: Fragment,
        binding: CoreFragmentOnboardingV2Binding,
        position: Int,
        isLastPage: Boolean
    ) = Unit

    fun customizeOnBoardingV3(
        fragment: Fragment,
        binding: FragmentOnboardingV3Binding,
        introductionPosition: Int,
        realPosition: Int,
        isPageEnd: Boolean,
        isShowAd: Boolean,
        onBoardingConfig: OnBoardingConfig
    ) = Unit

    fun customizeOnBoardingV3EndTab(
        fragment: Fragment,
        binding: FragmentOnboardingV3EndTabBinding,
        position: Int
    ) = Unit
}
