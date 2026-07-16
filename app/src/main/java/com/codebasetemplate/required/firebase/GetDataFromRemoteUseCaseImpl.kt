package com.codebasetemplate.required.firebase

import com.core.config.data.RemoteConfigService
import com.core.config.domain.GetDataFromRemoteConfigUseCase
import com.core.utilities.util.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetDataFromRemoteUseCaseImpl @Inject constructor(): GetDataFromRemoteConfigUseCase {
    var onBoardingConfig: OnBoardingConfig = OnBoardingConfig()
    var languageActivityConfig: LanguageActivityConfig = LanguageActivityConfig()

    override fun invoke(remoteConfig: RemoteConfigService) {
        val onBoardingConfigModel = remoteConfig.fetchOtherConfig<OnBoardingConfigModel>("onboarding_config")
        onBoardingConfig = OnBoardingConfig.from(onBoardingConfigModel)
        val languageActivityConfigModel =
            remoteConfig.fetchOtherConfig<LanguageActivityConfigModel>("language_activity_config")
        languageActivityConfig = LanguageActivityConfig.from(languageActivityConfigModel)
        Timber.d("onBoardingConfig: $onBoardingConfig")
        Timber.d("languageActivityConfig: $languageActivityConfig")
    }
}
