package com.codebasetemplate.features.feature_onboarding.ui.v3

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.core.ads.BaseAdmobApplication
import com.core.analytics.AnalyticsEvent
import com.core.baseui.ext.collectFlowOn
import com.core.config.domain.data.CoreAdPlaceName
import com.core.config.domain.data.IAdPlaceName
import com.core.utilities.setCurrentItemFixCrash
import com.core.startflow.StartFlowActivity
import com.core.startflow.StartFlowNavigator
import com.core.startflow.StartFlowShortcut
import com.core.startflow.databinding.ActivityOnboardingV3Binding
import com.core.startflow.OnBoardingConfigFactory
import com.codebasetemplate.features.feature_onboarding.ui.model.OnBoardingItem
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingEvent
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingViewModel
import com.codebasetemplate.util.EventTracking
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnBoardingActivityV3 : StartFlowActivity<ActivityOnboardingV3Binding>() {
    override val isHideStatusBar: Boolean
        get() = true

    override val isSpaceStatusBar: Boolean
        get() = false

    override val isSpaceDisplayCutout: Boolean
        get() = false


    private val sharedViewModel: OnBoardingViewModel by viewModels()

    override fun bindingProvider(inflater: LayoutInflater): ActivityOnboardingV3Binding {
        return ActivityOnboardingV3Binding.inflate(inflater)
    }

    private val targetScreenFromShortCut by lazy {
        intent.extras?.getString(StartFlowShortcut.KEY_SHORTCUT_TARGET_SCREEN, "")
    }

    @Inject
    lateinit var startFlowNavigator: StartFlowNavigator
    private val viewedOnboardingScreens = mutableSetOf<Int>()
    private var currentTrackingPosition = 0
    private var currentTrackingStartedAtMs = 0L
    private var pendingSwipeNavigation = false

    override fun initViews(savedInstanceState: Bundle?) {
        sharedViewModel.setup()

        super.initViews(savedInstanceState)
        val adapter = OnBoardingPagerAdapter3(
            supportFragmentManager,
            this.lifecycle,
            sharedViewModel.itemsOnboarding
        )


        viewBinding.run {
            viewPager.isUserInputEnabled = true
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

    override fun handleObservable() {
        super.handleObservable()

        collectFlowOn(sharedViewModel.navigateToFlow) { event ->
            when (event) {
                OnBoardingEvent.BackEvent -> {

                }

                OnBoardingEvent.NextEvent -> {
                    moveToNextOnboardingPage(EventTracking.VALUE_CLICK)
                }

                is OnBoardingEvent.NextAction -> {
                    moveToNextOnboardingPage(event.actionMethod)
                }

                OnBoardingEvent.FinishStep -> {
                    finishOnboarding(EventTracking.VALUE_CLICK)
                }

                is OnBoardingEvent.FinishAction -> {
                    finishOnboarding(event.actionMethod)
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
        val item = sharedViewModel.itemsOnboarding.getOrNull(position) ?: return
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
        val item = sharedViewModel.itemsOnboarding.getOrNull(fromPosition) ?: return
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
        if (position == null || position !in sharedViewModel.itemsOnboarding.indices) {
            return EventTracking.VALUE_NEXT_SCREEN
        }
        return when (val item = sharedViewModel.itemsOnboarding[position]) {
            is OnBoardingItem.FullNativeItem -> "onb_inter"
            is OnBoardingItem.Item -> "onb${item.position + 1}"
        }
    }

    override fun providerInterAdPlaceName(): List<IAdPlaceName> {
        return listOf(
            CoreAdPlaceName.ACTION_NEXT_IN_INTRODUCTION,
            CoreAdPlaceName.ACTION_SKIP_IN_INTRODUCTION
        )
    }

    override fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> {
        return OnBoardingConfigFactory.getOnBoardingAdPlaceName(
            remoteConfigRepository.getOnBoardingConfig(),
            remoteConfigRepository.getAppConfig()
        )
    }

    override fun onDestroy() {
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
