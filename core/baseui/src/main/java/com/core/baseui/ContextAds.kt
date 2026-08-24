package com.core.baseui

import android.app.Activity
import android.os.SystemClock
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import com.core.ads.domain.AdFullScreenUiResource
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.ads.domain.AdsManager
import com.core.baseui.ext.collectFlowOn
import com.core.config.domain.RemoteConfigRepository
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.NativeAdPlace
import com.core.utilities.isAppDebuggable
import com.core.utilities.manager.isNetworkConnected
import com.core.utilities.toast
import com.core.utilities.util.Timber
import com.core.utilities.util.postDelayLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.UUID
import kotlin.math.max

private const val TAG = "ContextAds"
private const val NATIVE_REFRESH_HOLDER_DELAY_MS = 2_000L

internal class NativeRefreshPauseController {
    var isPaused: Boolean = false
        private set

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    fun canRefresh(isWaitingLoad: Boolean, isLifecycleStarted: Boolean): Boolean {
        return !isPaused && !isWaitingLoad && isLifecycleStarted
    }
}

abstract class ContextAds(
    var adsManager: AdsManager,
    var remoteConfigRepository: RemoteConfigRepository,
    var lifecycleOwner: LifecycleOwner,
    var lifecycleScope: LifecycleCoroutineScope,
    val activity: Activity,
    var fragmentManager: FragmentManager,
    initInterstitialAdPlaceName: List<IAdPlaceName>,
    initRewardAdPlaceName: List<IAdPlaceName>,
    initBannerNativeAdPlaceName: List<IAdPlaceName>,
    initPreloadBannerNativeAdPlaceName: List<IAdPlaceName>,
    var identifier: String = UUID.randomUUID().toString(),
    var isWaitingLoad: Boolean = false
) {
    val activityRef = WeakReference<Activity>(activity)
    private var _retryLoadReward = 0
    private val _maxRetryLoadReward by lazy {
        remoteConfigRepository.getRewardedAdConfig().maxRetryOnContext
    }

    private val _timeWaitRetryOnContext by lazy {
        max(2000, remoteConfigRepository.getRewardedAdConfig().timeWaitRetryOnContext)
    }
    private var dialogLoadingAds: LoadingDialogFragment ?= null

    private fun showDialogLoadingAds() {
        if (!this@ContextAds.fragmentManager.isStateSaved) {
            dialogLoadingAds = LoadingDialogFragment()
            dialogLoadingAds?.show(this@ContextAds.fragmentManager, "Loading Ads")
        }
    }

    private fun dismissDialogLoadingAds() {
        if (!this@ContextAds.fragmentManager.isStateSaved) {
            dialogLoadingAds?.safeDismiss()
        }
    }

    private var adBannerOrNativeAll = mutableSetOf<IAdPlaceName>()
    private var adBannerOrNativePreload = mutableSetOf<IAdPlaceName>()
    private var adInterstitialLazyLoad = mutableSetOf<IAdPlaceName>()
    private var adInterstitialAll = mutableSetOf<IAdPlaceName>()
    private var adRewardLazyLoad = mutableSetOf<IAdPlaceName>()
    private var adRewardAll = mutableSetOf<IAdPlaceName>()
    private var adRewardWithoutAutoRetry = mutableSetOf<IAdPlaceName>()
    private val isAlwaysPreloadBannerNativeAdsWhenStart: Boolean by lazy {
        remoteConfigRepository.getAppConfig().isAlwaysPreloadBannerNativeAdsWhenStart
    }
    private var _isDisableAdDueManyClickFlow: Boolean? = null

    private var listHandleFullAds: HashMap<IAdPlaceName, (isShown: Boolean) -> Unit> = hashMapOf()
    private var listHandleRewardAds: HashMap<IAdPlaceName, (isShown: Boolean, isEarnedReward: Boolean, isNoAds: Boolean) -> Unit> =
        hashMapOf()
    private val bannerNativeRefreshJobs = mutableMapOf<IAdPlaceName, Job>()
    private val bannerNativeRefreshIntervals = mutableMapOf<IAdPlaceName, Int>()
    private val nativeRefreshLoadingStartedAtMs = mutableMapOf<IAdPlaceName, Long>()
    private val delayedNativeRefreshApplyJobs = mutableMapOf<IAdPlaceName, Job>()
    private val nativeRefreshPauseController = NativeRefreshPauseController()

    init {
        adBannerOrNativePreload.addAll(initPreloadBannerNativeAdPlaceName)
        adBannerOrNativeAll.addAll(initBannerNativeAdPlaceName)
        adInterstitialAll.addAll(initInterstitialAdPlaceName)
        adRewardAll.addAll(initRewardAdPlaceName)
        handleObservableAds()
    }

    fun ready() {
        isWaitingLoad = false
    }

    /**
     * Pauses future automatic native refresh requests for this context.
     * A refresh request that has already started is allowed to finish.
     */
    fun pauseNativeRefresh() {
        nativeRefreshPauseController.pause()
    }

    /**
     * Allows automatic native refresh requests again.
     * Lifecycle and loading-state restrictions are still applied.
     */
    fun resumeNativeRefresh() {
        nativeRefreshPauseController.resume()
    }

    fun isRewardReady(adPlaceName: IAdPlaceName): Boolean {
        return adsManager.isRewardReady(adPlaceName)
    }
    fun reinitAdPlaceName(
        initPreloadBannerNativeAdPlaceName: List<IAdPlaceName>? = null,
        initBannerNativeAdPlaceName: List<IAdPlaceName>? = null,
        initInterstitialAdPlaceName: List<IAdPlaceName>? = null,
        initRewardAdPlaceName: List<IAdPlaceName>? = null
    ) {
        initPreloadBannerNativeAdPlaceName?.let {
            adBannerOrNativePreload.clear()
            adBannerOrNativePreload.addAll(initPreloadBannerNativeAdPlaceName)
        }
        initBannerNativeAdPlaceName?.let {
            adBannerOrNativeAll.clear()
            adBannerOrNativeAll.addAll(initBannerNativeAdPlaceName)
        }

        initInterstitialAdPlaceName?.let {
            adInterstitialAll.clear()
            adInterstitialAll.addAll(initInterstitialAdPlaceName)
        }
        initRewardAdPlaceName?.let {
            adRewardAll.clear()
            adRewardAll.addAll(initRewardAdPlaceName)
        }
        syncNativeRefreshJobsWithAdPlaces()
    }

    fun onDestroy() {
        runCatching { dismissDialogLoadingAds() }
        if (!this@ContextAds.fragmentManager.isStateSaved) {
            this@ContextAds.fragmentManager.fragments.forEach { f ->
                when (f) {
                    is LoadingDialogFragment -> runCatching { f.dismissAllowingStateLoss() }
                    is RetryLoadRewardBottomSheetFragment -> runCatching { f.dismissAllowingStateLoss() }
                    is RequireTurnOnNetworkBottomSheetFragment -> runCatching { f.dismissAllowingStateLoss() }
                }
            }
        }
        listHandleFullAds.clear()
        listHandleRewardAds.clear()
        adBannerOrNativeAll.clear()
        adBannerOrNativePreload.clear()
        adInterstitialLazyLoad.clear()
        adInterstitialAll.clear()
        adRewardLazyLoad.clear()
        adRewardAll.clear()
        adRewardWithoutAutoRetry.clear()
        bannerNativeRefreshJobs.values.forEach { it.cancel() }
        bannerNativeRefreshJobs.clear()
        bannerNativeRefreshIntervals.clear()
        delayedNativeRefreshApplyJobs.values.forEach { it.cancel() }
        delayedNativeRefreshApplyJobs.clear()
        nativeRefreshLoadingStartedAtMs.clear()
        _retryLoadReward = 0
        _isDisableAdDueManyClickFlow = null
        activityRef.clear()
    }

    fun handleObservableAds() {
        collectFlowOn(
            lifecycleOwner = lifecycleOwner,
            lifecycleScope = lifecycleScope,
            stateFlow = adsManager.isDisableAdDueManyClickFlow,
            lifecycleState = Lifecycle.State.STARTED
        ) {
            if (isAlwaysPreloadBannerNativeAdsWhenStart) {
                preloadAds()
            } else {
                if (_isDisableAdDueManyClickFlow != it) {
                    Log.d(TAG, "handleObservable: isDisableAdDueManyClickFlow $it")
                    _isDisableAdDueManyClickFlow = it
                    preloadAds()
                } else {
                    preloadFullAds()
                }
            }
        }

        collectFlowOn(
            lifecycleOwner = lifecycleOwner,
            lifecycleScope = lifecycleScope,
            sharedFlow = adsManager.adFullScreenFlow
        ) { adResource ->
            handleFullAds(adResource)
        }

        collectFlowOn(
            lifecycleOwner = lifecycleOwner,
            lifecycleScope = lifecycleScope, sharedFlow = adsManager.adLoadBannerNativeFlow
        ) { adResource ->
            if (!handleNativeRefresh(adResource)) {
                onBannerNativeResult(adResource)
            }
        }

    }

    private fun handleNativeRefresh(adResource: AdLoadBannerNativeUiResource): Boolean {
        if (!isContextBannerNativePlace(adResource.commonAdPlaceName)) return false
        return when (adResource) {
            is AdLoadBannerNativeUiResource.NativeAdLoaded -> {
                startNativeRefreshIfNeed(adResource.adPlaceName)
                if (nativeRefreshLoadingStartedAtMs.containsKey(adResource.adPlaceName)) {
                    applyNativeRefreshAfterHolderDelay(adResource)
                    true
                } else {
                    false
                }
            }
            is AdLoadBannerNativeUiResource.AdFailed,
            is AdLoadBannerNativeUiResource.AdNetworkError -> {
                finishNativeRefreshTransition(adResource.commonAdPlaceName)
                false
            }
            else -> false
        }
    }

    private fun startNativeRefreshIfNeed(adPlaceName: IAdPlaceName) {
        if (!isContextBannerNativePlace(adPlaceName)) {
            cancelNativeRefresh(adPlaceName)
            return
        }

        val refreshTimeSecond = ((remoteConfigRepository.getAdPlaceBy(adPlaceName) as? NativeAdPlace)
            ?.refreshTimeSecond ?: 0).coerceAtLeast(0)
        if (refreshTimeSecond <= 0) {
            cancelNativeRefresh(adPlaceName)
            return
        }

        val currentJob = bannerNativeRefreshJobs[adPlaceName]
        if (currentJob?.isActive == true &&
            bannerNativeRefreshIntervals[adPlaceName] == refreshTimeSecond
        ) {
            return
        }

        currentJob?.cancel()
        bannerNativeRefreshIntervals[adPlaceName] = refreshTimeSecond
        bannerNativeRefreshJobs[adPlaceName] = lifecycleScope.launch {
            while (isActive) {
                delay(refreshTimeSecond.toLong() * 1_000L)
                if (!nativeRefreshPauseController.canRefresh(
                        isWaitingLoad = isWaitingLoad,
                        isLifecycleStarted = lifecycleOwner.lifecycle.currentState
                            .isAtLeast(Lifecycle.State.STARTED)
                    )
                ) {
                    continue
                }

                val activity = activityRef.get()
                if (activity == null || activity.isFinishing || activity.isDestroyed) {
                    cancelNativeRefresh(adPlaceName)
                    break
                }

                val currentRefreshTimeSecond =
                    ((remoteConfigRepository.getAdPlaceBy(adPlaceName) as? NativeAdPlace)
                        ?.refreshTimeSecond ?: 0).coerceAtLeast(0)
                if (currentRefreshTimeSecond <= 0) {
                    cancelNativeRefresh(adPlaceName)
                    break
                }
                if (currentRefreshTimeSecond != refreshTimeSecond) {
                    startNativeRefreshIfNeed(adPlaceName)
                    break
                }

                showNativeRefreshHolder(adPlaceName)
                adsManager.loadBannerNativeAd(
                    activity = activity,
                    adPlaceName = adPlaceName,
                    identifier = identifier,
                    isPreload = false,
                    isReload = true
                )
            }
        }
    }

    private fun showNativeRefreshHolder(adPlaceName: IAdPlaceName) {
        val nativeAdPlace = remoteConfigRepository.getAdPlaceBy(adPlaceName) as? NativeAdPlace
            ?: return
        nativeRefreshLoadingStartedAtMs[adPlaceName] = SystemClock.elapsedRealtime()
        delayedNativeRefreshApplyJobs.remove(adPlaceName)?.cancel()
        onBannerNativeResult(
            AdLoadBannerNativeUiResource.Loading(
                adPlaceName = adPlaceName,
                adType = nativeAdPlace.adType,
                bannerSize = com.core.config.domain.data.BannerSize.Anchored,
                nativeTemplateSize = nativeAdPlace.nativeTemplateSize
            )
        )
    }

    private fun applyNativeRefreshAfterHolderDelay(
        adResource: AdLoadBannerNativeUiResource.NativeAdLoaded
    ) {
        val adPlaceName = adResource.adPlaceName
        val loadingStartedAtMs = nativeRefreshLoadingStartedAtMs[adPlaceName]
            ?: SystemClock.elapsedRealtime()
        val delayMs = max(
            0L,
            NATIVE_REFRESH_HOLDER_DELAY_MS - (SystemClock.elapsedRealtime() - loadingStartedAtMs)
        )
        delayedNativeRefreshApplyJobs.remove(adPlaceName)?.cancel()
        delayedNativeRefreshApplyJobs[adPlaceName] = lifecycleScope.launch {
            delay(delayMs)
            finishNativeRefreshTransition(adPlaceName, cancelDelayedApply = false)
            if (isContextBannerNativePlace(adPlaceName) &&
                lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            ) {
                onBannerNativeResult(adResource)
            }
        }
    }

    private fun finishNativeRefreshTransition(
        adPlaceName: IAdPlaceName,
        cancelDelayedApply: Boolean = true
    ) {
        nativeRefreshLoadingStartedAtMs.remove(adPlaceName)
        val delayedApplyJob = delayedNativeRefreshApplyJobs.remove(adPlaceName)
        if (cancelDelayedApply) {
            delayedApplyJob?.cancel()
        }
    }

    private fun cancelNativeRefresh(adPlaceName: IAdPlaceName) {
        bannerNativeRefreshJobs.remove(adPlaceName)?.cancel()
        bannerNativeRefreshIntervals.remove(adPlaceName)
        finishNativeRefreshTransition(adPlaceName)
    }

    private fun isContextBannerNativePlace(adPlaceName: IAdPlaceName): Boolean {
        return adBannerOrNativeAll.contains(adPlaceName) ||
                adBannerOrNativePreload.contains(adPlaceName)
    }

    private fun syncNativeRefreshJobsWithAdPlaces() {
        val activeAdPlaceNames = adBannerOrNativeAll + adBannerOrNativePreload
        bannerNativeRefreshJobs.keys
            .filterNot { activeAdPlaceNames.contains(it) }
            .forEach { cancelNativeRefresh(it) }
    }

    fun loadBannerOrNativeAds(adPlaceName: IAdPlaceName, oneTimeLoad: Boolean, isReload: Boolean) {
        activityRef.get()?.let { activity ->
            adsManager.loadBannerNativeAd(
                activity,
                adPlaceName,
                identifier = identifier,
                isPreload = false,
                isReload = isReload
            )
            if (!oneTimeLoad) {
                adBannerOrNativeAll.add(adPlaceName)
            }
        }
    }

    fun loadInterstitialAds(adPlaceName: IAdPlaceName, oneTimeLoad: Boolean) {
        activityRef.get()?.let { activity ->
            adsManager.loadFullscreenAd(activity, adPlaceName, identifier)
            if (!oneTimeLoad) {
                adInterstitialAll.add(adPlaceName)
            }
            adInterstitialLazyLoad.add(adPlaceName)
        }
    }

    fun loadRewardAds(adPlaceName: IAdPlaceName, oneTimeLoad: Boolean) {
        activityRef.get()?.let { activity ->
            adsManager.loadFullscreenAd(activity, adPlaceName, identifier)
            if (!oneTimeLoad) {
                adRewardAll.add(adPlaceName)
            }
            adRewardLazyLoad.add(adPlaceName)
        }
    }


    abstract fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource)

    fun preloadAds() {
        preloadFullAds()
        preloadBannerNative()
    }

    private fun preloadFullAds() {
        if(isWaitingLoad) return
        activityRef.get()?.let { activity ->

            adInterstitialAll.forEach {
                adsManager.loadFullscreenAd(activity, it, identifier)
            }

            adRewardAll.forEach {
                adsManager.loadFullscreenAd(activity, it, identifier)
            }
        }

    }

    private fun preloadBannerNative() {
        if(isWaitingLoad) return
        activityRef.get()?.let { activity ->
            adBannerOrNativeAll.forEach {
                adsManager.loadBannerNativeAd(activity, it, identifier, false, isReload = false)
            }

            adBannerOrNativePreload.forEach {
                adsManager.loadBannerNativeAd(activity, it, identifier, true, isReload = false)
            }
        }
    }

    private fun setHandleFullAds(adPlaceName: IAdPlaceName, callback: (isShown: Boolean) -> Unit) {
        listHandleFullAds[adPlaceName] = callback
    }

    private fun setHandleRewardAds(
        adPlaceName: IAdPlaceName,
        callback: (isShown: Boolean, isEarnedReward: Boolean, isNoAds: Boolean) -> Unit
    ) {
        listHandleRewardAds[adPlaceName] = callback
    }

    private fun handleFullAds(
        adResource: AdFullScreenUiResource
    ) {
        activityRef.get()?.let { activityX ->
            if (adResource is AdFullScreenUiResource.AdCompleted) {
                (activityX as? FragmentActivity)?.runWhenResumed {
                    handleFullAdResult(adResource, activityX)
                } ?: run {
                    handleFullAdResult(adResource, activityX)
                }
            }
        }
    }

    private fun handleFullAdResult(
        adResource: AdFullScreenUiResource.AdCompleted,
        activityX: Activity
    ) {
        val adPlaceName = adResource.adPlaceName
        val isReward =
            adRewardAll.contains(adPlaceName) || adRewardLazyLoad.contains(adPlaceName)
        if (isReward) { // trường hợp ads reward
            if (!adResource.isShown && listHandleRewardAds[adPlaceName] != null) {
                if (adRewardWithoutAutoRetry.contains(adPlaceName)) {
                    listHandleRewardAds[adPlaceName]?.invoke(false, false, false)
                    listHandleRewardAds.remove(adPlaceName)
                    return
                }
                if (activityX.isNetworkConnected()) {
                    RetryLoadRewardBottomSheetFragment().apply {
                        onRetry = {
                            //Show loading
                            showDialogLoadingAds()
                            Timber.e("Retry load reward")
                            adsManager.loadFullscreenAd(
                                activityX,
                                adPlaceName,
                                identifier = identifier
                            )
                            _retryLoadReward++
                            postDelayLifecycle(_timeWaitRetryOnContext.toLong(), lifecycleOwner) {
                                Timber.e("Retry show reward")
                                dismissDialogLoadingAds()
                                //Dismiss loading
                                adsManager.showAd(activityX, fragmentManager = this@ContextAds.fragmentManager, adPlaceName = adPlaceName, identifier = identifier)
                            }
                        }
                        onCancel = {
                            if (_retryLoadReward < _maxRetryLoadReward){
                                listHandleRewardAds[adPlaceName]?.invoke(false, false, false)
                            } else {
                                listHandleRewardAds[adPlaceName]?.invoke(false, false, true)
                            }
                            listHandleRewardAds.remove(adPlaceName)
                        }

                        postDelayLifecycle(200, lifecycleOwner) {
                            if (isAdded) {
                                canRetry(_retryLoadReward < _maxRetryLoadReward)
                            }
                        }
                    }.show(this@ContextAds.fragmentManager, "Retry Reward Ads ${System.currentTimeMillis()}")
                } else {
                    showRequireTurnOnNetworkBottomSheetFragment(onRetry = {
                        showDialogLoadingAds()
                        this@ContextAds.apply {
                            adsManager.loadFullscreenAd(
                                activityX,
                                adPlaceName,
                                identifier = identifier
                            )
                            postDelayLifecycle(_timeWaitRetryOnContext.toLong(), lifecycleOwner) {
                                //Dismiss loading
                                dismissDialogLoadingAds()
                                adsManager.showAd(
                                    activityX,
                                    fragmentManager = this@ContextAds.fragmentManager,
                                    adPlaceName,
                                    identifier = identifier
                                )
                            }
                        }
                        //Show loading
                    }, onCancel = {
                        dismissDialogLoadingAds()
                        listHandleRewardAds[adPlaceName]?.invoke(false, false, false)
                        listHandleRewardAds.remove(adPlaceName)
                    })
                }
            } else {
                listHandleRewardAds[adPlaceName]?.invoke(
                    adResource.isShown,
                    adResource.isEarnedReward,
                    false
                )
                listHandleRewardAds.remove(adPlaceName)
            }
        } else {
            listHandleFullAds[adPlaceName]?.invoke(adResource.isShown)
            listHandleFullAds.remove(adPlaceName)
            Log.d(TAG, "handleFullAds: $adPlaceName")
        }
    }

    fun showInterAd(adPlaceName: IAdPlaceName, onHandleCompleted: ((isShown: Boolean) -> Unit)) {
        activityRef.get()?.let { activity ->
            if (activity.isAppDebuggable()) {
                if (!adInterstitialAll.contains(adPlaceName) && !adInterstitialLazyLoad.contains(
                        adPlaceName
                    )
                ) {
                    activity.toast("Vui lòng check lại danh sách adPlaceName trong hàm providerFullAdPlaceName ${adPlaceName.name}, hoặc gọi loadRewardAds")
                }
            }
            setHandleFullAds(adPlaceName, onHandleCompleted)
            adsManager.showAd(
                activity,
                fragmentManager = this@ContextAds.fragmentManager,
                adPlaceName = adPlaceName,
                identifier = identifier
            )
        }
    }

    fun showRewardAd(
        adPlaceName: IAdPlaceName,
        onHandleCompleted: ((isShown: Boolean, isEarnedReward: Boolean, isNoAds: Boolean) -> Unit),
        autoRetry: Boolean = true
    ) {
        if (!autoRetry) {
            adRewardWithoutAutoRetry.add(adPlaceName)
        } else {
            adRewardWithoutAutoRetry.remove(adPlaceName)
        }
        activityRef.get()?.let { activity ->
            if (activity.isAppDebuggable()) {
                if (!adRewardAll.contains(adPlaceName) && !adRewardLazyLoad.contains(adPlaceName)) {
                    activity.toast("Vui lòng check lại danh sách adPlaceName trong hàm providerRewardAdPlaceName ${adPlaceName.name}, hoặc gọi loadRewardAds")
                }
            }
            _retryLoadReward = 0
            setHandleRewardAds(adPlaceName, onHandleCompleted)
            adsManager.showAd(
                activity,
                fragmentManager = this@ContextAds.fragmentManager,
                adPlaceName = adPlaceName,
                identifier = identifier
            )
        }
    }

    private fun showRequireTurnOnNetworkBottomSheetFragment(
        onRetry: (() -> Unit),
        onCancel: (() -> Unit)? = null
    ) {
        RequireTurnOnNetworkBottomSheetFragment().apply {
            this.onRetry = onRetry
            this.onCancel = onCancel
        }.show(
            this@ContextAds.fragmentManager,
            RequireTurnOnNetworkBottomSheetFragment::class.java.simpleName + "${System.currentTimeMillis()}"
        )
    }

}

fun FragmentActivity.runWhenResumed(block: () -> Unit) {
    val fm = supportFragmentManager
    if (!fm.isStateSaved && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
        // Chạy ở "tick" kế tiếp để đảm bảo view đã attach sau onResume
        window?.decorView?.post { block() } ?: block()
    } else {
        val activity = this
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                activity.lifecycle.removeObserver(this)
                activity.window?.decorView?.post {
                    if (!activity.supportFragmentManager.isStateSaved &&
                        activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                    ) {
                        block()
                    } else {
                        // Nếu vẫn chưa an toàn, thử lại ở vòng resume kế tiếp
                        activity.runWhenResumed(block)
                    }
                }
            }
            override fun onDestroy(owner: LifecycleOwner) {
                activity.lifecycle.removeObserver(this)
            }
        })
    }
}

