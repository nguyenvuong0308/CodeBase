package com.core.config.data.mapper

import com.core.config.data.helper.ConfigParam
import com.core.config.data.model.SplashScreenConfigModel
import com.core.config.domain.data.AdType
import com.core.config.domain.data.SplashScreenConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SplashScreenConfigModelMapper @Inject constructor(
): ModelMapper<SplashScreenConfigModel, SplashScreenConfig> {

    override fun toData(model: SplashScreenConfigModel): SplashScreenConfig {
        return SplashScreenConfig(
            maxTimeToWaitAppOpenAd = model.maxTimeToWaitAppOpenAd ?: ConfigParam.SPLASH_SCREEN_CONFIG_DEFAULT_MAX_TIME_TO_WAIT_APP_OPEN_AD,
            timeSkipAppOpenAdWhenNotAvailable = model.timeSkipAppOpenAdWhenNotAvailable ?: ConfigParam.SPLASH_SCREEN_CONFIG_DEFAULT_TIME_SKIP_APP_OPEN_AD_WHEN_NOT_AVAILABLE,
            adTypeFirstOpen = AdType.getAdTypeBy(model.adTypeFirstOpen ?: AdType.AppOpen.key),
            adType = AdType.getAdTypeBy(model.adType ?: AdType.AppOpen.key),
            minTimeWaitProgressBeforeShowAd = model.minTimeWaitProgressBeforeShowAd ?: ConfigParam.SPLASH_SCREEN_CONFIG_DEFAULT_MIN_TIME_WAIT_PROGRESS_BEFORE_SHOW_AD,
            isEnableRetry = model.isEnableRetry ?: ConfigParam.SPLASH_SCREEN_CONFIG_DEFAULT_ENABLE_RETRY,
            maxRetryCount = model.maxRetryCount ?: ConfigParam.SPLASH_SCREEN_CONFIG_DEFAULT_MAX_RETRY_COUNT,
            retryFixedDelay = model.retryFixedDelay ?: ConfigParam.SPLASH_SCREEN_CONFIG_DEFAULT_RETRY_FIXED_DELAY,
            isLoadBeforeEuConsent = model.isLoadBeforeEuConsent ?: ConfigParam.SPLASH_SCREEN_CONFIG_DEFAULT_IS_LOAD_BEFORE_CONSENT,
            isEnableFirstOpen = model.isEnableFirstOpen,
            isEnableOpen = model.isEnableOpen,
        )
    }

}