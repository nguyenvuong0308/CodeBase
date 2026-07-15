package com.codebasetemplate.features.feature_onboarding.ui.helper

import com.codebasetemplate.R
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingActivity
import com.codebasetemplate.features.feature_onboarding.ui.v2.OnBoardingActivity2
import com.codebasetemplate.required.ads.AppAdPlaceName
import com.codebasetemplate.required.firebase.OnBoardingConfig
import com.core.config.domain.data.AppConfig
import com.core.config.domain.data.IAdPlaceName

object OnBoardingConfigFactory {

    const val INTRO_PAGE_COUNT = 3


    fun getOnBoardingAdPlaceName(
        onBoardingConfig: OnBoardingConfig,
        appConfig: AppConfig
    ): List<IAdPlaceName> {
        return if (onBoardingConfig.version == OnBoardingConfig.ONBOARDING_VERSION_1) {
            mutableListOf<IAdPlaceName>().apply {
                add(AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM)
                if (appConfig.introData.contains(AppConfig.DEFINE_INTRO_FULL_AD)) {
                    add(AppAdPlaceName.ANCHORED_FULL_ONBOARDING)
                }
            }
        } else {
            mutableListOf<IAdPlaceName>().apply {
                add(AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM_v2)
                if (appConfig.introDataV2.contains(AppConfig.DEFINE_INTRO_FULL_AD)) {
                    add(AppAdPlaceName.ANCHORED_FULL_ONBOARDING_v2)
                }
            }
        }
    }

    fun getOnBoardingAnchorFullAdPlaceName(onBoardingConfig: OnBoardingConfig): IAdPlaceName {
        return if (onBoardingConfig.version == OnBoardingConfig.ONBOARDING_VERSION_1) {
            AppAdPlaceName.ANCHORED_FULL_ONBOARDING
        } else {
            AppAdPlaceName.ANCHORED_FULL_ONBOARDING_v2
        }
    }

    fun getOnBoardingClass(onBoardingConfig: OnBoardingConfig) =
        if (onBoardingConfig.version == OnBoardingConfig.ONBOARDING_VERSION_1) {
            OnBoardingActivity::class.java
        } else {
            OnBoardingActivity2::class.java
        }

    fun getImageResIntro(position: Int): Int {
        return when (position) {
            0 -> R.drawable.intro_11
            1 -> R.drawable.intro_21
            2 -> R.drawable.intro_31
            else -> R.drawable.intro_31
        }
    }

    fun getStringIntro(position: Int): Int {
        return when (position) {
            0 -> R.string.core_onboarding_title_1
            1 -> R.string.core_onboarding_title_2
            2 -> R.string.core_onboarding_title_3
            else -> R.string.core_onboarding_title_1
        }
    }

    fun getSubtitleIntro(position: Int): Int? {
        return when (position) {
            0 -> R.string.core_onboarding_title_1
            1 -> R.string.core_onboarding_title_2
            2 -> R.string.core_onboarding_title_3
            else -> R.string.core_onboarding_title_1
        }
    }

}
