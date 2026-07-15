package com.core.config.domain.data

import android.util.Log

sealed class AdPlace {
    abstract val isTrackingClick: Boolean

    abstract val isTrackingShow: Boolean

    abstract val placeName: IAdPlaceName

    abstract val adId: String

    // Higher-priority ad unit ids used as a waterfall before falling back to adId.
    abstract val highFloorAdIds: List<String>

    abstract val isEnable: Boolean

    abstract val adType: AdType

    abstract val isAutoLoadAfterDismiss: Boolean

    abstract val isIgnoreInterval: Boolean

    abstract val isTutorialFlow: Boolean


    fun isNotValidToLoad(): Boolean {
        Log.d("AdmobManager", "adId: $adId isEnable: $isEnable adType: $adType")
        return adId.isBlank() || !isEnable || isAdTypeNone()
    }

    fun isBannerType() = adType is AdType.Banner

    fun isNativeType() = adType is AdType.Native

    fun isInterstitialType() = adType is AdType.Interstitial

    fun isRewardedInterstitialType() = adType is AdType.RewardedInterstitial

    fun isRewardedVideoType() = adType is AdType.RewardedVideo

    fun isAppOpenType() = adType is AdType.AppOpen

    fun isAdTypeNone() = adType is AdType.None

}

data class RewardedVideoAdPlace(
    override val placeName: IAdPlaceName,
    override val isTrackingShow: Boolean,
    override val isTrackingClick: Boolean,
    override val adId: String,
    override val highFloorAdIds: List<String>,
    override val isEnable: Boolean,
    override val adType: AdType,
    override val isAutoLoadAfterDismiss: Boolean,
    override val isIgnoreInterval: Boolean,
    override val isTutorialFlow: Boolean,
): AdPlace() {
}

data class RewardedInterstitialAdPlace(
    override val isTrackingShow: Boolean,
    override val isTrackingClick: Boolean,
    override val placeName: IAdPlaceName,
    override val adId: String,
    override val highFloorAdIds: List<String>,
    override val isEnable: Boolean,
    override val adType: AdType,
    override val isAutoLoadAfterDismiss: Boolean,
    override val isIgnoreInterval: Boolean,
    override val isTutorialFlow: Boolean,
): AdPlace()

data class InterstitialAdPlace(
    override val isTrackingShow: Boolean,
    override val isTrackingClick: Boolean,
    override val placeName: IAdPlaceName,
    override val adId: String,
    override val highFloorAdIds: List<String>,
    override val isEnable: Boolean,
    override val adType: AdType,
    override val isAutoLoadAfterDismiss: Boolean,
    override val isIgnoreInterval: Boolean,
    override val isTutorialFlow: Boolean,
    val plusInterval: Int,
): AdPlace()

data class NativeAdPlace(
    override val isTrackingShow: Boolean,
    override val isTrackingClick: Boolean,
    val nativeTemplateSize: NativeTemplateSize,
    val backgroundCta: String?,
    val ctaRadius: Int?,
    val ctaTextColor: String?,
    val ctaBorderColor: String?,
    val borderColor: String?,
    val backgroundColor: String?,
    val countDownTimer: Int?,
    val backgroundFullColor: String?,
    val backgroundColorAdsNotifyView: String?,
    val textColorAdsNotifyView: String?,
    val mediaBackgroundColor: String?,
    val backgroundRadius: Int?,
    val primaryTextColor: String?,
    val bodyTextColor: String?,
    val isEnableFullScreenImmersive: Boolean?,
    val expiredTimeSecond: Int?,
    val hideTextSkipCountDown: Boolean?,
    val hideTextCountDown: Boolean?,
    val hideProgressCountDown: Boolean?,
    val progressBarTint: String?,
    override val placeName: IAdPlaceName,
    override val adId: String,
    override val highFloorAdIds: List<String>,
    override val isEnable: Boolean,
    override val adType: AdType,
    override val isAutoLoadAfterDismiss: Boolean,
    override val isIgnoreInterval: Boolean,
    override val isTutorialFlow: Boolean,
): AdPlace()

data class BannerAdPlace(
    override val isTrackingShow: Boolean,
    override val isTrackingClick: Boolean,
    val bannerSize: BannerSize,
    val isCollapsible: Boolean,
    val autoReloadCollapsible: Boolean,
    override val placeName: IAdPlaceName,
    override val adId: String,
    override val highFloorAdIds: List<String>,
    override val isEnable: Boolean,
    override val adType: AdType,
    override val isAutoLoadAfterDismiss: Boolean,
    override val isIgnoreInterval: Boolean,
    override val isTutorialFlow: Boolean,
): AdPlace()

data class AppOpenAdPlace(
    override val isTrackingShow: Boolean,
    override val isTrackingClick: Boolean,
    val limitShow: Int,
    override val placeName: IAdPlaceName,
    override val adId: String,
    override val highFloorAdIds: List<String>,
    override val isEnable: Boolean,
    override val adType: AdType,
    override val isAutoLoadAfterDismiss: Boolean,
    override val isIgnoreInterval: Boolean,
    override val isTutorialFlow: Boolean,
): AdPlace()

data class NoneAdPlace(
    override val isTrackingShow: Boolean = false,
    override val isTrackingClick: Boolean = false,
    override val placeName: IAdPlaceName = CoreAdPlaceName.NONE,
    override val adId: String = "",
    override val highFloorAdIds: List<String> = emptyList(),
    override val isEnable: Boolean = false,
    override val adType: AdType = AdType.None,
    override val isAutoLoadAfterDismiss: Boolean = false,
    override val isIgnoreInterval: Boolean = false,
    override val isTutorialFlow: Boolean = false,
): AdPlace()

