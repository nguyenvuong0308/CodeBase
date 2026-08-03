package com.core.config.domain

import com.core.config.data.FetchRemoteConfigState
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.AppOpenAdTypeConfig
import com.core.config.domain.data.BannerAdTypeConfig
import com.core.config.domain.data.AdPlace
import com.core.config.domain.data.AppConfig
import com.core.config.domain.data.IapConfig
import com.core.config.domain.data.InterstitialAdTypeConfig
import com.core.config.domain.data.LanguageActivityConfig
import com.core.config.domain.data.NativeAdTypeConfig
import com.core.config.domain.data.OnBoardingConfig
import com.core.config.domain.data.RewardedAdTypeConfig
import com.core.config.domain.data.RewardedInterstitialAdTypeConfig
import com.core.config.domain.data.PreventAdClickConfig
import com.core.config.domain.data.RequestConsentConfig
import com.core.config.domain.data.SplashScreenConfig
import com.core.config.domain.data.StartFlowConfig
import com.core.config.domain.data.TutorialConfig
import kotlinx.coroutines.flow.SharedFlow


interface RemoteConfigRepository {

    val fetchStateCompleteFlow: SharedFlow<FetchRemoteConfigState>

    fun fetchAndActive()

    fun getAppConfig(): AppConfig

    fun getIapConfig(): IapConfig

    fun getAdPlaceBy(adPlaceName: IAdPlaceName): AdPlace

    fun getPreventAdClickConfig(): PreventAdClickConfig

    fun getAdsDisableByCountry(): List<String>

    fun getAdPlacesDisableWhenDetectTestAd(): List<String>

    fun isTurnOnAdPlacesDisabledWhenDetectTestAd(): Boolean

    fun getSplashScreenConfig(): SplashScreenConfig

    fun getLanguageActivityConfig(): LanguageActivityConfig

    fun getOnBoardingConfig(): OnBoardingConfig

    fun getStartFlowConfig(): StartFlowConfig

    fun getAdPlaces(): List<AdPlace>

    fun getBannerAdConfig(): BannerAdTypeConfig

    fun getNativeAdConfig(): NativeAdTypeConfig

    fun getInterstitialAdConfig(): InterstitialAdTypeConfig

    fun getRewardedInterstitialAdConfig(): RewardedInterstitialAdTypeConfig

    fun getRewardedAdConfig(): RewardedAdTypeConfig

    fun getAppOpenAdConfig(): AppOpenAdTypeConfig

    fun getRequestConsentConfig(): RequestConsentConfig

    fun getTutorialConfig(): TutorialConfig

}
