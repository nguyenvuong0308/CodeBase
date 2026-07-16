package com.codebasetemplate.features.feature_onboarding.ui.v2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.codebasetemplate.core.base_ui.CoreActivity
import com.codebasetemplate.databinding.CoreActivityOnboardingBinding
import com.codebasetemplate.features.feature_onboarding.ui.adapter.OnBoardingPagerAdapter2
import com.codebasetemplate.features.feature_onboarding.ui.model.OnBoardingItem
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingEvent
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingViewModel
import com.codebasetemplate.features.main.ui.MainActivityHost
import com.codebasetemplate.required.ads.AppAdPlaceName
import com.codebasetemplate.required.shortcut.AppShortCut
import com.core.ads.BaseAdmobApplication
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.analytics.AnalyticsEvent
import com.core.baseui.ext.collectFlowOn
import com.core.config.domain.data.AppConfig.Companion.DEFINE_INTRO_FULL_AD
import com.core.config.domain.data.AppConfig.Companion.DEFINE_INTRO_HAVE_ADS
import com.core.config.domain.data.AppConfig.Companion.DEFINE_INTRO_NO_ADS
import com.core.config.domain.data.IAdPlaceName
import com.core.utilities.getStatusBarHeight
import com.core.utilities.gone
import com.core.utilities.setCurrentItemFixCrash
import com.core.utilities.visibleIf
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnBoardingActivity2 : CoreActivity<CoreActivityOnboardingBinding>() {
    override val isHideStatusBar: Boolean
        get() = true

    override val isSpaceStatusBar: Boolean
        get() = false

    override val isSpaceDisplayCutout: Boolean
        get() = false


    private val sharedViewModel: OnBoardingViewModel by viewModels()

    override fun bindingProvider(inflater: LayoutInflater): CoreActivityOnboardingBinding {
        return CoreActivityOnboardingBinding.inflate(inflater)
    }

    private val targetScreenFromShortCut by lazy {
        intent.extras?.getString(AppShortCut.KEY_SHORTCUT_TARGET_SCREEN, "")
    }


    private val introData by lazy {
        remoteConfigRepository.getAppConfig().introDataV2.takeIf { it.isNotEmpty() } ?: arrayListOf(
            DEFINE_INTRO_HAVE_ADS,
            DEFINE_INTRO_HAVE_ADS,
            DEFINE_INTRO_HAVE_ADS
        )
    }

    val itemsOnboarding = ArrayList<OnBoardingItem>()

    override fun initViews(savedInstanceState: Bundle?) {
        itemsOnboarding.apply {
            var indexIntro = 0
            introData.forEachIndexed { index, defineIntro ->

                when (defineIntro) {
                    DEFINE_INTRO_HAVE_ADS -> {
                        add(
                            OnBoardingItem.Item(
                                position = indexIntro,
                                isShowAds = !purchasePreferences.isUserVip(),
                                isPageEnd = false
                            )
                        )
                        indexIntro++
                    }

                    DEFINE_INTRO_NO_ADS -> {
                        add(
                            OnBoardingItem.Item(
                                position = indexIntro,
                                isShowAds = false,
                                isPageEnd = false
                            )
                        )
                        indexIntro++
                    }

                    DEFINE_INTRO_FULL_AD -> {
                        if (!adsManager.isNotAbleToVisibleAdsToUser(AppAdPlaceName.ANCHORED_FULL_ONBOARDING_v2)) {
                            add(
                                OnBoardingItem.FullNativeItem()
                            )
                        }
                    }
                }
            }
            itemsOnboarding.lastOrNull { it is OnBoardingItem.Item }?.isPageEnd = true
        }

        super.initViews(savedInstanceState)
        val adapter = OnBoardingPagerAdapter2(
            supportFragmentManager,
            this.lifecycle,
            itemsOnboarding
        )

        val params = viewBinding.layoutToolbar.layoutParams as ViewGroup.MarginLayoutParams
        params.topMargin = getStatusBarHeight()
        viewBinding.layoutToolbar.layoutParams = params


        viewBinding.run {
            viewPager.isUserInputEnabled = false
            viewPager.adapter = adapter
            viewPager.offscreenPageLimit = adapter.itemCount
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val itemOnBoarding = itemsOnboarding[position]
                    layoutAds.visibleIf(itemOnBoarding.isShowAds && !purchasePreferences.isUserVip())
                }
            })
        }
    }

    override fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
        super.onBannerNativeResult(adResource)
        if (adResource.commonAdPlaceName == AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM_v2) {
            when (adResource) {
                is AdLoadBannerNativeUiResource.Loading -> {
                    viewBinding.layoutBannerNative.setAdSize(
                        adResource.adType,
                        adResource.bannerSize,
                        adResource.nativeTemplateSize
                    )
                    val isShowAds =
                        (itemsOnboarding[viewBinding.viewPager.currentItem]).isShowAds
                    if (isShowAds) {
                        viewBinding.layoutBannerNative.visibleIf(!purchasePreferences.isUserVip())
                    }
                }

                is AdLoadBannerNativeUiResource.AdFailed -> {
                    viewBinding.layoutBannerNative.gone()
                }

                is AdLoadBannerNativeUiResource.BannerAdLoaded -> {
                    val isShowAds =
                        (itemsOnboarding[viewBinding.viewPager.currentItem]).isShowAds
                    viewBinding.layoutBannerNative.onAdLoaded(adResource.bannerAd)
                    if (isShowAds) {
                        viewBinding.layoutBannerNative.visibleIf(!purchasePreferences.isUserVip())
                    }
                }

                is AdLoadBannerNativeUiResource.NativeAdLoaded -> {
                    val isShowAds = (itemsOnboarding[viewBinding.viewPager.currentItem]).isShowAds
                    viewBinding.layoutBannerNative.onAdLoaded(
                        adResource.nativeAd,
                        adResource.nativeAdPlace
                    )
                    if (!isShowAds) {
                        viewBinding.layoutBannerNative.visibleIf(!purchasePreferences.isUserVip())
                    }
                }

                is AdLoadBannerNativeUiResource.AdNetworkError -> {
                    /*if(isHideNativeBannerWhenNetworkError) {
                        binding.layoutBannerNative.gone()
                    }*/
                }
            }
        }
    }

    override fun handleObservable() {
        super.handleObservable()

        collectFlowOn(sharedViewModel.navigateToFlow) { event ->
            when (event) {
                OnBoardingEvent.BackEvent -> {

                }

                OnBoardingEvent.NextEvent, is OnBoardingEvent.NextAction -> {
                    viewBinding.viewPager.setCurrentItemFixCrash(
                        viewBinding.viewPager.currentItem + 1,
                        true
                    )
                }

                OnBoardingEvent.FinishStep, is OnBoardingEvent.FinishAction -> {
                    if (BaseAdmobApplication.isFirstSaveLanguage) {
                        BaseAdmobApplication.isFirstSaveLanguage = false
                        analyticsManager.logEvent(AnalyticsEvent.EVENT_ACTION_PASS_INTRO)
                    }
                    showInterAd(
                        AppAdPlaceName.ACTION_NEXT_IN_INTRODUCTION
                    ) {
                        openMain()
                    }
                }
            }
        }

    }

    override fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> {
        return listOf(AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM_v2)
    }

    override fun providerInterAdPlaceName(): List<IAdPlaceName> {
        return listOf(
            AppAdPlaceName.ACTION_NEXT_IN_INTRODUCTION,
            AppAdPlaceName.ACTION_SKIP_IN_INTRODUCTION
        )
    }

    override fun onDestroy() {
        adsManager.releaseBannerNative(AppAdPlaceName.ANCHORED_FULL_ONBOARDING_v2)
        super.onDestroy()
    }

    private fun openMain() {
        val intent = Intent(this, MainActivityHost::class.java)
        val bundle = Bundle().apply {
            putString(AppShortCut.KEY_SHORTCUT_TARGET_SCREEN, targetScreenFromShortCut)
        }
        intent.putExtras(bundle)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        this.startActivity(intent)
    }

    override fun setupAfterOnBackPressed() {
        // nothing
    }
}
