package com.codebasetemplate.features.feature_onboarding.ui.helper

import com.core.startflow.R

object OnBoardingConfigFactory {

    const val INTRO_PAGE_COUNT = 3

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
