package com.codebasetemplate.required.firebase

data class OnBoardingConfig (
    var version: Int = ONBOARDING_VERSION_1,
    var positionNext: String = POSITION_NEXT_TOP,
    var isShowClose: Boolean = true,
    var isShowSwipe: Boolean = true,
    var delayShowCloseSwipeSeconds: Long = DEFAULT_DELAY_SHOW_CLOSE_SWIPE_SECONDS,
) {
    companion object {
        const val ONBOARDING_VERSION_1 = 1
        const val ONBOARDING_VERSION_2 = 2
        const val ONBOARDING_VERSION_3 = 3
        const val POSITION_NEXT_TOP = "top"
        const val POSITION_NEXT_BOTTOM = "bottom"
        const val DEFAULT_DELAY_SHOW_CLOSE_SWIPE_SECONDS = 1L

        fun from(model: OnBoardingConfigModel?): OnBoardingConfig {
            val version = model?.version ?: ONBOARDING_VERSION_1
            val positionNext = when (model?.positionNext?.lowercase()) {
                POSITION_NEXT_BOTTOM -> POSITION_NEXT_BOTTOM
                POSITION_NEXT_TOP -> POSITION_NEXT_TOP
                else -> POSITION_NEXT_TOP
            }
            return OnBoardingConfig(
                version = version,
                positionNext = positionNext,
                isShowClose = model?.isShowClose ?: true,
                isShowSwipe = model?.isShowSwipe ?: true,
                delayShowCloseSwipeSeconds = model?.delayShowCloseSwipeSeconds
                    ?: DEFAULT_DELAY_SHOW_CLOSE_SWIPE_SECONDS,
            )
        }
    }

}
