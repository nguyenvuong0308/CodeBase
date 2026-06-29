package com.core.ads.customviews.ads

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.view.isNotEmpty
import com.core.ads.R
import com.core.ads.admob.AdmobManager
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.AdType
import com.core.config.domain.data.BannerSize
import com.core.config.domain.data.NativeAdPlace
import com.core.config.domain.data.NativeTemplateSize
import com.core.utilities.changeSize
import com.core.utilities.gone
import com.core.utilities.util.Timber
import com.core.utilities.visible
import com.core.utilities.visibleIf
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.nativead.NativeAd

class BannerNativeContainerLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    var onClose: (() -> Unit) ?= null
    private var isAnchoredBannerSize = false

    private var isNativeFullscreen = false

    private var backgroundRes: Int? = null

    private var backgroundAdsNotifyView: Int? = null

    private var placeHolderView: PlaceHolderView? = null

    private var adLoadBannerNativeUiResource: AdLoadBannerNativeUiResource? = null

    private val customNativeAds: CustomNativeAds
        get() {
            return CustomNativeProvider.getInstance().getCustomNativeAds()
        }

    init {
        context.theme.obtainStyledAttributes(
            attrs, R.styleable.BannerNativeContainerLayout, 0, 0
        ).apply {
            val backgroundColorId = getColor(
                R.styleable.BannerNativeContainerLayout_bnl_background,
                Color.TRANSPARENT
            )
            setBackgroundColor(backgroundColorId)

            backgroundRes = getResourceId(
                R.styleable.BannerNativeContainerLayout_bnl_backgroundRes,
                R.drawable.core_bg_native_ads
            )
            backgroundAdsNotifyView = getResourceId(
                R.styleable.BannerNativeContainerLayout_bnl_backgroundAdsNotifyView,
                R.drawable.gnt_rounded_corners_shape
            )
            isNativeFullscreen =
                getBoolean(R.styleable.BannerNativeContainerLayout_bnl_isNativeFullscreen, false)
        }
        if (placeHolderView == null) {
            initPlaceHolder(R.layout.banner_adapter_small_shimmer)
        }
        runCatching {
            addView(placeHolderView)
        }
        placeHolderView?.startShimmer()
    }

    fun setAdSize(adType: AdType, bannerSize: BannerSize, nativeTemplateSize: NativeTemplateSize) {
        isAnchoredBannerSize = false

        val shimmerResId = when (adType) {

            AdType.Banner -> {
                when (bannerSize) {
                    BannerSize.Anchored -> {
                        isAnchoredBannerSize = true
                        R.layout.banner_adapter_small_shimmer
                    }

                    BannerSize.AnchoredLarge -> {
                        isAnchoredBannerSize = true
                        R.layout.banner_adapter_anchord_large_shimmer
                    }

                    BannerSize.Inline -> R.layout.banner_adapter_medium_shimmer
                    BannerSize.StandardLarge -> R.layout.banner_adapter_medium_shimmer
                    BannerSize.StandardMedium -> R.layout.banner_adapter_medium_shimmer
                }
            }

            AdType.Native -> {
                when (nativeTemplateSize) {
                    NativeTemplateSize.Medium -> R.layout.gnt_medium_template_view_shimmer

                    NativeTemplateSize.MediumCtaRight -> R.layout.gnt_medium_cta_right_shimmer

                    NativeTemplateSize.MediumCtaBottom -> R.layout.gnt_medium_cta_bottom_template_view_shimmer

                    NativeTemplateSize.MediumCtaRightTop -> R.layout.gnt_medium_cta_right_top_shimmer

                    NativeTemplateSize.MediumCtaTop -> R.layout.gnt_medium_cta_top_template_view_shimmer

                    NativeTemplateSize.MediumMediaLeft -> R.layout.gnt_medium_media_left_shimmer

                    NativeTemplateSize.MediumMediaRight -> R.layout.gnt_medium_media_right_shimmer

                    NativeTemplateSize.Small -> R.layout.gnt_small_template_view_shimmer

                    NativeTemplateSize.SmallCtaTop -> R.layout.gnt_small_cta_top_template_view_shimmer

                    NativeTemplateSize.SmallCtaBottom -> R.layout.gnt_small_cta_bottom_template_view_shimmer

                    NativeTemplateSize.SmallCtaRight -> R.layout.gnt_small_cta_right_shimmer

                    NativeTemplateSize.SmallForPopup -> R.layout.gnt_small_for_popup_template_view_shimmer

                    NativeTemplateSize.SmallLong -> R.layout.gnt_small_long_template_view_shimmer

                    NativeTemplateSize.MiniCtaRight -> R.layout.gnt_mini_cta_right_shimmer

                    NativeTemplateSize.FullCtaBottomOnboarding, NativeTemplateSize.FullCtaBottom, NativeTemplateSize.FullCtaTop, NativeTemplateSize.FullCtaRight, NativeTemplateSize.FullInterstitialV1, NativeTemplateSize.FullInterstitialV2 -> R.layout.gnt_full_cta_bottom_template_view_shimmer

                    else -> customNativeAds.createShimmerLayoutPlaceHolder(nativeTemplateSize = nativeTemplateSize)
                }
            }

            else -> R.layout.gnt_small_template_view_shimmer
        }


        if (placeHolderView == null) {
            initPlaceHolder(shimmerResId)
        } else {
            placeHolderView?.setPlaceHolder(shimmerResId)
        }

        if(findOldAdView() == null) {
            removeAllViewInChildViewIfNeed()
            runCatching {
                addView(placeHolderView)
            }
            placeHolderView?.startShimmer()
        }
    }

    private fun initPlaceHolder(@LayoutRes layoutResId: Int) {
        placeHolderView = PlaceHolderView(context)
        placeHolderView?.setPlaceHolder(layoutResId)
        placeHolderView?.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            if (isNativeFullscreen) {
                gravity = Gravity.BOTTOM
            }
        }
    }

