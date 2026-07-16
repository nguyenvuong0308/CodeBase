package com.codebasetemplate.features.feature_onboarding.ui.v3

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.codebasetemplate.features.feature_onboarding.ui.model.OnBoardingItem
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingFullNativeFragment

class OnBoardingPagerAdapter3(
    fm: FragmentManager,
    lifecycle: Lifecycle,
    var items: List<OnBoardingItem>
) : FragmentStateAdapter(fm, lifecycle) {


    override fun getItemCount(): Int {
        return items.size
    }


    override fun createFragment(position: Int): Fragment {
        val item = items[position]
        return if (item is OnBoardingItem.FullNativeItem) {
            OnBoardingFullNativeFragmentV3.newInstance(item.position)
        } else {
            if (position == items.size - 1) {
                if(item.isShowAds) {
                    OnBoardingFragmentV3.newInstance(
                        position = (item as OnBoardingItem.Item).position,
                        isPageEnd = item.isPageEnd,
                        isShowAd = item.isShowAds,
                        realPosition = item.realPosition
                    )
                } else {
                    OnBoardingFragmentV3EndTab.newInstance((item as OnBoardingItem.Item).realPosition)
                }
            } else {
                OnBoardingFragmentV3.newInstance(
                    position = (item as OnBoardingItem.Item).position,
                    isPageEnd = item.isPageEnd,
                    isShowAd = item.isShowAds,
                    realPosition = item.realPosition
                )
            }
        }
    }
}