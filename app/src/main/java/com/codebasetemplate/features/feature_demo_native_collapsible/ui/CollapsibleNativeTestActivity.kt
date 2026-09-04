package com.codebasetemplate.features.feature_demo_native_collapsible.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.codebasetemplate.R
import com.codebasetemplate.core.base_ui.CoreActivity
import com.codebasetemplate.databinding.ActivityNativeCollapsibleTestBinding
import com.codebasetemplate.required.ads.AppAdPlaceName
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.baseui.toolbar.CoreToolbarView
import com.core.config.domain.data.IAdPlaceName
import com.core.utilities.setOnSingleClick
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CollapsibleNativeTestActivity : CoreActivity<ActivityNativeCollapsibleTestBinding>() {

    override fun bindingProvider(inflater: LayoutInflater): ActivityNativeCollapsibleTestBinding {
        return ActivityNativeCollapsibleTestBinding.inflate(inflater)
    }

    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)
        viewBinding.toolbar.onToolbarListener = object : CoreToolbarView.OnToolbarListener {
            override fun onBack() {
                finish()
            }
        }
        viewBinding.reloadButton.setOnSingleClick {
            contextAds?.loadBannerOrNativeAds(adPlaceName = TEST_PLACE_NAME, isReload = true, oneTimeLoad = false)
        }
        val controls = CollapsibleNativeTestControls(viewBinding.layoutNative::setNativeExpanded)
        viewBinding.expandButton.setOnSingleClick { controls.expand() }
        viewBinding.collapseButton.setOnSingleClick { controls.collapse() }
        viewBinding.layoutNative.onClose = {
            viewBinding.layoutNative.visibility = View.GONE
            viewBinding.statusText.setText(R.string.collapsible_native_test_status_closed)
            setControlsEnabled(false)
        }
    }

    override fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> {
        return listOf(TEST_PLACE_NAME)
    }

    override fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
        when (adResource) {
            is AdLoadBannerNativeUiResource.NativeAdRefreshStarted -> {
                viewBinding.layoutNative.processAdResource(adResource, TEST_PLACE_NAME)
            }

            is AdLoadBannerNativeUiResource.Loading -> {
                setControlsEnabled(false)
                viewBinding.statusText.setText(R.string.collapsible_native_test_status_loading)
                viewBinding.layoutNative.processAdResource(adResource, TEST_PLACE_NAME)
            }

            is AdLoadBannerNativeUiResource.NativeAdLoaded -> {
                val collapsiblePlace = adResource.nativeAdPlace.asForcedCollapsibleTestPlace()
                viewBinding.statusText.text = getString(
                    R.string.collapsible_native_test_status_loaded,
                    collapsiblePlace.nativeTemplateSize.key,
                    collapsiblePlace.nativeExpandTemplate.key,
                    collapsiblePlace.controlClosePosition ?: "right",
                    collapsiblePlace.collapsibleExpandCooldownSecond ?: 0,
                )
                viewBinding.layoutNative.processAdResource(
                    adResource.copy(nativeAdPlace = collapsiblePlace),
                    TEST_PLACE_NAME,
                )
                setControlsEnabled(true)
            }

            is AdLoadBannerNativeUiResource.AdFailed -> {
                setControlsEnabled(false)
                viewBinding.statusText.setText(R.string.collapsible_native_test_status_failed)
                viewBinding.layoutNative.processAdResource(adResource, TEST_PLACE_NAME)
            }

            is AdLoadBannerNativeUiResource.AdNetworkError -> {
                setControlsEnabled(false)
                viewBinding.statusText.setText(R.string.collapsible_native_test_status_network_error)
                viewBinding.layoutNative.processAdResource(adResource, TEST_PLACE_NAME)
            }

            is AdLoadBannerNativeUiResource.BannerAdLoaded -> {
                setControlsEnabled(false)
                viewBinding.statusText.setText(R.string.collapsible_native_test_status_wrong_type)
                viewBinding.layoutNative.visibility = View.GONE
            }
        }
    }

    override fun onPause() {
        viewBinding.layoutNative.pauseCloseCountDown()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        viewBinding.layoutNative.resumeCloseCountDown()
    }

    private fun setControlsEnabled(enabled: Boolean) {
        viewBinding.expandButton.isEnabled = enabled
        viewBinding.collapseButton.isEnabled = enabled
    }

    private companion object {
        val TEST_PLACE_NAME = AppAdPlaceName.ANCHORED_NATIVE_COLLAPSIBLE_TEST
    }
}
