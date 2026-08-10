package com.codebasetemplate.features.feature_onboarding.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.codebasetemplate.features.feature_onboarding.ui.model.OnBoardingItem
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingFragment
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingFullNativeFragment

class OnBoardingPagerAdapter(
    fm: FragmentManager,
    lifecycle: Lifecycle,
    var items: List<OnBoardingItem>
) : FragmentStateAdapter(fm, lifecycle) {


    override fun getItemCount(): Int {
        return items.size
    }


    override fun createFragment(position: Int): Fragment {
        val item = items[position]
        return if(item is OnBoardingItem.FullNativeItem) {
            OnBoardingFullNativeFragment.Companion.newInstance()
        } else {
            OnBoardingFragment.Companion.newInstance((item as OnBoardingItem.Item).position)
        }
    }
}