package com.core.config.data.mapper

import com.core.config.data.model.InterstitialAdConfigModel
import com.core.config.data.helper.ConfigParam
import com.core.config.domain.data.InterstitialAdTypeConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class InterstitialAdConfigModelMapper @Inject constructor(
): ModelMapper<InterstitialAdConfigModel, InterstitialAdTypeConfig> {

    override fun toData(model: InterstitialAdConfigModel): InterstitialAdTypeConfig {
        return InterstitialAdTypeConfig(
            isWaitLoadToShow = model.isWaitLoadToShow ?: false,
            adsPerSession = model.adsPerSession ?: ConfigParam.INTERSTITIAL_AD_CONFIG_DEFAULT_ADS_PER_SESSION,
            timePerSession = model.timePerSession ?: ConfigParam.INTERSTITIAL_AD_CONFIG_DEFAULT_TIME_PER_SESSION,
            timeInterval = model.timeInterval ?: ConfigParam.INTERSTITIAL_AD_CONFIG_DEFAULT_TIME_INTERVAL,
            timeIntervalAfterShowOpenAd = model.timeIntervalAfterShowOpenAd ?: ConfigParam.REOPEN_TO_INTERSTITIAL_AD_CONFIG_DEFAULT_TIME_INTERVAL,
            meaningfulActionsBetweenInterstitial = model.meaningfulActionsBetweenInterstitial
                ?: ConfigParam.INTERSTITIAL_AD_CONFIG_DEFAULT_MEANINGFUL_ACTIONS,
            isEnableRetry = model.isEnableRetry ?: ConfigParam.RETRY_IS_ENABLE_RETRY,
            maxRetryCount = model.maxRetryCount ?: ConfigParam.RETRY_MAX_RETRY_COUNT,
            retryIntervalSecondList = model.retryIntervalSecondList ?: ConfigParam.RETRY_INTERVAL_LIST,
        )
    }

}
