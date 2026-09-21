package com.core.ads.model

import android.os.SystemClock
import android.util.Log
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.core.config.domain.data.AdPlace
import java.util.Date


sealed class AdHolder {
    abstract var adPlace: AdPlace
    var isLoading: Boolean = false
    var isShowing: Boolean = false
    var isWaitLoadToShow: Boolean = false
    var retryCount: Int = 0
    var needRetry: Boolean = true

    // Accessed on Main only. Reset/release and a new load invalidate older SDK callbacks.
    internal var loadGeneration: Long = 0
        private set

    internal fun beginLoad(): Long {
        isLoading = true
        return ++loadGeneration
    }

    internal fun acceptsLoadCallback(generation: Long): Boolean =
        isLoading && loadGeneration == generation

    protected fun invalidateLoad() {
        loadGeneration++
        isLoading = false
    }

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
        invalidateLoad()
        isShowing = false
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
        invalidateLoad()
        isShowing = false
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
        invalidateLoad()
        isShowing = false
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
    var loadingBannerAd: AdView? = null

    fun destroyLoadingBanner() {
        val loading = loadingBannerAd
        loadingBannerAd = null
        loading?.destroySafely()
    }

    override fun reset() {
        invalidateLoad()
        isShowing = false
        isWaitLoadToShow = false
        val loaded = bannerAd
        bannerAd = null
        if (loadingBannerAd !== loaded) destroyLoadingBanner()
        loadingBannerAd = null
        loaded?.destroySafely()
        retryCount = 0
//        needRetry = true // native don't need reset this field
    }

    override fun isAdLoaded() = bannerAd != null
}

// Detach before destroying: the SDK may otherwise encounter an already-parented child.
internal fun AdView.destroySafely() {
    runCatching {
        (parent as? ViewGroup)?.let {
            it.endViewTransition(this)
            it.layoutTransition = null
            it.removeView(this)
        }
    }.onFailure { Log.w("AdHolder", "Unable to detach banner", it) }
    runCatching { destroy() }
        .onFailure { Log.w("AdHolder", "Unable to destroy banner", it) }
}

data class NativeAdHolder(
    override var adPlace: AdPlace,
    var nativeAd: NativeAd? = null,
    var loadedAtMs: Long = 0L,
): AdHolder() {
    // A fullscreen show can join a native request that is already in flight.
    internal var onFullscreenLoadFinished: ((Boolean) -> Unit)? = null

    internal fun finishFullscreenLoad(isLoaded: Boolean) {
        val callback = onFullscreenLoadFinished
        onFullscreenLoadFinished = null
        isWaitLoadToShow = false
        callback?.invoke(isLoaded)
    }

    override fun reset() {
        resetLoadState()
        isShowing = false
        clearNativeAd()
//        needRetry = true // Quyền retry của native do flow gọi load quyết định.
    }

    /**
     * Chỉ reset trạng thái load, giữ lại native đang cache. Dùng khi một lượt load thất bại nhưng
     * native cũ vẫn đang được container hiển thị: xoá nó sẽ làm hỏng quảng cáo đang trên màn hình.
     */
    fun resetLoadState() {
        invalidateLoad()
        retryCount = 0
        finishFullscreenLoad(false)
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
        invalidateLoad()
        isShowing = false
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
