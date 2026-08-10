package com.core.config.domain.data

import com.core.config.data.model.OnBoardingConfigModel

data class OnBoardingConfig(
    val version: Int = ONBOARDING_VERSION_1,
    val positionNext: String = POSITION_NEXT_TOP,
    val isShowClose: Boolean = true,
    val isShowSwipe: Boolean = true,
    val delayShowCloseSwipeSeconds: Long = DEFAULT_DELAY_SHOW_CLOSE_SWIPE_SECONDS,
) {
    companion object {
        const val ONBOARDING_VERSION_1 = 1
        const val ONBOARDING_VERSION_2 = 2
        const val ONBOARDING_VERSION_3 = 3
        const val POSITION_NEXT_TOP = "top"
        const val POSITION_NEXT_TOP_V2 = "top_v2"
        const val POSITION_NEXT_BOTTOM = "bottom"
        const val DEFAULT_DELAY_SHOW_CLOSE_SWIPE_SECONDS = 1L

        fun from(model: OnBoardingConfigModel?): OnBoardingConfig {
            val positionNext = when (model?.positionNext?.lowercase()) {
                POSITION_NEXT_BOTTOM -> POSITION_NEXT_BOTTOM
                POSITION_NEXT_TOP -> POSITION_NEXT_TOP
                POSITION_NEXT_TOP_V2 -> POSITION_NEXT_TOP_V2
                else -> POSITION_NEXT_TOP
            }
            return OnBoardingConfig(
                version = model?.version ?: ONBOARDING_VERSION_1,
                positionNext = positionNext,
                isShowClose = model?.isShowClose ?: true,
                isShowSwipe = model?.isShowSwipe ?: true,
                delayShowCloseSwipeSeconds = model?.delayShowCloseSwipeSeconds
                    ?: DEFAULT_DELAY_SHOW_CLOSE_SWIPE_SECONDS,
            )
        }
    }
}
