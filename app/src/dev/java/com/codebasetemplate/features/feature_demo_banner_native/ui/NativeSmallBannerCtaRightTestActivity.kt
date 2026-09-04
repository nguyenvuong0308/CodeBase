package com.codebasetemplate.features.feature_demo_banner_native.ui

import android.os.Bundle
import android.view.LayoutInflater
import com.codebasetemplate.core.base_ui.CoreActivity
import com.codebasetemplate.databinding.ActivityNativeSmallBannerCtaRightTestBinding
import com.codebasetemplate.required.ads.AppAdPlaceName
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.NativeTemplateSize
import com.core.utilities.setOnSingleClick
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NativeSmallBannerCtaRightTestActivity :
    CoreActivity<ActivityNativeSmallBannerCtaRightTestBinding>() {

    override fun bindingProvider(
        inflater: LayoutInflater,
    ): ActivityNativeSmallBannerCtaRightTestBinding {
        return ActivityNativeSmallBannerCtaRightTestBinding.inflate(inflater)
    }

    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)
        viewBinding.reloadButton.setOnSingleClick {
            loadBannerOrNativeAds(TEST_PLACE_NAME, oneTimeLoad = true, isReload = true)
        }
    }

    override fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> = listOf(TEST_PLACE_NAME)

    override fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
        when (adResource) {
            is AdLoadBannerNativeUiResource.NativeAdRefreshStarted -> {
                viewBinding.layoutNative.processAdResource(adResource, TEST_PLACE_NAME)
            }

            is AdLoadBannerNativeUiResource.Loading -> {
                viewBinding.statusText.setText(com.codebasetemplate.R.string.small_banner_native_test_loading)
                viewBinding.layoutNative.processAdResource(
                    adResource.copy(nativeTemplateSize = NativeTemplateSize.SmallBannerCtaRight),
                    TEST_PLACE_NAME,
                )
            }

            is AdLoadBannerNativeUiResource.NativeAdLoaded -> {
                viewBinding.statusText.setText(com.codebasetemplate.R.string.small_banner_native_test_loaded)
                viewBinding.layoutNative.processAdResource(
                    adResource.copy(
                        nativeAdPlace = adResource.nativeAdPlace.copy(
                            nativeTemplateSize = NativeTemplateSize.SmallBannerCtaRight,
                        )
                    ),
                    TEST_PLACE_NAME,
                )
            }

            is AdLoadBannerNativeUiResource.AdFailed,
            is AdLoadBannerNativeUiResource.AdNetworkError,
            -> {
                viewBinding.statusText.setText(com.codebasetemplate.R.string.small_banner_native_test_failed)
                viewBinding.layoutNative.processAdResource(adResource, TEST_PLACE_NAME)
            }

            is AdLoadBannerNativeUiResource.BannerAdLoaded -> {
                viewBinding.statusText.setText(com.codebasetemplate.R.string.small_banner_native_test_wrong_type)
            }
        }
    }

    private companion object {
        val TEST_PLACE_NAME = AppAdPlaceName.ANCHORED_NATIVE_TEST
    }
}
