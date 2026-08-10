package com.codebasetemplate.features.feature_onboarding.ui.v3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.core.startflow.StartFlowScreenType
import com.core.startflow.databinding.FragmentOnboardingV3EndTabBinding
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingEvent
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingViewModel
import com.codebasetemplate.util.EventTracking
import com.core.baseui.fragment.BaseFragment
import com.core.baseui.fragment.ScreenType
import com.core.baseui.fragment.argument
import com.core.startflow.onboarding.OnBoardingContentProvider
import com.core.startflow.onboarding.OnBoardingUiCustomizer
import com.core.startflow.onboarding.activeOnBoardingContentProvider
import com.core.utilities.setOnSingleClick
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnBoardingFragmentV3EndTab : BaseFragment<FragmentOnboardingV3EndTabBinding>() {

    private val sharedViewModel: OnBoardingViewModel by activityViewModels()

    @Inject
    lateinit var uiCustomizers: Set<@JvmSuppressWildcards OnBoardingUiCustomizer>

    @Inject
    lateinit var contentProviders: Set<@JvmSuppressWildcards OnBoardingContentProvider>

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
        get() = StartFlowScreenType.OnBoarding

    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)
        val contentProvider = contentProviders.activeOnBoardingContentProvider()
        viewBinding.ivIntroduction.setImageResource(
            contentProvider.getImageResIntro(
                introductionPosition
            )
        )
        viewBinding.tvTitle.text =
            getString(contentProvider.getStringIntro(introductionPosition))

        viewBinding.btGetStart.setOnSingleClick {
            sharedViewModel.navigateTo(OnBoardingEvent.FinishAction(EventTracking.VALUE_CLICK))
        }

        uiCustomizers.forEach {
            it.customizeOnBoardingV3EndTab(
                fragment = this,
                binding = viewBinding,
                position = introductionPosition
            )
        }
    }

}
