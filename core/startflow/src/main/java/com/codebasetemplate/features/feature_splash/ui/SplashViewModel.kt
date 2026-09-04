package com.codebasetemplate.features.feature_splash.ui

import androidx.lifecycle.ViewModel
import com.core.preference.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BaseSplashViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    internal val networkGate = SplashNetworkGate()

    var needHandleEventWhenResume = false

    var isActivityResume = false

    var isRequestEuConsentComplete = false

    var isRemoteConfigReady = false

    var isSplashAdsFlowStarted = false

    var isSplashAdLoadRequested = false

    var isAppOpenAdLoaded = false

    var isAppOpenAdShowing = false

    var isAppOpenAdDismissed = false

    var isAdNotValidOrLoadFailed = false

    var currentProgress: Long = 0

    var isTimerComplete = false

    var maxProgress = 0L

    var timeSkipAppOpenAdWhenNotAvailable = 0L

    val isFirstOpenApp by lazy {
        appPreferences.openAppCount == 1
    }

    init {
        appPreferences.openAppCount++
    }

    fun handleWhenAdLoaded() {
        isSplashAdLoadRequested = false
        isAppOpenAdLoaded = true
        isAdNotValidOrLoadFailed = false
        isAppOpenAdShowing = false
        isAppOpenAdDismissed = false
    }

    fun handleWhenAdNotValidOrLoadFailed() {
        isSplashAdLoadRequested = false
        isAdNotValidOrLoadFailed = true
        isAppOpenAdShowing = false
        isAppOpenAdDismissed = false
    }

    fun handleWhenAdShowing() {
        isSplashAdLoadRequested = false
        isAdNotValidOrLoadFailed = false
        isAppOpenAdShowing = true
        isAppOpenAdDismissed = false
    }

    fun handleWhenAdDismissed() {
        isSplashAdLoadRequested = false
        isAppOpenAdDismissed = false
        isAppOpenAdShowing = false
        isAppOpenAdDismissed = true
    }

    fun prepareForSplashAdRetry() {
        isSplashAdLoadRequested = true
        isAdNotValidOrLoadFailed = false
        isAppOpenAdLoaded = false
    }
}
