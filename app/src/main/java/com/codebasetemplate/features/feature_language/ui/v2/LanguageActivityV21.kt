package com.codebasetemplate.features.feature_language.ui.v2

import android.content.Context
import android.content.Intent
import com.core.config.domain.data.IAdPlaceName
import com.codebasetemplate.features.feature_language.ui.v2.adapter.LanguageGroup
import com.codebasetemplate.features.feature_onboarding.ui.helper.OnBoardingConfigFactory
import com.codebasetemplate.required.ads.AppAdPlaceName
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LanguageActivityV21 : BaseLanguageActivityV2FlowActivity() {

    override val languageNativeAdPlaceName: IAdPlaceName
        get() = AppAdPlaceName.ANCHORED_CHANGE_LANGUAGE_V2_NATIVE_1

    override val trackingViewEventName: String
        get() = "lfo1_view"

    override val trackingCompleteEventName: String
        get() = "lfo1_complete"

    override fun onGroupClick(group: LanguageGroup): Boolean {
        logTrackingComplete()
        val nextIntent = LanguageActivityV22.intentStart(
            context = this,
            selectedGroupId = group.id,
            fromSetting = isFromSetting,
            fromSplash = isOpenFromSlash,
            fromIntroduction = backFromIntroduction
        )
        startStep(nextIntent)
        return true
    }

    override fun onApplyClicked() = Unit

    override fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> {
        return buildList {
            add(languageNativeAdPlaceName)
            if ((isOpenFromSlash || backFromIntroduction) && isEnableIntroductionScreen) {
                addAll(
                    OnBoardingConfigFactory.getOnBoardingAdPlaceName(
                        getDataFromRemoteUseCase.onBoardingConfig,
                        remoteConfigRepository.getAppConfig()
                    )
                )
            }
        } + AppAdPlaceName.ANCHORED_CHANGE_LANGUAGE_V2_NATIVE_2 + AppAdPlaceName.ANCHORED_CHANGE_LANGUAGE_V2_NATIVE_3
    }

    companion object {
        fun intentStart(
            context: Context,
            fromSetting: Boolean = false,
            fromSplash: Boolean = false,
            fromIntroduction: Boolean = false,
        ): Intent {
            return LanguageV2FlowArgs.buildIntent(
                context = context,
                target = LanguageActivityV21::class.java,
                fromSetting = fromSetting,
                fromSplash = fromSplash,
                fromIntroduction = fromIntroduction
            )
        }
    }
}
