package com.codebasetemplate.features.main.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.codebasetemplate.databinding.CoreFragmentMainBinding
import com.codebasetemplate.features.main.ui.host.MainHostEvent
import com.codebasetemplate.features.main.ui.host.MainHostViewModel
import com.codebasetemplate.required.ads.AppAdPlaceName
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.baseui.fragment.BaseChildOfHostFragment
import com.core.baseui.fragment.CoreScreenType
import com.core.baseui.fragment.ScreenType
import com.core.baseui.toolbar.CoreToolbarView
import com.core.config.domain.data.IAdPlaceName
import com.core.utilities.setOnSingleClick
import com.core.utilities.toast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainChildOfHostFragment : BaseChildOfHostFragment<CoreFragmentMainBinding, MainHostEvent, MainHostViewModel>() {
    override fun bindingProvider(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): CoreFragmentMainBinding {
        return CoreFragmentMainBinding.inflate(inflater, container, false)
    }

    override val hostViewModel: MainHostViewModel by viewModels(ownerProducer = { requireParentFragment() })
    override val screenType: ScreenType
        get() = CoreScreenType.Main

    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)
        viewBinding.run {
            toolbar.onToolbarListener = object : CoreToolbarView.OnToolbarListener {
                override fun onAction() {
                    hostViewModel.navigateTo(MainHostEvent.OpenSetting)
                }
            }

            nativeAndBannerLayout.setOnSingleClick {
                hostViewModel.navigateTo(MainHostEvent.OpenBannerAndNative)
            }

            nativeInListLayout.setOnSingleClick {
                hostViewModel.navigateTo(MainHostEvent.OpenNativeInList)
            }

            showInterstitialLayout.setOnSingleClick {
                showInterAd(AppAdPlaceName.FULLSCREEN_TEST) {
                    toast("Show inter ad completed")
                }
            }

            showRewardLayout.setOnSingleClick {
                showRewardAd(AppAdPlaceName.REWARD_TEST) { isShown, isEarnedReward, isNoAds ->
                    toast("Show reward ad completed $isEarnedReward")
                }
            }

            showInterstitialLayoutLazyLoad.setOnSingleClick {
                showInterAd(AppAdPlaceName.FULLSCREEN_TEST_LAZY_LOAD) {
                    toast("Show inter ad completed")
                }
            }

            loadInterstitialLayout.setOnSingleClick {
                loadInterstitialAds(AppAdPlaceName.FULLSCREEN_TEST_LAZY_LOAD, oneTimeLoad = true)
            }

            shopLayout.setOnSingleClick {
                hostViewModel.navigateTo(MainHostEvent.OpenShop)
            }

        }
    }

    override fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> {
        return listOf(
            AppAdPlaceName.ANCHORED_BOTTOM_HOME
        )
    }

    override fun providerPreloadBannerNativeAdPlaceName(): List<IAdPlaceName> {
        return listOf(
            AppAdPlaceName.ANCHORED_NATIVE_IN_LIST_TEST,
            AppAdPlaceName.ANCHORED_BANNER_TEST,
            AppAdPlaceName.ANCHORED_NATIVE_TEST
        )
    }

    /***/
    override fun providerInterAdPlaceName(): List<IAdPlaceName> {
        return listOf(AppAdPlaceName.FULLSCREEN_TEST)
    }

    override fun providerRewardAdPlaceName(): List<IAdPlaceName> {
        return listOf(
            AppAdPlaceName.REWARD_TEST
        )
    }

    override fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
        viewBinding.layoutBannerNative.processAdResource(
            adResource,
            AppAdPlaceName.ANCHORED_BOTTOM_HOME
        )
    }

}