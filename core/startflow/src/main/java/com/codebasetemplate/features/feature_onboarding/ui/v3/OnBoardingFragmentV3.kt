package com.codebasetemplate.features.feature_onboarding.ui.v3

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.codebasetemplate.features.feature_onboarding.ui.model.OnBoardingItem
import com.core.startflow.R
import com.core.startflow.StartFlowScreenType
import com.core.startflow.databinding.FragmentOnboardingV3Binding
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingEvent
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingViewModel
import com.codebasetemplate.util.EventTracking
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.baseui.fragment.BaseFragment
import com.core.baseui.fragment.ScreenType
import com.core.baseui.fragment.argument
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.OnBoardingConfig
import com.core.startflow.OnBoardingConfigFactory as StartFlowOnBoardingConfigFactory
import com.core.startflow.onboarding.OnBoardingContentProvider
import com.core.startflow.onboarding.OnBoardingUiCustomizer
import com.core.startflow.onboarding.activeOnBoardingContentProvider
import com.core.utilities.gone
import com.core.utilities.setOnSingleClick
import com.core.utilities.visibleIf
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnBoardingFragmentV3 : BaseFragment<FragmentOnboardingV3Binding>() {

    private val sharedViewModel: OnBoardingViewModel by activityViewModels()

    @Inject
    lateinit var uiCustomizers: Set<@JvmSuppressWildcards OnBoardingUiCustomizer>

    @Inject
    lateinit var contentProviders: Set<@JvmSuppressWildcards OnBoardingContentProvider>

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

        val contentProvider = contentProviders.activeOnBoardingContentProvider()
        viewBinding.ivIntroduction.setImageResource(
            contentProvider.getImageResIntro(
                realPosition
            )
        )
        viewBinding.tvTitle.text =
            getString(contentProvider.getStringIntro(realPosition))
        val onBoardingConfig = remoteConfigRepository.getOnBoardingConfig()
        val positionNext = onBoardingConfig.positionNext
        viewBinding.topNext.visibleIf(positionNext == OnBoardingConfig.POSITION_NEXT_TOP)
        viewBinding.topNextV2.visibleIf(positionNext == OnBoardingConfig.POSITION_NEXT_TOP_V2)
        viewBinding.bottomNext.visibleIf(positionNext == OnBoardingConfig.POSITION_NEXT_BOTTOM)
        if (positionNext == OnBoardingConfig.POSITION_NEXT_TOP) {
            viewBinding.frameAds.setBackgroundColor(Color.WHITE)
        } else {
            viewBinding.frameAds.setBackgroundResource(R.drawable.bg_white_round_top)
        }

        val indicators2 = listOf(
            viewBinding.indicatorTopV11,
            viewBinding.indicatorTopV12,
            viewBinding.indicatorTopV13,
            viewBinding.indicatorTopV14,
        )
        indicators2.updateSelectedIndicator(realPosition)

        val indicators1 = listOf(
            viewBinding.indicatorTopV21,
            viewBinding.indicatorTopV22,
            viewBinding.indicatorTopV23,
            viewBinding.indicatorTopV24,

        )
        indicators1.updateSelectedIndicator(realPosition)

        val indicators0 = listOf(
            viewBinding.indicatorBottom1,
            viewBinding.indicatorBottom2,
            viewBinding.indicatorBottom3,
            viewBinding.indicatorBottom4
        )
        indicators0.updateSelectedIndicator(realPosition)

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
        return StartFlowOnBoardingConfigFactory.getOnboardingV3AdPlaceNames(introductionPosition)
    }

    override fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
        super.onBannerNativeResult(adResource)
        viewBinding.layoutBannerNative.processAdResource(
            adResource,
            StartFlowOnBoardingConfigFactory.getOnboardingV3AdPlaceName(introductionPosition)
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

            if(index > sharedViewModel.itemsOnboarding.filterIsInstance<OnBoardingItem.Item>().size - 1) {
                view.gone()
            }
        }
    }

}
