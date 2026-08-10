package com.codebasetemplate.features.feature_onboarding.ui.v2

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.fragment.app.activityViewModels
import com.core.startflow.R
import com.core.startflow.databinding.CoreFragmentOnboardingV2Binding
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingEvent
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingViewModel
import com.core.startflow.StartFlowScreenType
import com.core.startflow.onboarding.OnBoardingContentProvider
import com.core.startflow.onboarding.OnBoardingUiCustomizer
import com.core.startflow.onboarding.activeOnBoardingContentProvider
import com.core.baseui.fragment.BaseFragment
import com.core.baseui.fragment.ScreenType
import com.core.baseui.fragment.argument
import com.core.utilities.gone
import com.core.utilities.setOnSingleClick
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnBoardingFragment2: BaseFragment<CoreFragmentOnboardingV2Binding>() {

    private val sharedViewModel: OnBoardingViewModel by activityViewModels()

    @Inject
    lateinit var uiCustomizers: Set<@JvmSuppressWildcards OnBoardingUiCustomizer>

    @Inject
    lateinit var contentProviders: Set<@JvmSuppressWildcards OnBoardingContentProvider>

    companion object {
        fun newInstance(position: Int) = OnBoardingFragment2().apply {
            this.introductionPosition = position
        }
    }

    private var introductionPosition by argument<Int>()

    override fun bindingProvider(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): CoreFragmentOnboardingV2Binding {
        return CoreFragmentOnboardingV2Binding.inflate(inflater, container, false)
    }

    override val screenType: ScreenType
        get() = StartFlowScreenType.OnBoarding

    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)

        val contentProvider = contentProviders.activeOnBoardingContentProvider()
        val introPageCount = contentProvider.introPageCount.coerceAtLeast(1)

        viewBinding.ivIntroduction.setImageResource(contentProvider.getImageResIntro(introductionPosition))
        viewBinding.tvTitle.text = getString(contentProvider.getStringIntro(introductionPosition))
        contentProvider.getSubtitleIntro(introductionPosition)?.let {
            viewBinding.tvTitle2.text = getString(it)
        } ?: run {
            viewBinding.tvTitle2.gone()
        }


        viewBinding.tvNext.text =  if(introductionPosition == introPageCount - 1) {
            /**Set gradient cho button start*/
            viewBinding.tvNext.setGradientStrokeBackground(
                Color.RED,
                Color.BLUE,
                strokeWidthDp = 1.5f,
                cornerRadiusDp = 12f
            )
            viewBinding.tvNext.setFillGradientEnabled(
                true,
                intArrayOf(
                    "#fb03fb".toColorInt(),
                    "#0bdaff".toColorInt()
                )
            )
            viewBinding.layoutContent.setBackgroundColor(Color.TRANSPARENT)
            getString(R.string.core_onboarding_action_get_start)
        } else {
            viewBinding.tvNext.setFillGradientEnabled(false)
            viewBinding.tvNext.setTextColor(Color.WHITE)
            viewBinding.tvNext.setBackgroundResource(R.drawable.core_button_onboarding)
            viewBinding.layoutContent.setBackgroundResource(R.drawable.core_bg_content_onboarding_v2)
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
            it.customizeOnBoardingV2(
                fragment = this,
                binding = viewBinding,
                position = introductionPosition,
                isLastPage = introductionPosition == introPageCount - 1
            )
        }
    }

}
