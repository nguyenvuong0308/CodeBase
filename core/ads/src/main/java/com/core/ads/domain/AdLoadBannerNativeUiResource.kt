package com.core.ads.domain

import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.nativead.NativeAd
import com.core.config.domain.data.BannerAdPlace
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.AdType
import com.core.config.domain.data.BannerSize
import com.core.config.domain.data.NativeAdPlace
import com.core.config.domain.data.NativeTemplateSize

sealed class AdLoadBannerNativeUiResource(val commonAdPlaceName: IAdPlaceName) {

    data class Loading(
        val adPlaceName: IAdPlaceName,
        val adType: AdType,
        val bannerSize: BannerSize,
        val nativeTemplateSize: NativeTemplateSize
        ) : AdLoadBannerNativeUiResource(adPlaceName)

    /**
     * Signals that a native refresh has started while the previously loaded ad stays visible.
     * Consumers may use this event to dismiss transient UI such as a collapsible popup, but must
     * not replace the current native with a loading placeholder.
     */
    data class NativeAdRefreshStarted(
        val adPlaceName: IAdPlaceName
    ) : AdLoadBannerNativeUiResource(adPlaceName)

    data class AdFailed(val adPlaceName: IAdPlaceName) : AdLoadBannerNativeUiResource(adPlaceName)

    data class AdNetworkError(val adPlaceName: IAdPlaceName) : AdLoadBannerNativeUiResource(adPlaceName)

    data class BannerAdLoaded(
        val bannerAd: AdView,
        val adPlaceName: IAdPlaceName,
        val bannerAdPlace: BannerAdPlace
    ) : AdLoadBannerNativeUiResource(adPlaceName)

    data class NativeAdLoaded(
        val nativeAd: NativeAd,
        val adPlaceName: IAdPlaceName,
        val nativeAdPlace: NativeAdPlace
    ) : AdLoadBannerNativeUiResource(adPlaceName)
}
