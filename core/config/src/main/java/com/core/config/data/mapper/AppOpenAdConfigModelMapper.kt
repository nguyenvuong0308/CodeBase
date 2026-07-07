package com.core.config.data.mapper

import com.core.config.data.model.AppOpenAdConfigModel
import com.core.config.data.helper.ConfigParam
import com.core.config.domain.data.AppOpenAdTypeConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AppOpenAdConfigModelMapper @Inject constructor(
): ModelMapper<AppOpenAdConfigModel, AppOpenAdTypeConfig> {

    override fun toData(model: AppOpenAdConfigModel): AppOpenAdTypeConfig {
        return AppOpenAdTypeConfig(
            timeMillisDelayBeforeShow = model.timeMillisDelayBeforeShow ?: ConfigParam.DEFAULT_OPEN_APP_AD_TIME_MILLIS_DELAY_BEFORE_SHOW,
            timeInterval = model.timeInterval ?: ConfigParam.DEFAULT_OPEN_APP_AD_TIME_INTERVAL,
            isEnableRetry = model.isEnableRetry ?: ConfigParam.RETRY_IS_ENABLE_RETRY,
            maxRetryCount = model.maxRetryCount ?: ConfigParam.RETRY_MAX_RETRY_COUNT,
            retryIntervalSecondList = model.retryIntervalSecondList ?: ConfigParam.RETRY_INTERVAL_LIST,
            reopenMode = model.reopenMode ?: AppOpenAdTypeConfig.REOPEN_MODE_APP_OPEN_AD
        )
    }

}