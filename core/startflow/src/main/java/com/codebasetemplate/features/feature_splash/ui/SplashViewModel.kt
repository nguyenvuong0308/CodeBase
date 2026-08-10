package com.codebasetemplate.features.feature_splash.ui

import androidx.lifecycle.ViewModel
import com.core.preference.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    val showRequireTurnOnNetworkWhenRetryClickedFlow = MutableSharedFlow<Boolean>()

    var needHandleEventWhenResume = false

    var isActivityResume = false

    var isRequestEuConsentComplete = false

    var isRemoteConfigReady = false

    var isSplashAdsFlowStarted = false

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
        isAppOpenAdLoaded = true
        isAdNotValidOrLoadFailed = false
        isAppOpenAdShowing = false
        isAppOpenAdDismissed = false
    }

    fun handleWhenAdNotValidOrLoadFailed() {
        isAdNotValidOrLoadFailed = true
        isAppOpenAdShowing = false
        isAppOpenAdDismissed = false
    }

    fun handleWhenAdShowing() {
        isAdNotValidOrLoadFailed = false
        isAppOpenAdShowing = true
        isAppOpenAdDismissed = false
    }

    fun handleWhenAdDismissed() {
        isAppOpenAdDismissed = false
        isAppOpenAdShowing = false
        isAppOpenAdDismissed = true
    }
}
