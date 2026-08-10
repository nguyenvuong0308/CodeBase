package com.core.startflow.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.core.startflow.R

interface OnBoardingContentProvider {
    val introPageCount: Int

    @DrawableRes
    fun getImageResIntro(position: Int): Int

    @StringRes
    fun getStringIntro(position: Int): Int

    @StringRes
    fun getSubtitleIntro(position: Int): Int?
}

object DefaultOnBoardingContentProvider : OnBoardingContentProvider {
    override val introPageCount: Int = 3

    override fun getImageResIntro(position: Int): Int {
        return when (position) {
            0 -> R.drawable.intro_11
            1 -> R.drawable.intro_21
            2 -> R.drawable.intro_31
            else -> R.drawable.intro_31
        }
    }

    override fun getStringIntro(position: Int): Int {
        return when (position) {
            0 -> R.string.core_onboarding_title_1
            1 -> R.string.core_onboarding_title_2
            2 -> R.string.core_onboarding_title_3
            else -> R.string.core_onboarding_title_1
        }
    }

    override fun getSubtitleIntro(position: Int): Int? {
        return when (position) {
            0 -> R.string.core_onboarding_title_1
            1 -> R.string.core_onboarding_title_2
            2 -> R.string.core_onboarding_title_3
            else -> R.string.core_onboarding_title_1
        }
    }
}

fun Set<OnBoardingContentProvider>.activeOnBoardingContentProvider(): OnBoardingContentProvider {
    return firstOrNull() ?: DefaultOnBoardingContentProvider
}
