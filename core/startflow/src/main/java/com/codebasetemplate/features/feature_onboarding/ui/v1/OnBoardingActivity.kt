package com.codebasetemplate.features.feature_onboarding.ui.v1

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.core.startflow.StartFlowActivity
import com.core.startflow.databinding.CoreActivityOnboardingBinding
import com.codebasetemplate.features.feature_onboarding.ui.adapter.OnBoardingPagerAdapter
import com.codebasetemplate.features.feature_onboarding.ui.model.OnBoardingItem
import com.core.config.domain.data.CoreAdPlaceName
import com.core.startflow.OnBoardingConfigFactory
import com.core.startflow.StartFlowNavigator
import com.core.startflow.StartFlowShortcut
import com.core.ads.BaseAdmobApplication
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.analytics.AnalyticsEvent
import com.core.baseui.ext.collectFlowOn
import com.core.config.domain.data.AppConfig.Companion.DEFINE_INTRO_FULL_AD
import com.core.config.domain.data.AppConfig.Companion.DEFINE_INTRO_HAVE_ADS
import com.core.config.domain.data.AppConfig.Companion.DEFINE_INTRO_NO_ADS
import com.core.config.domain.data.IAdPlaceName
import com.core.startflow.onboarding.OnBoardingContentProvider
import com.core.startflow.onboarding.activeOnBoardingContentProvider
import com.core.utilities.getStatusBarHeight
import com.core.utilities.gone
import com.core.utilities.setCurrentItemFixCrash
import com.core.utilities.visibleIf
import com.codebasetemplate.util.EventTracking
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnBoardingActivity : StartFlowActivity<CoreActivityOnboardingBinding>() {
    override val isHideStatusBar: Boolean
        get() = true

    override val isSpaceStatusBar: Boolean
        get() = false

    override val isSpaceDisplayCutout: Boolean
        get() = false


    private val sharedViewModel: OnBoardingViewModel by viewModels()

    @Inject
    lateinit var startFlowNavigator: StartFlowNavigator

    @Inject
    lateinit var contentProviders: Set<@JvmSuppressWildcards OnBoardingContentProvider>

    override fun bindingProvider(inflater: LayoutInflater): CoreActivityOnboardingBinding {
        return CoreActivityOnboardingBinding.inflate(inflater)
    }

    private val targetScreenFromShortCut by lazy {
        intent.extras?.getString(StartFlowShortcut.KEY_SHORTCUT_TARGET_SCREEN, "")
    }


    private val introData by lazy {
        remoteConfigRepository.getAppConfig().introData.takeIf { it.isNotEmpty() }
            ?: List(contentProviders.activeOnBoardingContentProvider().introPageCount.coerceAtLeast(1)) {
                DEFINE_INTRO_HAVE_ADS
            }
    }

    val itemsOnboarding = ArrayList<OnBoardingItem>()
    private val viewedOnboardingScreens = mutableSetOf<Int>()
    private var currentTrackingPosition = 0
    private var currentTrackingStartedAtMs = 0L
    private var pendingSwipeNavigation = false

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
                        if (!adsManager.isNotAbleToVisibleAdsToUser(CoreAdPlaceName.ANCHORED_FULL_ONBOARDING)) {
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
        val adapter = OnBoardingPagerAdapter(
            supportFragmentManager,
            this.lifecycle,
            itemsOnboarding
        )

        val params = viewBinding.layoutToolbar.layoutParams as ViewGroup.MarginLayoutParams
        params.topMargin = getStatusBarHeight()
        viewBinding.layoutToolbar.layoutParams = params


        viewBinding.run {
            viewPager.adapter = adapter
            viewPager.offscreenPageLimit = adapter.itemCount
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrollStateChanged(state: Int) {
                    pendingSwipeNavigation = state == ViewPager2.SCROLL_STATE_DRAGGING
                }

                override fun onPageSelected(position: Int) {
                    if (position != currentTrackingPosition && pendingSwipeNavigation) {
                        logCurrentPageComplete(
                            toPosition = position,
                            actionMethod = EventTracking.VALUE_SWIPE
                        )
                    }
                    currentTrackingPosition = position
                    logPageView(position)
                    pendingSwipeNavigation = false

                    val itemOnBoarding = itemsOnboarding[position]
                    layoutAds.visibleIf(itemOnBoarding.isShowAds && !purchasePreferences.isUserVip())
                }
            })
            viewPager.post {
                if (currentTrackingStartedAtMs == 0L) {
                    currentTrackingPosition = viewPager.currentItem
                    logPageView(currentTrackingPosition)
                }
            }
        }
    }

    override fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
        super.onBannerNativeResult(adResource)
        if (adResource.commonAdPlaceName == CoreAdPlaceName.ANCHORED_ONBOARDING_BOTTOM) {
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
                    val actionMethod = if (event is OnBoardingEvent.NextAction) {
                        event.actionMethod
                    } else {
                        EventTracking.VALUE_CLICK
                    }
                    moveToNextOnboardingPage(actionMethod)
                }

                OnBoardingEvent.FinishStep, is OnBoardingEvent.FinishAction -> {
                    val actionMethod = if (event is OnBoardingEvent.FinishAction) {
                        event.actionMethod
                    } else {
                        EventTracking.VALUE_CLICK
                    }
                    finishOnboarding(actionMethod)
                }
            }
        }

    }

    private fun moveToNextOnboardingPage(actionMethod: String) {
        val nextPosition = viewBinding.viewPager.currentItem + 1
        pendingSwipeNavigation = false
        logCurrentPageComplete(
            toPosition = nextPosition,
            actionMethod = actionMethod
        )
        viewBinding.viewPager.setCurrentItemFixCrash(nextPosition, true)
    }

    private fun finishOnboarding(actionMethod: String) {
        logCurrentPageComplete(
            toPosition = null,
            actionMethod = actionMethod
        )
        if (BaseAdmobApplication.isFirstSaveLanguage) {
            BaseAdmobApplication.isFirstSaveLanguage = false
            analyticsManager.logEvent(AnalyticsEvent.EVENT_ACTION_PASS_INTRO)
        }
        showInterAd(
            CoreAdPlaceName.ACTION_NEXT_IN_INTRODUCTION
        ) {
            openMain()
        }
    }

    private fun logPageView(position: Int) {
        val item = itemsOnboarding.getOrNull(position) ?: return
        currentTrackingStartedAtMs = SystemClock.elapsedRealtime()
        when (item) {
            is OnBoardingItem.FullNativeItem -> {
                EventTracking.logEvent(EventTracking.EVENT_ONBOARD_INTER_VIEW)
            }

            is OnBoardingItem.Item -> {
                val screenNumber = item.position + 1
                val viewType = if (viewedOnboardingScreens.add(screenNumber)) {
                    EventTracking.VALUE_FIRST_VIEW
                } else {
                    EventTracking.VALUE_REVISIT
                }
                EventTracking.logEvent(
                    EventTracking.onboardingViewEvent(screenNumber),
                    Bundle().apply {
                        putString(EventTracking.PARAM_VIEW_TYPE, viewType)
                    }
                )
            }
        }
    }

    private fun logCurrentPageComplete(toPosition: Int?, actionMethod: String) {
        val fromPosition = currentTrackingPosition
        val item = itemsOnboarding.getOrNull(fromPosition) ?: return
        val nowMs = SystemClock.elapsedRealtime()
        when (item) {
            is OnBoardingItem.FullNativeItem -> {
                EventTracking.logEngagementComplete(
                    EventTracking.EVENT_ONBOARD_INTER_COMPLETE,
                    currentTrackingStartedAtMs,
                    nowMs
                )
            }

            is OnBoardingItem.Item -> {
                val direction = when {
                    toPosition == null -> EventTracking.VALUE_FORWARD
                    toPosition > fromPosition -> EventTracking.VALUE_FORWARD
                    else -> EventTracking.VALUE_BACKWARD
                }
                val screenNumber = item.position + 1
                EventTracking.logEvent(
                    EventTracking.onboardingCompleteEvent(screenNumber),
                    Bundle().apply {
                        putLong(
                            EventTracking.PARAM_ENGAGEMENT_TIME,
                            (nowMs - currentTrackingStartedAtMs).coerceAtLeast(0L)
                        )
                        putString(EventTracking.PARAM_ACTION_METHOD, actionMethod)
                        putString(EventTracking.PARAM_NAV_DIRECTION, direction)
                        putString(EventTracking.PARAM_TO_SCREEN, toScreenName(toPosition))
                    }
                )
            }
        }
    }

    private fun toScreenName(position: Int?): String {
        if (position == null || position !in itemsOnboarding.indices) {
            return EventTracking.VALUE_NEXT_SCREEN
        }
        return when (val item = itemsOnboarding[position]) {
            is OnBoardingItem.FullNativeItem -> "onb_inter"
            is OnBoardingItem.Item -> "onb${item.position + 1}"
        }
    }

    override fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> {
        return OnBoardingConfigFactory.getOnBoardingAdPlaceName(remoteConfigRepository.getOnBoardingConfig(), remoteConfigRepository.getAppConfig())
    }

    override fun providerInterAdPlaceName(): List<IAdPlaceName> {
        return listOf(
            CoreAdPlaceName.ACTION_NEXT_IN_INTRODUCTION,
            CoreAdPlaceName.ACTION_SKIP_IN_INTRODUCTION
        )
    }

    override fun onDestroy() {
        adsManager.releaseBannerNative(CoreAdPlaceName.ANCHORED_FULL_ONBOARDING)
        super.onDestroy()
    }

    private fun openMain() {
        val intent = Intent(this, startFlowNavigator.mainClass())
        val bundle = Bundle().apply {
            putString(StartFlowShortcut.KEY_SHORTCUT_TARGET_SCREEN, targetScreenFromShortCut)
        }
        intent.putExtras(bundle)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        this.startActivity(intent)
    }

    override fun setupAfterOnBackPressed() {
        // nothing
    }
}
