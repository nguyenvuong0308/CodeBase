package com.codebasetemplate.features.feature_splash.ui

internal enum class SplashNetworkAction {
    None,
    ShowNetworkPrompt,
    StartPrerequisites,
    RetryPrerequisites,
    PauseAds,
    ResumeAds,
}

/**
 * Keeps network transitions idempotent across lifecycle restarts and configuration changes.
 * Android-specific side effects remain in [BaseSplashActivity].
 */
internal class SplashNetworkGate {
    var isWaitingForNetwork: Boolean = false
        private set

    private var isDataReady = false
    private var arePrerequisitesStarted = false
    private var isOfflineFallbackActive = false

    fun onDataReady(isNetworkAvailable: Boolean): SplashNetworkAction {
        if (isDataReady) return SplashNetworkAction.None
        isDataReady = true
        return if (isNetworkAvailable) {
            arePrerequisitesStarted = true
            isWaitingForNetwork = false
            SplashNetworkAction.StartPrerequisites
        } else {
            isWaitingForNetwork = true
            SplashNetworkAction.ShowNetworkPrompt
        }
    }

    fun onNetworkAvailabilityChanged(
        isNetworkAvailable: Boolean,
        shouldGuardAds: Boolean,
        isAdsFlowStarted: Boolean,
    ): SplashNetworkAction {
        if (!isDataReady) return SplashNetworkAction.None

        if (!isNetworkAvailable) {
            if (!shouldGuardAds || isWaitingForNetwork || isOfflineFallbackActive) {
                return SplashNetworkAction.None
            }
            isWaitingForNetwork = true
            return if (isAdsFlowStarted) {
                SplashNetworkAction.PauseAds
            } else {
                SplashNetworkAction.ShowNetworkPrompt
            }
        }

        isOfflineFallbackActive = false
        if (!arePrerequisitesStarted) {
            arePrerequisitesStarted = true
            isWaitingForNetwork = false
            return SplashNetworkAction.StartPrerequisites
        }

        if (!isWaitingForNetwork) return SplashNetworkAction.None
        isWaitingForNetwork = false
        return if (isAdsFlowStarted) {
            SplashNetworkAction.ResumeAds
        } else {
            SplashNetworkAction.RetryPrerequisites
        }
    }

    fun continueWithoutNetwork(isAdsFlowStarted: Boolean): SplashNetworkAction {
        isWaitingForNetwork = false
        isOfflineFallbackActive = true
        if (!arePrerequisitesStarted) {
            arePrerequisitesStarted = true
            return SplashNetworkAction.StartPrerequisites
        }
        return if (isAdsFlowStarted) {
            SplashNetworkAction.ResumeAds
        } else {
            SplashNetworkAction.RetryPrerequisites
        }
    }

    fun releaseNetworkWait() {
        isWaitingForNetwork = false
    }
}
