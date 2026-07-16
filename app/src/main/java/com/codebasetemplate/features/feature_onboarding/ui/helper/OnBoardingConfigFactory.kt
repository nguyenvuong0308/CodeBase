package com.codebasetemplate.features.feature_onboarding.ui.helper

import com.codebasetemplate.R
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingActivity
import com.codebasetemplate.features.feature_onboarding.ui.v2.OnBoardingActivity2
import com.codebasetemplate.features.feature_onboarding.ui.v3.OnBoardingActivityV3
import com.codebasetemplate.required.ads.AppAdPlaceName
import com.codebasetemplate.required.firebase.OnBoardingConfig
import com.core.config.domain.data.AppConfig
import com.core.config.domain.data.CoreAdPlaceName
import com.core.config.domain.data.IAdPlaceName

object OnBoardingConfigFactory {

    const val INTRO_PAGE_COUNT = 3


    fun getOnBoardingAdPlaceName(
        onBoardingConfig: OnBoardingConfig,
        appConfig: AppConfig
    ): List<IAdPlaceName> {
        return when (onBoardingConfig.version) {
            OnBoardingConfig.ONBOARDING_VERSION_1 -> {
                mutableListOf<IAdPlaceName>().apply {
                    add(AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM)
                    if (appConfig.introData.contains(AppConfig.DEFINE_INTRO_FULL_AD)) {
                        add(AppAdPlaceName.ANCHORED_FULL_ONBOARDING)
                    }
                }
            }

            OnBoardingConfig.ONBOARDING_VERSION_2 -> {
                mutableListOf<IAdPlaceName>().apply {
                    add(AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM_v2)
                    if (appConfig.introDataV2.contains(AppConfig.DEFINE_INTRO_FULL_AD)) {
                        add(AppAdPlaceName.ANCHORED_FULL_ONBOARDING_v2)
                    }
                }
            }

            else -> {
                mutableListOf<IAdPlaceName>().apply {
                    appConfig.introDataV3.forEachIndexed { index, type ->
                        val adPlaceName = getOnboardingV3AdPlaceName(index)
                        if(type != AppConfig.DEFINE_INTRO_NO_ADS) {
                            this.add(adPlaceName)
                        }
                    }
                }
            }
        }
    }

    fun getOnboardingV3AdPlaceNames(introductionPosition: Int): List<IAdPlaceName> {
        return when (introductionPosition) {
            0 -> {
                listOf(AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM_V3_1)
            }

            1 -> {
                listOf(AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM_V3_2)
            }

            2 -> {
                listOf(AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM_V3_3)
            }

            3 -> {
                listOf(AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM_V3_4)
            }

            4 -> {
                listOf(AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM_V3_5)
            }

            else -> {
                listOf()
            }
        }
    }

    fun getOnboardingV3AdPlaceName(introductionPosition: Int): IAdPlaceName {
        return when (introductionPosition) {
            0 -> AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM_V3_1
            1 -> AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM_V3_2
            2 -> AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM_V3_3
            3 -> AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM_V3_4
            4 -> AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM_V3_5
            else -> CoreAdPlaceName.NONE
        }
    }

    fun getOnBoardingAnchorFullAdPlaceName(onBoardingConfig: OnBoardingConfig): IAdPlaceName {
        return when (onBoardingConfig.version) {
            OnBoardingConfig.ONBOARDING_VERSION_1 -> AppAdPlaceName.ANCHORED_FULL_ONBOARDING
            OnBoardingConfig.ONBOARDING_VERSION_2 -> AppAdPlaceName.ANCHORED_FULL_ONBOARDING_v2
            else -> AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM_V3_3
        }
    }

    fun getOnBoardingClass(onBoardingConfig: OnBoardingConfig) =
        when (onBoardingConfig.version) {
            OnBoardingConfig.ONBOARDING_VERSION_1 -> OnBoardingActivity::class.java
            OnBoardingConfig.ONBOARDING_VERSION_2 -> OnBoardingActivity2::class.java
            else -> OnBoardingActivityV3::class.java
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
