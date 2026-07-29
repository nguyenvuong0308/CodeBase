package com.core.ads.model

import android.os.SystemClock
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.core.config.domain.data.AdPlace
import java.util.Date


sealed class AdHolder {
    abstract var adPlace: AdPlace
    var isLoading: Boolean = false
    var isShowing: Boolean = false
    var isWaitLoadToShow: Boolean = false
    var retryCount: Int = 0
    var needRetry: Boolean = true

    abstract fun reset()

    abstract fun isAdLoaded(): Boolean
}

internal data class RewardedAdHolder(
    override var adPlace: AdPlace,
    var rewardedAd: RewardedAd? = null,
    var isEarnedReward: Boolean = false,
    var amount: Int = 0,
): AdHolder() {
    override fun reset() {
        isLoading = false
        isWaitLoadToShow = false
        rewardedAd = null
        isEarnedReward = false
        amount = 0
        retryCount = 0
        needRetry = true
    }

    override fun isAdLoaded() = rewardedAd != null
}

internal data class RewardedInterstitialAdHolder(
    override var adPlace: AdPlace,
    var rewardedInterstitialAd: RewardedInterstitialAd? = null,
    var isEarnedReward: Boolean = false,
    var amount: Int = 0,
): AdHolder() {
    override fun reset() {
        isLoading = false
        isWaitLoadToShow = false
        rewardedInterstitialAd = null
        isEarnedReward = false
        amount = 0
        retryCount = 0
        needRetry = true
    }

    override fun isAdLoaded() = rewardedInterstitialAd != null
}

internal data class InterstitialAdHolder(
    override var adPlace: AdPlace,
    var interstitialAd: InterstitialAd? = null,
    var fragmentManager: FragmentManager? = null,
    var identifier: String = "",
): AdHolder() {
    override fun reset() {
        isLoading = false
        isWaitLoadToShow = false
        interstitialAd = null
        fragmentManager = null
        identifier = ""
        retryCount = 0
        needRetry = true
    }

    override fun isAdLoaded() = interstitialAd != null
}

internal data class BannerAdHolder(
    override var adPlace: AdPlace,
    var bannerAd: AdView? = null,
    var identifier: String? = null,
): AdHolder() {
    override fun reset() {
        isLoading = false
        isWaitLoadToShow = false
        runCatching {
            val parentAdView = bannerAd?.parent
            if (parentAdView != null) {
                (parentAdView as ViewGroup).endViewTransition(bannerAd)
                parentAdView.layoutTransition = null
                parentAdView.removeView(bannerAd)
            }
        }
        // Calling this method might cause a crash in admob built-in classes:
        // java.lang.IllegalStateException: The specified child already has a parent
//        bannerAd?.destroy()
        bannerAd = null
        retryCount = 0
//        needRetry = true // native don't need reset this field
    }

    override fun isAdLoaded() = bannerAd != null
}

data class NativeAdHolder(
    override var adPlace: AdPlace,
    var nativeAd: NativeAd? = null,
    var loadedAtMs: Long = 0L,
): AdHolder() {
    override fun reset() {
        isLoading = false
        isWaitLoadToShow = false
        clearNativeAd()
        retryCount = 0
//        needRetry = true // Quyền retry của native do flow gọi load quyết định.
    }

    fun clearNativeAd() {
        nativeAd?.destroy()
        nativeAd = null
        loadedAtMs = 0L
    }

    fun isAdExpired(ttlMs: Long): Boolean {
        return SystemClock.elapsedRealtime() - loadedAtMs > ttlMs
    }

    override fun isAdLoaded() = nativeAd != null
}

data class AppOpenAdHolder(
    override var adPlace: AdPlace,
    var appOpenAd: AppOpenAd? = null,
    var loadTime: Long = 0L
): AdHolder() {
    override fun reset() {
        isLoading = false
        isWaitLoadToShow = false
        appOpenAd = null
        retryCount = 0
    }

    override fun isAdLoaded() = appOpenAd != null

    fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour = 3600000L
        return dateDifference < numMilliSecondsPerHour * numHours
    }
}
