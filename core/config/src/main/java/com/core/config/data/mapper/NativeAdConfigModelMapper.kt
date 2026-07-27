package com.core.config.data.mapper

import com.core.config.data.helper.ConfigParam
import com.core.config.data.model.NativeAdConfigModel
import com.core.config.domain.data.NativeAdTypeConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class NativeAdConfigModelMapper @Inject constructor(
): ModelMapper<NativeAdConfigModel, NativeAdTypeConfig> {

    override fun toData(model: NativeAdConfigModel): NativeAdTypeConfig {
        return NativeAdTypeConfig(
            isEnableRetry = model.isEnableRetry ?: ConfigParam.RETRY_IS_ENABLE_RETRY,
            maxRetryCount = model.maxRetryCount ?: ConfigParam.RETRY_MAX_RETRY_COUNT,
            isHideWhenError = model.isHideWhenError ?: ConfigParam.IS_HIDE_WHEN_ERROR,
            retryIntervalSecondList = model.retryIntervalSecondList ?: ConfigParam.RETRY_INTERVAL_LIST,
            expiredTimeSecond = model.expiredTimeSecond ?: ConfigParam.EXPIRED_NATIVE_TIME_DEFAULT,
        )
    }

}