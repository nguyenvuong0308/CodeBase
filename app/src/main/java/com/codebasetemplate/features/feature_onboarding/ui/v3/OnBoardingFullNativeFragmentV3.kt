package com.codebasetemplate.features.feature_onboarding.ui.v3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.codebasetemplate.databinding.CoreFragmentOnboardingFullNativeBinding
import com.codebasetemplate.features.feature_onboarding.ui.helper.OnBoardingConfigFactory
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingEvent
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingViewModel
import com.codebasetemplate.required.firebase.GetDataFromRemoteUseCaseImpl
import com.codebasetemplate.required.shortcut.AppScreenType
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.baseui.R
import com.core.baseui.fragment.BaseFragment
import com.core.baseui.fragment.ScreenType
import com.core.baseui.fragment.argument
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.NativeAdPlace
import com.core.utilities.getStatusBarHeight
import com.core.utilities.padding
import com.core.utilities.setOnSingleClick
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnBoardingFullNativeFragmentV3 : BaseFragment<CoreFragmentOnboardingFullNativeBinding>() {

    companion object {
        private const val SHOW_CONTROL_ANIMATION_DURATION_MS = 300L

        fun newInstance(position: Int) = OnBoardingFullNativeFragmentV3().apply {
            this.introductionPosition = position
        }
    }

    private var introductionPosition by argument<Int>()
    private var isControlShownInCurrentView = false
    private val sharedViewModel: OnBoardingViewModel by activityViewModels()
    @Inject
    lateinit var getDataFromRemoteUseCase: GetDataFromRemoteUseCaseImpl

    override fun bindingProvider(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): CoreFragmentOnboardingFullNativeBinding {
        return CoreFragmentOnboardingFullNativeBinding.inflate(inflater, container, false)
    }

    override val screenType: ScreenType
        get() = AppScreenType.OnBoarding


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fullscreenNativeAd =
            remoteConfigRepository.getAdPlaceBy(OnBoardingConfigFactory.getOnboardingV3AdPlaceName(introductionPosition))
        if (fullscreenNativeAd.isNativeType()) {
            val backgroundColor = (fullscreenNativeAd as? NativeAdPlace)?.backgroundColor?.let {
                try {
                    it.toColorInt()
                } catch (e: Exception) {
                    ContextCompat.getColor(requireContext(), R.color.intro_blue)
                }
            } ?: ContextCompat.getColor(requireContext(), R.color.intro_blue)

            viewBinding.layoutRoot.setBackgroundColor(backgroundColor) // đặt màu nền giống màu nền quảng cáo
        }

        viewBinding.ivClose.setOnSingleClick {
            sharedViewModel.navigateTo(OnBoardingEvent.NextEvent)
        }
        val onBoardingConfig = getDataFromRemoteUseCase.onBoardingConfig
        val ivClose = viewBinding.ivClose
        val swipe = viewBinding.swipe
        isControlShownInCurrentView = false
        ivClose.prepareDelayedVisible(onBoardingConfig.isShowClose)
        swipe.prepareDelayedVisible(onBoardingConfig.isShowSwipe)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (isControlShownInCurrentView) return@repeatOnLifecycle
                delay(onBoardingConfig.delayShowCloseSwipeSeconds.coerceAtLeast(0L) * 1000L)
                ivClose.showWithFadeIfNeeded()
                swipe.showWithFadeIfNeeded()
                isControlShownInCurrentView = true
            }
        }
        viewBinding.layoutRoot.padding(top = getStatusBarHeight()) // Fullscreen cách statusbar (để hiển thị chữ "i" quảng cáo không bị che)
    }

    override fun onDestroyView() {
        viewBinding.ivClose.animate().cancel()
        viewBinding.swipe.animate().cancel()
        super.onDestroyView()
    }

    override fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> {
        return OnBoardingConfigFactory.getOnboardingV3AdPlaceNames(introductionPosition)
    }

    private fun View.prepareDelayedVisible(isShow: Boolean) {
        isGone = !isShow
        alpha = 0f
    }

    private fun View.showWithFadeIfNeeded() {
        if (!isVisible) return
        animate()
            .alpha(1f)
            .setDuration(SHOW_CONTROL_ANIMATION_DURATION_MS)
            .start()
    }

    override fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
        super.onBannerNativeResult(adResource)
        viewBinding.layoutBannerNative.processAdResource(
            adResource, OnBoardingConfigFactory.getOnboardingV3AdPlaceName(introductionPosition)
        )
    }

}
