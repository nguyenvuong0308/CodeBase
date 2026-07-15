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
import com.google.android.gms.ads.AdapterResponseInfo
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import com.core.ads.domain.AdOpenAdUiResource
import com.core.ads.domain.AdsManager
import com.core.ads.model.AppOpenAdHolder
import com.core.ads.model.PreventShowManyInterstitialAds
import com.core.analytics.AdjustAnalytics
import com.core.config.domain.RemoteConfigRepository
import com.core.config.domain.data.AdPlace
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
    private val reOpenShowCondition: ReOpenShowCondition,
    private val adjustAnalytics: AdjustAnalytics,
    private val reopenAction: ReopenAction
) : LifecycleObserver, Application.ActivityLifecycleCallbacks {

    companion object {
        const val TAG = "AdmobManager"

        // Safety net for a load that neither succeeds nor fails (e.g. Activity destroyed
        // mid-load), so waiting placements don't stay latched on isLoading = true forever.
        private const val APP_OPEN_LOAD_TIMEOUT_MS = 30_000L
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _adOpenAppFlow = MutableSharedFlow<AdOpenAdUiResource>()
    val adOpenAppFlow = _adOpenAppFlow.asSharedFlow()

    // Cache app-open ads by ad unit id so placements sharing the same high-floor id reuse
    // one load result instead of starting duplicate SDK requests.
    private val appOpenAdUnitHolderMap = mutableMapOf<String, AppOpenAdUnitHolder>()

    private var currentActivity: Activity? = null

    var isFirstOpenApp = true

    var skipAppReopenAds = false

    // Tracks the load state for one app-open ad unit, plus placements waiting for it.
    private data class AppOpenAdUnitHolder(
        var isLoading: Boolean = false,
        var appOpenAd: AppOpenAd? = null,
        var loadTime: Long = 0L,
        var loadId: Int = 0,
        val waiterActivities: MutableMap<IAdPlaceName, Activity> = linkedMapOf()
    ) {
        fun isAdAvailable(): Boolean {
            return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
        }

        private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
            val dateDifference = Date().time - loadTime
            val numMilliSecondsPerHour = 3600000L
            return dateDifference < numMilliSecondsPerHour * numHours
        }
    }

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
                if(!reopenAction.isCustomAction()) {
                    showAdIfAvailable(activity, CoreAdPlaceName.APP_REOPEN)
                } else {
                    if(!adManager.isHasFullscreenAdShowing()) {
                        reopenAction.reopenAction(activity)
                    }
                }
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

    fun fetchAd(activity: Activity, adPlaceName: IAdPlaceName, waterfallIndex: Int = 0) {
        if(reopenAction.isCustomAction() && adPlaceName == CoreAdPlaceName.APP_REOPEN) return

        if (adManager.isNotAbleToVisibleAdsToUser(adPlaceName)) {
            notifyAdNotValidOrLoadFailed(adPlaceName)
            return
        }
        val adPlace = remoteConfigRepository.getAdPlaceBy(adPlaceName)
        val adHolder = adManager.getOrCreateAppOpenAdHolderBy(adPlace)

        if (adHolder.isLoading) {
            return
        }
        if (adHolder.isAdAvailable() || findLoadedAppOpenAdUnitHolder(adHolder.adPlace) != null) {
            notifyAdOpenAppLoaded(adPlaceName)
            return
        }
        adHolder.isLoading = true
        val waterfallAdUnitIds = adHolder.adPlace.getWaterfallAdUnitIds()
        if (waterfallAdUnitIds.isEmpty()) {
            failAppOpenLoadBecauseNoAdUnit(adHolder)
            return
        }
        val adUnitId = waterfallAdUnitIds[waterfallIndex]
        val adUnitHolder = appOpenAdUnitHolderMap.getOrPut(adUnitId) { AppOpenAdUnitHolder() }
        if (adUnitHolder.isLoading) {
            markAppOpenAdUnitWaiter(adHolder.adPlace, adUnitHolder, activity)
            return
        }
        adUnitHolder.isLoading = true
        markAppOpenAdUnitWaiter(adHolder.adPlace, adUnitHolder, activity)
        scheduleAppOpenLoadTimeout(adUnitHolder, adUnitId)
        val loadCallback = object : AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                super.onAdLoaded(ad)
                Log.i(TAG, "AppOpenAd loaded $adPlaceName $adUnitId")
                ad.setOnPaidEventListener { adValue ->
                    trackAdjustAdRevenue(
                        adUnitId = adUnitId,
                        loadedAdapterResponseInfo = ad.responseInfo?.loadedAdapterResponseInfo,
                        adValueMicros = adValue.valueMicros,
                        adValueCurrencyCode = adValue.currencyCode
                    )
                }
                ad.setImmersiveMode(true)
                adUnitHolder.isLoading = false
                adUnitHolder.appOpenAd = ad
                adUnitHolder.loadTime = Date().time
                onAppOpenAdUnitLoaded(adHolder.adPlace, adUnitHolder)
                if (adHolder.isWaitLoadToShow) {
                    showAdIfAvailable(activity, adPlaceName)
                    adHolder.isWaitLoadToShow = false
                }
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                adUnitHolder.isLoading = false
                val nextWaterfallIndex = waterfallIndex + 1
                if (nextWaterfallIndex < waterfallAdUnitIds.size) {
                    resumeSiblingAppOpenWaitersOnFail(adUnitHolder, adPlaceName, adUnitId)
                    Log.i(TAG, "AppOpenAd waterfall next $adPlaceName ${waterfallAdUnitIds[nextWaterfallIndex]}")
                    fetchAd(activity, adPlaceName, nextWaterfallIndex)
                    return
                }
                onAppOpenAdUnitFailed(adHolder.adPlace, adUnitHolder, adUnitId)
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
        Log.i(TAG, "AppOpenAd start load $adPlaceName $adUnitId")
        AppOpenAd.load(
            context,
            adUnitId,
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

        if (!adHolder.isShowing && (adHolder.isAdAvailable() || findLoadedAppOpenAdUnitHolder(adPlace) != null)) {
            if (!isTimeAvailableShowAds()) {
                notifyAdNotValidOrLoadFailed(adPlaceName)
                return
            }
            val appOpenAd = adHolder.appOpenAd ?: consumeAppOpenAd(adPlace)?.also {
                adHolder.appOpenAd = it
                adHolder.loadTime = Date().time
            }
            if (appOpenAd == null) {
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
            appOpenAd.fullScreenContentCallback = fullScreenContentCallback
            appOpenAd.show(activity)
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

    /**
     * Danh sách ad unit id để load theo thứ tự waterfall: highFloorAdIds được ưu tiên,
     * adId thường là fallback cuối cùng.
     *
     * Với ad place thuộc tutorial flow (isTutorialFlow = true), tutorial_config tinh chỉnh từng nhóm:
     *  - enableAd1 = false -> loại toàn bộ highFloorAdIds (nhóm ad 1 = high floor).
     *  - enableAd2 = false -> loại adId thường (nhóm ad 2 = adId mặc định).
     * Kết quả có thể rỗng -> caller phải tự xử lý qua nhánh fail.
     */
    private fun AdPlace.getWaterfallAdUnitIds(): List<String> {
        val tutorialConfig = remoteConfigRepository.getTutorialConfig().takeIf { isTutorialFlow }
        val availableHighFloorAdIds = if (tutorialConfig?.enableAd1 == false) {
            emptyList()
        } else {
            highFloorAdIds
        }
        val availableAdId = if (tutorialConfig?.enableAd2 == false) "" else adId
        return (availableHighFloorAdIds + availableAdId)
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun failAppOpenLoadBecauseNoAdUnit(adHolder: AppOpenAdHolder) {
        val placeName = adHolder.adPlace.placeName
        Log.i(TAG, "AppOpenAd no available ad unit $placeName")
        adHolder.reset()
        notifyAdNotValidOrLoadFailed(placeName)
    }

    private fun findLoadedAppOpenAdUnitHolder(adPlace: AdPlace): AppOpenAdUnitHolder? {
        return adPlace.getWaterfallAdUnitIds()
            .firstNotNullOfOrNull { adUnitId ->
                appOpenAdUnitHolderMap[adUnitId]?.takeIf { it.isAdAvailable() }
            }
    }

    private fun markAppOpenAdUnitWaiter(
        adPlace: AdPlace,
        adUnitHolder: AppOpenAdUnitHolder,
        activity: Activity
    ) {
        adManager.getOrCreateAppOpenAdHolderBy(adPlace).isLoading = true
        adUnitHolder.waiterActivities[adPlace.placeName] = activity
    }

    private fun onAppOpenAdUnitLoaded(
        adPlace: AdPlace,
        adUnitHolder: AppOpenAdUnitHolder
    ) {
        // One SDK load can satisfy every placement that was waiting on this ad unit.
        val notifiedPlaceNames = (adUnitHolder.waiterActivities.keys + adPlace.placeName).distinct()
        notifiedPlaceNames.forEach { placeName ->
            val holderAdPlace = remoteConfigRepository.getAdPlaceBy(placeName)
            adManager.getOrCreateAppOpenAdHolderBy(holderAdPlace).isLoading = false
            notifyAdOpenAppLoaded(placeName)
        }
        adUnitHolder.waiterActivities.clear()
    }

    private fun onAppOpenAdUnitFailed(
        adPlace: AdPlace,
        adUnitHolder: AppOpenAdUnitHolder,
        adUnitId: String
    ) {
        // Owner (adPlace) has exhausted its waterfall and is handled by the caller's retry
        // logic. Siblings sharing this ad unit continue their own waterfall independently.
        resumeSiblingAppOpenWaitersOnFail(adUnitHolder, adPlace.placeName, adUnitId)
    }

    /**
     * Handles every waiter of a just-failed app-open ad unit except [ownerPlaceName]:
     * resumes the placement at its own next waterfall tier if one exists, otherwise notifies
     * it failed. Also resets the owner's loading flag and clears the dead waiter map.
     */
    private fun resumeSiblingAppOpenWaitersOnFail(
        adUnitHolder: AppOpenAdUnitHolder,
        ownerPlaceName: IAdPlaceName?,
        failedAdUnitId: String
    ) {
        val siblingPlaceNames = adUnitHolder.waiterActivities.keys.filter { it != ownerPlaceName }
        siblingPlaceNames.forEach { placeName ->
            val holderAdPlace = remoteConfigRepository.getAdPlaceBy(placeName)
            val activity = adUnitHolder.waiterActivities[placeName]
            val siblingWaterfall = holderAdPlace.getWaterfallAdUnitIds()
            val siblingNextIndex = siblingWaterfall.indexOf(failedAdUnitId) + 1
            adManager.getOrCreateAppOpenAdHolderBy(holderAdPlace).isLoading = false
            val canResume = activity != null && !activity.isDestroyed && !activity.isFinishing &&
                    siblingNextIndex in 1 until siblingWaterfall.size
            if (canResume) {
                Log.i(TAG, "AppOpenAd sibling waterfall next $placeName ${siblingWaterfall[siblingNextIndex]}")
                fetchAd(activity!!, placeName, siblingNextIndex)
            } else {
                notifyAdNotValidOrLoadFailed(placeName)
            }
        }
        ownerPlaceName?.let {
            adManager.getOrCreateAppOpenAdHolderBy(remoteConfigRepository.getAdPlaceBy(it)).isLoading = false
        }
        adUnitHolder.waiterActivities.clear()
    }

    private fun scheduleAppOpenLoadTimeout(
        adUnitHolder: AppOpenAdUnitHolder,
        adUnitId: String
    ) {
        val loadId = ++adUnitHolder.loadId
        applicationScope.launch {
            delay(APP_OPEN_LOAD_TIMEOUT_MS)
            if (adUnitHolder.loadId != loadId || !adUnitHolder.isLoading || adUnitHolder.isAdAvailable()) {
                return@launch
            }
            Log.i(TAG, "AppOpenAd load timeout $adUnitId, releasing stuck waiters")
            adUnitHolder.isLoading = false
            // No distinguished owner on timeout: every waiter resumes its own waterfall or fails.
            resumeSiblingAppOpenWaitersOnFail(adUnitHolder, ownerPlaceName = null, failedAdUnitId = adUnitId)
        }
    }

    private fun consumeAppOpenAd(adPlace: AdPlace): AppOpenAd? {
        // Move a loaded shared ad into the placement holder right before showing it.
        adPlace.getWaterfallAdUnitIds().forEach { adUnitId ->
            val adUnitHolder = appOpenAdUnitHolderMap[adUnitId] ?: return@forEach
            val ad = adUnitHolder.appOpenAd ?: return@forEach
            adUnitHolder.appOpenAd = null
            if (!adUnitHolder.isLoading && !adUnitHolder.isAdAvailable()) {
                appOpenAdUnitHolderMap.remove(adUnitId)
            }
            return ad
        }
        return null
    }

    private fun trackAdjustAdRevenue(
        adUnitId: String,
        loadedAdapterResponseInfo: AdapterResponseInfo?,
        adValueMicros: Long,
        adValueCurrencyCode: String
    ) {
        adjustAnalytics.trackRevenueNetwork(
            adUnitId = adUnitId,
            adSourceName = loadedAdapterResponseInfo?.adSourceName,
            adValueMicros = adValueMicros,
            adValueCurrencyCode = adValueCurrencyCode
        )
    }
}
