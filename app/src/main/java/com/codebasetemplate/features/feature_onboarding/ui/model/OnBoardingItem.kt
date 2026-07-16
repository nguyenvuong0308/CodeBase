package com.codebasetemplate.features.feature_onboarding.ui.model

sealed class OnBoardingItem(var isShowAds: Boolean, var isPageEnd: Boolean, var position: Int) {
    class Item(
        isPageEnd: Boolean,
        position: Int,
        var realPosition: Int = 0,
        isShowAds: Boolean
    ) :
        OnBoardingItem(isShowAds, isPageEnd, position)

    class FullNativeItem(position: Int = 0) : OnBoardingItem(false, false, position)
}