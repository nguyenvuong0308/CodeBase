package com.core.ads.admob

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import com.core.ads.domain.AdOpenAdUiResource
import com.core.ads.domain.AdsManager
import com.core.ads.model.PreventShowManyInterstitialAds
import com.core.config.domain.RemoteConfigRepository
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.CoreAdPlaceName
import com.core.utilities.getCurrentTimeInSecond
import com.core.utilities.manager.isNetworkConnected
import com.core.utilities.removeDimForReopenApp
import com.core.utilities.showDimForReopenApp
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AppOpenAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val adManager: AdsManager,
    private val reOpenShowCondition: ReOpenShowCondition
) : LifecycleObserver, Application.ActivityLifecycleCallbacks {

    companion object {
        const val TAG = "AdmobManager"
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _adOpenAppFlow = MutableSharedFlow<AdOpenAdUiResource>()
    val adOpenAppFlow = _adOpenAppFlow.asSharedFlow()

    private var currentActivity: Activity? = null

    var isFirstOpenApp = true

    var skipAppReopenAds = false

    init {
        val app = (context as Application)
        app.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        if (isFirstOpenApp || !reOpenShowCondition.isCanShow()) {
            return
        }
        currentActivity?.let { activity ->
            applicationScope.launch {
                delay(remoteConfigRepository.getAppOpenAdConfig().timeMillisDelayBeforeShow)
                showAdIfAvailable(activity, CoreAdPlaceName.APP_REOPEN)
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        if (isFirstOpenApp) {
            return
        }
        if (adManager.isHasFullscreenAdShowing()) {
            return
        }
        if (skipAppReopenAds) {
            return
        }
        currentActivity?.let {
            fetchAd(it, CoreAdPlaceName.APP_REOPEN)
        }
    }

    fun setupDefaultValue() {
        isFirstOpenApp = true
        adManager.setupAppOpenAdDefaultValue()
    }

    fun fetchAd(activity: Activity, adPlaceName: IAdPlaceName) {
        if (adManager.isNotAbleToVisibleAdsToUser(adPlaceName)) {
            notifyAdNotValidOrLoadFailed(adPlaceName)
            return
        }
        val adPlace = remoteConfigRepository.getAdPlaceBy(adPlaceName)
        val adHolder = adManager.getOrCreateAppOpenAdHolderBy(adPlace)

        if (adHolder.isLoading) {
            return
        }
        if (adHolder.isAdAvailable()) {
            notifyAdOpenAppLoaded(adPlaceName)
            return
        }
        adHolder.isLoading = true
        val loadCallback = object : AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                super.onAdLoaded(ad)
                Log.i(TAG, "AppOpenAd loaded $adPlaceName")
                adHolder.isLoading = false
                adHolder.appOpenAd = ad
                ad.setImmersiveMode(true)
                adHolder.loadTime = Date().time
                notifyAdOpenAppLoaded(adPlaceName)
                if (adHolder.isWaitLoadToShow) {
                    showAdIfAvailable(activity, adPlaceName)
                    adHolder.isWaitLoadToShow = false
                }
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                adHolder.isLoading = false
                val maxRetryCount = remoteConfigRepository.getSplashScreenConfig().maxRetryCount
                val retryFixedDelay = remoteConfigRepository.getSplashScreenConfig().retryFixedDelay
                when {
                    (adHolder.retryCount in 0 until maxRetryCount) && remoteConfigRepository.getSplashScreenConfig().isEnableRetry -> {
                        adHolder.retryCount++
                        Log.i(TAG, "AppOpenAd retry load ${adHolder.retryCount} $adPlaceName")
                        applicationScope.launch {
                            delay(retryFixedDelay)
                            fetchAd(activity, adPlaceName)
                        }
                    }

                    else -> {
                        Log.i(TAG, "AppOpenAd load failed $adPlaceName")
                        notifyAdNotValidOrLoadFailed(adPlaceName)
                        adHolder.reset()
                    }
                }
            }
        }
        Log.i(TAG, "AppOpenAd start load $adPlaceName")
        AppOpenAd.load(
            context,
            adHolder.adPlace.adId,
            adManager.getAdRequest(),
            loadCallback
        )
    }

    fun showAdIfAvailable(activity: Activity, adPlaceName: IAdPlaceName) {
        if (adManager.isNotAbleToVisibleAdsToUser(adPlaceName) || adManager.isHasFullscreenAdShowing()) {
            notifyAdNotValidOrLoadFailed(adPlaceName)
            return
        }
        if (skipAppReopenAds) return

        val adPlace = remoteConfigRepository.getAdPlaceBy(adPlaceName)
        val adHolder = adManager.getOrCreateAppOpenAdHolderBy(adPlace)

        if (!adHolder.isShowing && adHolder.isAdAvailable()) {
            if (!isTimeAvailableShowAds()) {
                notifyAdNotValidOrLoadFailed(adPlaceName)
                return
            }

            val fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    super.onAdShowedFullScreenContent()
                    Log.i(TAG, "AppOpenAd dismissed $adPlaceName")
                    PreventShowManyInterstitialAds.updateLastTimeShowedAppOpenAd()
                    activity.removeDimForReopenApp()
                    adHolder.isShowing = false
                    adHolder.reset()
                    notifyAdOpenAppDismissed(adPlaceName)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    super.onAdShowedFullScreenContent()
                    Log.i(TAG, "AppOpenAd failed to show $adPlaceName")
                    Firebase.crashlytics.log("AppOpenAd failed to show $adPlaceName: ${adError.message}")
                    adHolder.isShowing = false
                    adHolder.reset()
                    notifyAdNotValidOrLoadFailed(adPlaceName)
                }

                override fun onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent()
                    Log.i(TAG, "AppOpenAd showed $adPlaceName")
                    activity.showDimForReopenApp()
                    notifyAdOpenAppShowing(adPlaceName)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    adManager.increaseAdClickedCount()
                }
            }
            adHolder.isShowing = true

            Log.i(TAG, "AppOpenAd start show $adPlaceName")
            adHolder.appOpenAd?.fullScreenContentCallback = fullScreenContentCallback
            adHolder.appOpenAd?.show(activity)
        } else {
            if (context.isNetworkConnected()) {
                fetchAd(activity, adPlaceName)
            } else {
                notifyAdNotValidOrLoadFailed(adPlaceName)
            }
        }
    }

    private fun isTimeAvailableShowAds(): Boolean {
        val timeInterval =
            remoteConfigRepository.getAppOpenAdConfig().timeInterval
        val currentTimeInSecond = getCurrentTimeInSecond()
        return currentTimeInSecond - PreventShowManyInterstitialAds.getLastTimeShowedInterAd() >= timeInterval
                && currentTimeInSecond - PreventShowManyInterstitialAds.getLastTimeShowedAppOpenAd() >= timeInterval
    }

    private fun notifyAdOpenAppLoaded(adPlaceName: IAdPlaceName) {
        applicationScope.launch {
            _adOpenAppFlow.emit(AdOpenAdUiResource.AdLoaded(adPlaceName))
        }
    }

    private fun notifyAdOpenAppShowing(adPlaceName: IAdPlaceName) {
        applicationScope.launch {
            _adOpenAppFlow.emit(AdOpenAdUiResource.AdShowing(adPlaceName))
        }
    }

    private fun notifyAdNotValidOrLoadFailed(adPlaceName: IAdPlaceName) {
        applicationScope.launch {
            _adOpenAppFlow.emit(AdOpenAdUiResource.AdNotValidOrLoadFailed(adPlaceName))
        }
    }

    private fun notifyAdOpenAppDismissed(adPlaceName: IAdPlaceName) {
        applicationScope.launch {
            _adOpenAppFlow.emit(AdOpenAdUiResource.AdDismissed(adPlaceName))
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (!adManager.isHasAppOpenAdShowing()) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}


}