package com.codebasetemplate.features.feature_onboarding.ui.model

sealed class OnBoardingItem(var isShowAds: Boolean, var isPageEnd: Boolean, var position: Int, var isFullAds: Boolean) {
    class Item(
        isPageEnd: Boolean,
        position: Int,
        var realPosition: Int = 0,
        isShowAds: Boolean,
        isFullAds: Boolean = false
    ) :
        OnBoardingItem(isShowAds, isPageEnd, position, isFullAds)

    class FullNativeItem(position: Int = 0) : OnBoardingItem(false, false, position, true)
}