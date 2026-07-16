package com.codebasetemplate.features.feature_onboarding.ui.v1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.activityViewModels
import com.codebasetemplate.databinding.CoreFragmentOnboardingFullNativeBinding
import com.codebasetemplate.features.feature_onboarding.ui.helper.OnBoardingConfigFactory
import com.codebasetemplate.required.firebase.GetDataFromRemoteUseCaseImpl
import com.codebasetemplate.required.shortcut.AppScreenType
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.baseui.R
import com.core.baseui.fragment.BaseFragment
import com.core.baseui.fragment.ScreenType
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.NativeAdPlace
import com.core.utilities.getStatusBarHeight
import com.core.utilities.padding
import com.core.utilities.setOnSingleClick
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnBoardingFullNativeFragment : BaseFragment<CoreFragmentOnboardingFullNativeBinding>() {

    private val sharedViewModel: OnBoardingViewModel by activityViewModels()

    @Inject
    lateinit var getDataFromRemoteUseCase: GetDataFromRemoteUseCaseImpl

    private val nativeAdPlaceName by lazy {
        OnBoardingConfigFactory.getOnBoardingAnchorFullAdPlaceName(getDataFromRemoteUseCase.onBoardingConfig)
    }

    override fun bindingProvider(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): CoreFragmentOnboardingFullNativeBinding {
        return CoreFragmentOnboardingFullNativeBinding.inflate(inflater, container, false)
    }

    override val screenType: ScreenType
        get() = AppScreenType.OnBoarding

    companion object {
        fun newInstance() = OnBoardingFullNativeFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fullscreenNativeAd =
            remoteConfigRepository.getAdPlaceBy(nativeAdPlaceName)
        if (fullscreenNativeAd.isNativeType()) {
            val backgroundColor = (fullscreenNativeAd as? NativeAdPlace)?.backgroundColor?.let {
                try {
                    it.toColorInt()
                } catch (e: Exception) {
                    ContextCompat.getColor(requireContext(), R.color.intro_blue)
                }
            } ?: ContextCompat.getColor(requireContext(), R.color.intro_blue)

            viewBinding.layoutRoot.setBackgroundColor(backgroundColor)
        }

        viewBinding.ivClose.setOnSingleClick {
            sharedViewModel.navigateTo(OnBoardingEvent.NextEvent)
        }
        viewBinding.layoutRoot.padding(top = getStatusBarHeight())
    }

    override fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
        viewBinding.layoutBannerNative.processAdResource(adResource, nativeAdPlaceName)
    }

    override fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> {
        return listOf(nativeAdPlaceName)
    }

}