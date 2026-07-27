package com.core.config.data.mapper

import com.core.config.data.helper.ConfigParam
import com.core.config.data.model.BannerAdConfigModel
import com.core.config.domain.data.BannerAdTypeConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class BannerAdConfigModelMapper @Inject constructor(
): ModelMapper<BannerAdConfigModel, BannerAdTypeConfig> {

    override fun toData(model: BannerAdConfigModel): BannerAdTypeConfig {
        return BannerAdTypeConfig(
            isEnableRetry = model.isEnableRetry ?: ConfigParam.RETRY_IS_ENABLE_RETRY,
            maxRetryCount = model.maxRetryCount ?: ConfigParam.RETRY_MAX_RETRY_COUNT,
            isHideWhenError = model.isHideWhenError ?: ConfigParam.IS_HIDE_WHEN_ERROR,
            retryIntervalSecondList = model.retryIntervalSecondList ?: ConfigParam.RETRY_INTERVAL_LIST,
        )
    }

}