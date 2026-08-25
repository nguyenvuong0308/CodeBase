package com.codebasetemplate.required.startflow

import android.view.View
import com.codebasetemplate.R
import com.codebasetemplate.databinding.FragmentOnboardingV3Binding
import com.core.startflow.onboarding.v3.OnBoardingV3ActionPosition
import com.core.startflow.onboarding.v3.OnBoardingV3PageRenderer
import com.core.startflow.onboarding.v3.OnBoardingV3PageState
import com.core.startflow.onboarding.v3.OnBoardingV3RenderScope
import com.core.startflow.onboarding.v3.OnBoardingV3RenderedPage
import com.core.utilities.setOnSingleClick
import com.core.utilities.visibleIf
import javax.inject.Inject
import javax.inject.Singleton

/** App-owned renderer used to exercise the normal-ad V3 customization path. */
@Singleton
class AppOnBoardingV3PageRenderer @Inject constructor() : OnBoardingV3PageRenderer {

    override val priority: Int = 100

    override fun supports(state: OnBoardingV3PageState): Boolean {
        return state.isShowAd && !state.isFullAds
    }

    override fun render(scope: OnBoardingV3RenderScope): OnBoardingV3RenderedPage {
        val binding = FragmentOnboardingV3Binding.inflate(
            scope.inflater,
            scope.parent,
            false,
        )
        val state = scope.state

        binding.ivIntroduction.setImageResource(state.imageRes)
        binding.tvTitle.text = state.title
        binding.frameAds.visibleIf(state.isShowAd)

        val showBottomAction = state.actionPosition == OnBoardingV3ActionPosition.BOTTOM
        binding.topNext.visibleIf(!showBottomAction)
        binding.bottomNext.visibleIf(showBottomAction)

        val actionText = binding.root.context.getString(
            if (state.isPageEnd) {
                R.string.core_onboarding_action_get_start
            } else {
                R.string.common_next
            }
        )
        binding.tvNextTop.text = actionText
        binding.tvNextBottom.text = actionText
        binding.tvNextTop.setOnSingleClick { scope.actions.onPrimaryAction() }
        binding.tvNextBottom.setOnSingleClick { scope.actions.onPrimaryAction() }

        listOf(
            binding.indicator11,
            binding.indicator22,
            binding.indicator33,
            binding.indicator44,
        ).updateIndicators(state.introductionPosition, state.pageCount)
        listOf(
            binding.indicator1,
            binding.indicator2,
            binding.indicator3,
            binding.indicator4,
        ).updateIndicators(state.introductionPosition, state.pageCount)

        return OnBoardingV3RenderedPage(
            view = binding.root,
            onBannerNativeResult = { resource, placeName ->
                binding.layoutBannerNative.processAdResource(resource, placeName)
            },
        )
    }

    private fun List<View>.updateIndicators(selectedPosition: Int, pageCount: Int) {
        val normalWidth = firstOrNull()?.layoutParams?.width ?: return
        val selectedWidth = normalWidth * 2

        forEachIndexed { index, indicator ->
            indicator.visibleIf(index < pageCount)
            indicator.isSelected = index == selectedPosition
            indicator.layoutParams = indicator.layoutParams.apply {
                width = if (indicator.isSelected) selectedWidth else normalWidth
            }
        }
    }
}