//    override fun removeDetachedView(child: View?, animate: Boolean) {
//        super.removeDetachedView(child, true)
//    }

    fun setNativeFullScreenLoading() {
        val shimmerResId = R.layout.gnt_full_cta_bottom_template_view_shimmer
        if (placeHolderView == null) {
            initPlaceHolder(shimmerResId)
        } else {
            placeHolderView?.setPlaceHolder(shimmerResId)
        }
        removeAllViewInChildViewIfNeed()
        addView(placeHolderView)
        placeHolderView?.startShimmer()
    }

    fun onAdLoaded(adView: AdView) {
        try {
            if (adView.parent == this) return
            (adView.parent as? ViewGroup)?.let { oldParent ->
                // tránh animation/transition giữ child
                try { oldParent.endViewTransition(adView) } catch (e: Throwable) {Timber.d("onAdLoaded error ${e.message}")}
                oldParent.layoutTransition = null
                oldParent.removeView(adView)
            }
            removeAllViewInChildViewIfNeed()
            addView(adView)
        } catch (e: Exception) {
            // do nothing
            Timber.d("onAdLoaded error ${e.message}")
        }

    }

    private fun findOldAdView(): AdView? {
        return try {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child is AdView) {
                    return child
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    fun onAdLoaded(nativeAd: NativeAd, nativeAdPlace: NativeAdPlace) {
        removeAllViewInChildViewIfNeed()
        populateUnifiedNativeAdView(nativeAd, nativeAdPlace)
    }

    private fun removeAllViewInChildViewIfNeed() {
        if (isNotEmpty()) {
            val childView = getChildAt(0)
            if (childView is BaseNativeTemplateView) {
                childView.removeAllViews()
            }
        }
        removeAllViews()
    }

    private fun populateUnifiedNativeAdView(nativeAd: NativeAd, nativeAdPlace: NativeAdPlace) {
        Timber.d("populateUnifiedNativeAdView nativeAdPlace $nativeAdPlace")
        val borderColor =
            if (nativeAdPlace.borderColor != null) nativeAdPlace.borderColor else "#" + Integer.toHexString(
                ContextCompat.getColor(context, R.color.background_divider)
            )

        val style = NativeTemplateStyle.Builder()
            .withCallToActionBackgroundColor(nativeAdPlace.backgroundCta)
            .withCallToActionRadius(nativeAdPlace.ctaRadius)
            .withCallToActionTypefaceColor(nativeAdPlace.ctaTextColor)
            .withCtaBorderColor(nativeAdPlace.ctaBorderColor)
            .withBorderColor(borderColor)
            .withBackgroundColor(nativeAdPlace.backgroundColor)
            .withCountDownSecond(nativeAdPlace.countDownTimer)
            .withBackgroundFullColor(nativeAdPlace.backgroundFullColor)
            .withPrimaryTextTypefaceColor(nativeAdPlace.primaryTextColor)
            .withTertiaryTextTypefaceColor(nativeAdPlace.bodyTextColor)
            .withBackgroundResource(backgroundRes)
            .withMainBackgroundRadius(nativeAdPlace.backgroundRadius)
            .withBackgroundAdsNotifyView(backgroundAdsNotifyView)
            .withMediaBackgroundColor(nativeAdPlace.mediaBackgroundColor)
            .withIsEnableImmersive(nativeAdPlace.isEnableFullScreenImmersive)
            .withHideTextCountDown(nativeAdPlace.hideTextCountDown)
            .withHideTextSkipCountDown(nativeAdPlace.hideTextSkipCountDown)
            .withHideProgressCountDown(nativeAdPlace.hideProgressCountDown)
            .withProgressBarTint(nativeAdPlace.progressBarTint)
            .build()

        val nativeTemplateView = when (nativeAdPlace.nativeTemplateSize) {

            NativeTemplateSize.Medium -> NativeMediumTemplateView(context)

            NativeTemplateSize.SmallCtaBottom -> NativeSmallCtaBottomTemplateView(context)

            NativeTemplateSize.MediumCtaBottom -> NativeMediumCtaBottomTemplateView(context)

            NativeTemplateSize.MediumCtaTop -> NativeMediumCtaTopTemplateView(context)

            NativeTemplateSize.MediumCtaRight -> NativeMediumCtaRightTemplateView(context)

            NativeTemplateSize.MediumCtaRightTop -> NativeMediumCtaRightTopTemplateView(context)

            NativeTemplateSize.MediumMediaRight -> NativeMediumMediaRightTemplateView(context)

            NativeTemplateSize.MediumMediaLeft -> NativeMediumMediaLeftTemplateView(context)

            NativeTemplateSize.Small -> NativeSmallTemplateView(context)

            NativeTemplateSize.SmallCtaTop -> NativeSmallCtaTopTemplateView(context)

            NativeTemplateSize.SmallCtaRight -> NativeSmallCtaRightTemplateView(context)

            NativeTemplateSize.SmallForPopup -> NativeSmallForPopupTemplateView(context)

            NativeTemplateSize.SmallLong -> NativeSmallLongTemplateView(context)

            NativeTemplateSize.FullCtaBottom -> NativeFullCtaBottomTemplateView(context)

            NativeTemplateSize.FullCtaBottomOnboarding -> NativeFullCtaBottomOnboardingTemplateView(context)

            NativeTemplateSize.FullCtaTop -> NativeFullCtaTopTemplateView(context)

            NativeTemplateSize.FullCtaRight -> NativeFullCtaRightTemplateView(context)

            NativeTemplateSize.MiniCtaRight -> NativeMiniCtaRightTemplateView(context)

            NativeTemplateSize.FullInterstitialV1 -> NativeInterstitialV1View(context)

            NativeTemplateSize.FullInterstitialV2 -> NativeInterstitialV2View(context)

            is NativeTemplateSize.CustomKey -> customNativeAds.createNativeAds(context, nativeAdPlace)
        }
        runCatching {
            addView(nativeTemplateView)
        }

        nativeTemplateView.applyStyles(style)
        nativeTemplateView.setNativeAd(nativeAd)
        nativeTemplateView.onClose = {
            onClose?.invoke()
        }
    }


    fun processAdResource(adResource: AdLoadBannerNativeUiResource, placeName: IAdPlaceName, canVisible: Boolean = true, isHideNativeBannerWhenNetworkError: Boolean = false) {
        Timber.d("processAdResource adResource $adResource")
        if(adResource.commonAdPlaceName != placeName) return
        try {
            if(AdmobManager.isTestAd && AdmobManager.adPlacesDisabledWhenDetectTestAd.contains(placeName.name)) {
                gone()
                return
            }
        } catch (ex: Exception) {
            Timber.e(ex.message)
        }
        adLoadBannerNativeUiResource = adResource
        when (adResource) {
            is AdLoadBannerNativeUiResource.Loading -> {
                    setAdSize(
                        adResource.adType,
                        adResource.bannerSize,
                        adResource.nativeTemplateSize
                    )
                    visibleIf(canVisible)
            }

            is AdLoadBannerNativeUiResource.AdFailed -> {
                    gone()
            }

            is AdLoadBannerNativeUiResource.BannerAdLoaded -> {
                    visibleIf(canVisible)
                    onAdLoaded(adResource.bannerAd)
            }

            is AdLoadBannerNativeUiResource.NativeAdLoaded -> {
                    visibleIf(canVisible)
                    onAdLoaded(
                        adResource.nativeAd,
                        adResource.nativeAdPlace
                    )
            }

            is AdLoadBannerNativeUiResource.AdNetworkError -> {
                if (isHideNativeBannerWhenNetworkError) {
                    gone()
                }
            }
        }
    }

    fun processAdResourceOnRecyclerView(adResource: AdLoadBannerNativeUiResource, placeName: IAdPlaceName, isHideNativeBannerWhenNetworkError: Boolean = false) {
        Timber.Forest.d("processAdResource adResource $adResource")
        if (adResource.commonAdPlaceName != placeName) return
        adLoadBannerNativeUiResource = adResource
        when (adResource) {
            is AdLoadBannerNativeUiResource.AdFailed -> {
                changeSize(0, 0)
            }

            is AdLoadBannerNativeUiResource.Loading -> {
                setAdSize(
                    adResource.adType,
                    adResource.bannerSize,
                    adResource.nativeTemplateSize
                )
                visible()
                changeSize(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            }

            is AdLoadBannerNativeUiResource.NativeAdLoaded -> {
                visible()
                onAdLoaded(
                    adResource.nativeAd,
                    adResource.nativeAdPlace
                )
                changeSize(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            }

            is AdLoadBannerNativeUiResource.BannerAdLoaded -> {
                visible()
                onAdLoaded(adResource.bannerAd)
                changeSize(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            }

            is AdLoadBannerNativeUiResource.AdNetworkError -> {
                if (isHideNativeBannerWhenNetworkError) {
                    changeSize(0, 0)
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        onClose = null
    }

    fun pauseCloseCountDown() {
        getCurrentNativeTemplateView()?.onHostPause()
    }

    fun resumeCloseCountDown() {
        getCurrentNativeTemplateView()?.onHostResume()
    }

    private fun getCurrentNativeTemplateView(): BaseNativeTemplateView? {
        if (childCount == 0) return null
        return getChildAt(0) as? BaseNativeTemplateView
    }
}
