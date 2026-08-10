package com.codebasetemplate.features.feature_onboarding.ui.v1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.core.startflow.R
import com.core.startflow.databinding.CoreFragmentOnboardingBinding
import com.core.startflow.StartFlowScreenType
import com.core.startflow.onboarding.OnBoardingContentProvider
import com.core.startflow.onboarding.OnBoardingUiCustomizer
import com.core.startflow.onboarding.activeOnBoardingContentProvider
import com.core.baseui.fragment.BaseFragment
import com.core.baseui.fragment.ScreenType
import com.core.baseui.fragment.argument
import com.core.utilities.setOnSingleClick
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnBoardingFragment: BaseFragment<CoreFragmentOnboardingBinding>() {

    private val sharedViewModel: OnBoardingViewModel by activityViewModels()

    @Inject
    lateinit var uiCustomizers: Set<@JvmSuppressWildcards OnBoardingUiCustomizer>

    @Inject
    lateinit var contentProviders: Set<@JvmSuppressWildcards OnBoardingContentProvider>

    companion object {
        fun newInstance(position: Int) = OnBoardingFragment().apply {
            this.introductionPosition = position
        }
    }

    private var introductionPosition by argument<Int>()

    override fun bindingProvider(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): CoreFragmentOnboardingBinding {
        return CoreFragmentOnboardingBinding.inflate(inflater, container, false)
    }

    override val screenType: ScreenType
        get() = StartFlowScreenType.OnBoarding

    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)

        val contentProvider = contentProviders.activeOnBoardingContentProvider()
        val introPageCount = contentProvider.introPageCount.coerceAtLeast(1)

        viewBinding.ivIntroduction.setImageResource(contentProvider.getImageResIntro(introductionPosition))
        viewBinding.tvTitle.text = getString(contentProvider.getStringIntro(introductionPosition))

        viewBinding.dotsIndicator.setCountPage(introPageCount)
        viewBinding.dotsIndicator.setPage(introductionPosition)

        viewBinding.tvNext.text =  if(introductionPosition == introPageCount - 1) {
            getString(R.string.core_onboarding_action_get_start)
        } else {
            getString(R.string.core_onboarding_action_next)
        }

        viewBinding.tvNext.setOnSingleClick {
            if(introductionPosition == introPageCount - 1) {
                sharedViewModel.navigateTo(OnBoardingEvent.FinishStep)
            } else {
                sharedViewModel.navigateTo(OnBoardingEvent.NextEvent)
            }
        }

        uiCustomizers.forEach {
            it.customizeOnBoardingV1(
                fragment = this,
                binding = viewBinding,
                position = introductionPosition,
                isLastPage = introductionPosition == introPageCount - 1
            )
        }
    }

}
