package com.codebasetemplate.features.feature_demo_banner_native.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.codebasetemplate.core.base_ui.CoreActivity
import com.codebasetemplate.databinding.ActivityNativeGalleryPreviewBinding
import com.codebasetemplate.required.ads.AppAdPlaceName
import com.core.ads.customviews.ads.NativePictureInPicture
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.NativeExpandTemplate
import com.core.config.domain.data.NativeTemplateSize
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NativeGalleryPreviewActivity : CoreActivity<ActivityNativeGalleryPreviewBinding>() {

    override val isHideNavigationBar = true
    override val isHideStatusBar = true
    override val isSpaceStatusBar = false
    override val isSpaceDisplayCutout = false

    private val templateKey: String by lazy {
        intent.getStringExtra(NativeGalleryPreviewConfig.EXTRA_TEMPLATE_KEY)
            ?.takeIf(String::isNotBlank)
            ?: NativeTemplateSize.Small.key
    }
    private val isCollapsible: Boolean by lazy {
        intent.getBooleanExtra(NativeGalleryPreviewConfig.EXTRA_COLLAPSIBLE, false)
    }
    private val expandTemplate: NativeExpandTemplate by lazy {
        NativeExpandTemplate.getBy(
            intent.getStringExtra(NativeGalleryPreviewConfig.EXTRA_EXPAND_TEMPLATE)
        )
    }
    private var nativePictureInPicture: NativePictureInPicture? = null

    override fun bindingProvider(inflater: LayoutInflater): ActivityNativeGalleryPreviewBinding {
        return ActivityNativeGalleryPreviewBinding.inflate(inflater)
    }

    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)
        viewBinding.previewRoot.contentDescription =
            NativeGalleryPreviewConfig.loadingDescription(templateKey)
        configureContainerSize()
    }

    override fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> = listOf(TEST_PLACE_NAME)

    override fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
        when (adResource) {
            is AdLoadBannerNativeUiResource.Loading -> {
                if (templateKey != NativeGalleryPreviewConfig.NATIVE_PIP_KEY) {
                    viewBinding.layoutNative.processAdResource(
                        adResource.copy(
                            nativeTemplateSize = NativeTemplateSize.getSizeBy(templateKey)
                        ),
                        TEST_PLACE_NAME,
                    )
                }
            }

            is AdLoadBannerNativeUiResource.NativeAdLoaded -> showNativePreview(adResource)

            is AdLoadBannerNativeUiResource.AdFailed,
            is AdLoadBannerNativeUiResource.AdNetworkError,
            is AdLoadBannerNativeUiResource.BannerAdLoaded,
            -> {
                viewBinding.previewRoot.contentDescription =
                    NativeGalleryPreviewConfig.errorDescription(templateKey)
            }
        }
    }

    override fun onDestroy() {
        nativePictureInPicture?.dismiss()
        super.onDestroy()
    }

    private fun showNativePreview(adResource: AdLoadBannerNativeUiResource.NativeAdLoaded) {
        val previewPlace = adResource.nativeAdPlace.copy(
            nativeTemplateSize = NativeTemplateSize.getSizeBy(templateKey),
            isNativeCollapsible = isCollapsible,
            nativeExpandTemplate = expandTemplate,
            backgroundCta = "#22C55E",
            ctaRadius = 8,
            ctaTextColor = "#FFFFFF",
            ctaBorderColor = "#22C55E",
            borderColor = "#E5E7EB",
            backgroundColor = "#FFFFFF",
            backgroundFullColor = "#F5F6F8",
            primaryTextColor = "#111827",
            bodyTextColor = "#6B7280",
            backgroundColorAdsNotifyView = "#FFC107",
            textColorAdsNotifyView = "#FFFFFF",
            controlClosePosition = "right",
            collapsibleExpandCooldownSecond = 0,
            pipAnchorMode = NativePictureInPicture.AnchorMode.Fixed.key,
            pipMarginDp = 20f,
            pipTopMarginDp = 20f,
        )
        val previewResource = adResource.copy(nativeAdPlace = previewPlace)

        if (templateKey == NativeGalleryPreviewConfig.NATIVE_PIP_KEY) {
            val pictureInPicture = NativePictureInPicture(this).also {
                nativePictureInPicture = it
            }
            pictureInPicture.processAdResource(
                lifecycleOwner = this,
                activity = this,
                adResource = previewResource,
                placeName = TEST_PLACE_NAME,
                config = NativePictureInPicture.Config(
                    initialGravity = Gravity.TOP or Gravity.END,
                    anchorMode = NativePictureInPicture.AnchorMode.Fixed,
                    closeCountDownSeconds = 0,
                ),
            )
        } else {
            viewBinding.layoutNative.processAdResource(previewResource, TEST_PLACE_NAME)
            if (isCollapsible) {
                viewBinding.layoutNative.post {
                    viewBinding.layoutNative.setNativeExpanded(true)
                }
            }
        }

        viewBinding.previewRoot.postDelayed(
            {
                viewBinding.previewRoot.contentDescription =
                    NativeGalleryPreviewConfig.readyDescription(templateKey)
            },
            PREVIEW_SETTLE_DELAY_MILLIS,
        )
    }

    private fun configureContainerSize() {
        val fullScreen = NativeGalleryPreviewConfig.requiresFullScreen(templateKey)
        viewBinding.layoutNative.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            if (fullScreen) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT,
            if (fullScreen) Gravity.FILL else Gravity.CENTER,
        )
    }

    private companion object {
        const val PREVIEW_SETTLE_DELAY_MILLIS = 1_000L
        val TEST_PLACE_NAME = AppAdPlaceName.ANCHORED_NATIVE_TEST
    }
}
