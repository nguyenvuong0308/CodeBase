package com.codebasetemplate.features.feature_onboarding.ui.v3

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.core.startflow.R
import com.core.startflow.StartFlowScreenType
import com.core.startflow.databinding.FragmentOnboardingV3Binding
import com.codebasetemplate.features.feature_onboarding.ui.helper.OnBoardingConfigFactory
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingEvent
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingViewModel
import com.codebasetemplate.util.EventTracking
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.baseui.fragment.BaseFragment
import com.core.baseui.fragment.ScreenType
import com.core.baseui.fragment.argument
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.OnBoardingConfig
import com.core.startflow.onboarding.OnBoardingUiCustomizer
import com.core.utilities.setOnSingleClick
import com.core.utilities.visibleIf
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnBoardingFragmentV3 : BaseFragment<FragmentOnboardingV3Binding>() {

    private val sharedViewModel: OnBoardingViewModel by activityViewModels()

    @Inject
    lateinit var uiCustomizers: Set<@JvmSuppressWildcards OnBoardingUiCustomizer>

    companion object {
        fun newInstance(position: Int, isPageEnd: Boolean, isShowAd: Boolean, realPosition: Int) = OnBoardingFragmentV3().apply {
            this.realPosition = realPosition
            this.introductionPosition = position
            this.isShowAd = isShowAd
            this.isPageEnd = isPageEnd
        }
    }

    private var realPosition by argument<Int>()
    private var introductionPosition by argument<Int>()
    private var isPageEnd by argument<Boolean>()
    private var isShowAd by argument<Boolean>()

    override fun bindingProvider(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentOnboardingV3Binding {
        return FragmentOnboardingV3Binding.inflate(inflater, container, false)
    }

    override val screenType: ScreenType
        get() = StartFlowScreenType.OnBoarding

    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)

        viewBinding.ivIntroduction.setImageResource(
            OnBoardingConfigFactory.getImageResIntro(
                realPosition
            )
        )
        viewBinding.tvTitle.text =
            getString(OnBoardingConfigFactory.getStringIntro(realPosition))
        val onBoardingConfig = remoteConfigRepository.getOnBoardingConfig()
        val positionNext = onBoardingConfig.positionNext
        viewBinding.topNext.visibleIf(positionNext == OnBoardingConfig.POSITION_NEXT_TOP)
        viewBinding.bottomNext.visibleIf(positionNext == OnBoardingConfig.POSITION_NEXT_BOTTOM)
        if (positionNext == OnBoardingConfig.POSITION_NEXT_TOP) {
            viewBinding.frameAds.setBackgroundColor(Color.WHITE)
        } else {
            viewBinding.frameAds.setBackgroundResource(R.drawable.bg_white_round_top)
        }

        val indicators = listOf(
            viewBinding.indicator1,
            viewBinding.indicator2,
            viewBinding.indicator3
        )
        indicators.updateSelectedIndicator(realPosition)

        val indicators2 = listOf(
            viewBinding.indicator11,
            viewBinding.indicator22,
            viewBinding.indicator33
        )
        indicators2.updateSelectedIndicator(realPosition)

        viewBinding.tvNextTop.setOnSingleClick {
            if (isPageEnd) {
                sharedViewModel.navigateTo(OnBoardingEvent.FinishAction(EventTracking.VALUE_CLICK))
            } else {
                sharedViewModel.navigateTo(OnBoardingEvent.NextAction(EventTracking.VALUE_CLICK))
            }
        }

        viewBinding.tvNextBottom.setOnSingleClick {
            if (isPageEnd) {
                sharedViewModel.navigateTo(OnBoardingEvent.FinishAction(EventTracking.VALUE_CLICK))
            } else {
                sharedViewModel.navigateTo(OnBoardingEvent.NextAction(EventTracking.VALUE_CLICK))
            }
        }
//        viewBinding.swipe.visibleIf(introductionPosition == 0)
        viewBinding.frameAds.visibleIf(isShowAd)

        uiCustomizers.forEach {
            it.customizeOnBoardingV3(
                fragment = this,
                binding = viewBinding,
                introductionPosition = introductionPosition,
                realPosition = realPosition,
                isPageEnd = isPageEnd,
                isShowAd = isShowAd,
                onBoardingConfig = onBoardingConfig
            )
        }
    }

    override fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> {
        if (!isShowAd) return listOf()
        return OnBoardingConfigFactory.getOnboardingV3AdPlaceNames(introductionPosition)
    }

    override fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
        super.onBannerNativeResult(adResource)
        viewBinding.layoutBannerNative.processAdResource(
            adResource,
            OnBoardingConfigFactory.getOnboardingV3AdPlaceName(introductionPosition)
        )
    }

    private fun List<View>.updateSelectedIndicator(selectedPosition: Int) {
        val normalWidth = firstOrNull()?.layoutParams?.width ?: return
        val selectedWidth = normalWidth * 2

        forEachIndexed { index, view ->
            val isSelected = index == selectedPosition
            view.isSelected = isSelected
            view.layoutParams = view.layoutParams.apply {
                width = if (isSelected) selectedWidth else normalWidth
            }
        }
    }

}
