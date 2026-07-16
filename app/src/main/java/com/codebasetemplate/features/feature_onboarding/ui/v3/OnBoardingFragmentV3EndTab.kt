package com.codebasetemplate.features.feature_onboarding.ui.v3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.codebasetemplate.databinding.FragmentOnboardingV3EndTabBinding
import com.codebasetemplate.features.feature_onboarding.ui.helper.OnBoardingConfigFactory
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingEvent
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingViewModel
import com.codebasetemplate.required.shortcut.AppScreenType
import com.codebasetemplate.util.EventTracking
import com.core.baseui.fragment.BaseFragment
import com.core.baseui.fragment.ScreenType
import com.core.baseui.fragment.argument
import com.core.utilities.setOnSingleClick
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnBoardingFragmentV3EndTab : BaseFragment<FragmentOnboardingV3EndTabBinding>() {

    private val sharedViewModel: OnBoardingViewModel by activityViewModels()

    companion object {
        fun newInstance(position: Int) = OnBoardingFragmentV3EndTab().apply {
            this.introductionPosition = position
        }
    }

    private var introductionPosition by argument<Int>()

    override fun bindingProvider(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentOnboardingV3EndTabBinding {
        return FragmentOnboardingV3EndTabBinding.inflate(inflater, container, false)
    }

    override val screenType: ScreenType
        get() = AppScreenType.OnBoarding

    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)
        viewBinding.ivIntroduction.setImageResource(
            OnBoardingConfigFactory.getImageResIntro(
                introductionPosition
            )
        )
        viewBinding.tvTitle.text =
            getString(OnBoardingConfigFactory.getStringIntro(introductionPosition))

        viewBinding.btGetStart.setOnSingleClick {
            sharedViewModel.navigateTo(OnBoardingEvent.FinishAction(EventTracking.VALUE_CLICK))
        }
    }

}
