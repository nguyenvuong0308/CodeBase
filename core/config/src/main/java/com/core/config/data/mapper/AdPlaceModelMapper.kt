package com.core.config.data.mapper

import android.content.Context
import com.core.config.data.model.AdPlaceModel
import com.core.config.domain.data.AdType
import com.core.config.domain.data.AdPlace
import com.core.config.domain.data.AppOpenAdPlace
import com.core.config.domain.data.BannerAdPlace
import com.core.config.domain.data.BannerSize
import com.core.config.domain.data.CoreAdPlaceName
import com.core.config.domain.data.CoreAdPlaceName.NONE
import com.core.config.domain.data.IAppProviderAdPlaceName
import com.core.config.domain.data.InterstitialAdPlace
import com.core.config.domain.data.NativeAdPlace
import com.core.config.domain.data.NativeTemplateSize
import com.core.config.domain.data.NoneAdPlace
import com.core.config.domain.data.RewardedInterstitialAdPlace
import com.core.config.domain.data.RewardedVideoAdPlace
import com.core.utilities.isAppDebuggable
import com.core.utilities.util.Timber
import com.core.utilities.util.toast.Toasty
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AdPlaceModelMapper @Inject constructor(
    private val appAdPlaceName: IAppProviderAdPlaceName,
    @ApplicationContext
    private val context: Context
): ModelMapper<AdPlaceModel, AdPlace> {

    override fun toData(model: AdPlaceModel): AdPlace {
        val placeNameApp = appAdPlaceName.findAdPlaceName(model.adPlace ?: "")
        val placeNameCore =  CoreAdPlaceName.fromKey(model.adPlace ?: "")
        if(placeNameApp != null && placeNameCore != NONE && context.isAppDebuggable()) {
            Toasty.error(context, "Conflict adPlaname core and app ${placeNameCore.name}, please remove it from app module").show()
            Timber.e("Conflict adPlaname core and app ${placeNameCore.name}, please remove it from app module")
        }
        val placeName = placeNameApp?: placeNameCore
        val adId = model.adId ?: ""
        val adType = AdType.getAdTypeBy(model.adType ?: "")
        val isEnable = model.isEnable ?: false
        val isAutoLoadAfterDismiss = model.isAutoLoadAfterDismiss ?: true
        val isIgnoreInterval = model.isIgnoreInterval ?: false
        val isTrackingClick = model.isTrackingClick ?: false
        val isTrackingShow = model.isTrackingShow ?: false
        val ctaRadius = model.ctaRadius
        return when(adType) {
            AdType.RewardedVideo -> RewardedVideoAdPlace(
                placeName = placeName,
                adId = adId,
                adType = adType,
                isEnable = isEnable,
                isAutoLoadAfterDismiss = isAutoLoadAfterDismiss,
                isIgnoreInterval = isIgnoreInterval,
                isTrackingClick = isTrackingClick,
                isTrackingShow = isTrackingShow
            )
            AdType.RewardedInterstitial -> RewardedInterstitialAdPlace(
                placeName = placeName,
                adId = adId,
                adType = adType,
                isEnable = isEnable,
                isAutoLoadAfterDismiss = isAutoLoadAfterDismiss,
                isIgnoreInterval = isIgnoreInterval,
                isTrackingClick = isTrackingClick,
                isTrackingShow = isTrackingShow
            )
            AdType.Interstitial -> InterstitialAdPlace(
                placeName = placeName,
                adId = adId,
                adType = adType,
                isEnable = isEnable,
                isAutoLoadAfterDismiss = isAutoLoadAfterDismiss,
                isIgnoreInterval = isIgnoreInterval,
                isTrackingClick = isTrackingClick,
                isTrackingShow = isTrackingShow,
                plusInterval = model.plusInterval ?: 0
            )
            AdType.Native -> NativeAdPlace(
                placeName = placeName,
                adId = adId,
                adType = adType,
                isEnable = isEnable,
                isAutoLoadAfterDismiss = isAutoLoadAfterDismiss,
                isIgnoreInterval = isIgnoreInterval,
                nativeTemplateSize = NativeTemplateSize.getSizeBy(model.nativeTemplateSize ?: ""),
                backgroundCta = model.backgroundCta,
                borderColor = model.borderColor,
                backgroundColor = model.backgroundColor,
                primaryTextColor = model.primaryTextColor,
                bodyTextColor = model.bodyTextColor,
                isEnableFullScreenImmersive = model.isEnableFullScreenImmersive,
                isTrackingClick = isTrackingClick,
                ctaTextColor = model.ctaTextColor,
                isTrackingShow = isTrackingShow,
                ctaRadius = ctaRadius,
                backgroundRadius = model.backgroundRadius,
                ctaBorderColor = model.ctaBorderColor,
                backgroundFullColor = model.backgroundFullColor,
                expiredTimeSecond = model.expiredTimeSecond,
                countDownTimer = model.countDownTimer,
                mediaBackgroundColor = model.mediaBackgroundColor,
                hideTextCountDown = model.hideTextCountDown,
                hideProgressCountDown = model.hideProgressCountDown,
                hideTextSkipCountDown = model.hideTextSkipCountDown,
                progressBarTint = model.progressBarTint
            )
            AdType.Banner -> BannerAdPlace(
                placeName = placeName,
                adId = adId,
                adType = adType,
                isEnable = isEnable,
                isAutoLoadAfterDismiss = isAutoLoadAfterDismiss,
                isIgnoreInterval = isIgnoreInterval,
                bannerSize = BannerSize.getSizeBy(model.bannerSize ?: ""),
                isCollapsible = model.isCollapsible ?: false,
                isTrackingClick = isTrackingClick,
                isTrackingShow = isTrackingShow,
                autoReloadCollapsible = model.autoReloadCollapsible ?: false
            )
            AdType.AppOpen -> AppOpenAdPlace(
                placeName = placeName,
                adId = adId,
                adType = adType,
                isEnable = isEnable,
                isAutoLoadAfterDismiss = isAutoLoadAfterDismiss,
                isIgnoreInterval = isIgnoreInterval,
                limitShow = model.limitShow ?: 10000,
                isTrackingClick = isTrackingClick,
                isTrackingShow = isTrackingShow
            )
            AdType.None -> NoneAdPlace()
        }
    }

}
