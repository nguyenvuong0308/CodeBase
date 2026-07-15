package com.core.config.data.mapper

import com.core.config.data.model.TutorialConfigModel
import com.core.config.domain.data.TutorialConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TutorialConfigModelMapper @Inject constructor(
) : ModelMapper<TutorialConfigModel, TutorialConfig> {

    override fun toData(model: TutorialConfigModel): TutorialConfig {
        return TutorialConfig(
            enableAllAds = model.enableAllAds ?: true,
            enableAd1 = model.enableAd1 ?: true,
            enableAd2 = model.enableAd2 ?: true,
        )
    }
}
