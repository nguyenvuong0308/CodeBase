package com.codebasetemplate.features.feature_splash.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.viewbinding.ViewBinding
import com.codebasetemplate.util.EventTracking
import com.core.ads.domain.AdFullScreenUiResource
import com.core.ads.domain.AdOpenAdUiResource
import com.core.ads.domain.ConsentFormUiResource
import com.core.ads.model.PreventShowManyInterstitialAds
import com.core.analytics.AnalyticsEvent
import com.core.baseui.countdown.JsgCountDownTimer
import com.core.baseui.ext.collectFlowOn
import com.core.config.data.FetchRemoteConfigState
import com.core.config.domain.data.AdType
import com.core.config.domain.data.CoreAdPlaceName
import com.core.config.domain.data.IAdPlaceName
import com.core.preference.SharedPrefs
import com.core.startflow.OnBoardingConfigFactory
import com.core.startflow.StartFlowActivity
import com.core.startflow.StartFlowNavigator
import com.core.startflow.StartFlowScreenType
import com.core.startflow.StartFlowShortcut
import com.core.utilities.getCurrentLanguageCode
import com.core.utilities.hideNavigationBar
import com.core.utilities.manager.isNetworkConnected
import com.core.utilities.util.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.absoluteValue

private const val TAG = "BaseSplashActivity"

abstract class BaseSplashActivity<VB : ViewBinding> : StartFlowActivity<VB>() {

    @Inject
    lateinit var startFlowNavigator: StartFlowNavigator

    protected val baseViewModel by viewModels<BaseSplashViewModel>()
    override val isWaitingAds: Boolean
        get() = true
    protected var timeShowIntro by SharedPrefs.instance.preference(
        defaultValue = 0L,
        key = "timeShowIntro"
    )

    protected var countDownTimer: JsgCountDownTimer? = null
    protected var delayedShowAdJob: Job? = null
    private var splashTrackingStartedAtMs = 0L
    private var splashBeforeAdLogged = false
    private var splashCompleteLogged = false
    private var interSplashTrackingStartedAtMs = 0L
    private var interSplashCompleteLogged = false
    private var enterAppOnNetworkLost = false
    private var networkLostFastEnterJob: Job? = null

    private val appOpenPlaceName by lazy {
        if (baseViewModel.isFirstOpenApp) {
            CoreAdPlaceName.APP_OPEN_FIRST_OPEN
        } else {
            CoreAdPlaceName.APP_OPEN
        }
    }

    private val interstitialPlaceName by lazy {
        if (baseViewModel.isFirstOpenApp) {
            CoreAdPlaceName.ACTION_OPEN_APP_FIRST_OPEN
        } else {
            CoreAdPlaceName.ACTION_OPEN_APP
        }
    }

    protected val isEnableIntroductionScreen: Boolean by lazy {
        remoteConfigRepository.getAppConfig().isEnableIntroductionScreen
    }
    protected val isEnableLanguageScreen: Boolean by lazy {
        remoteConfigRepository.getAppConfig().isEnableChangeLanguageScreen
    }
    protected val isAlwaysShowIntroAndLanguageScreen: Boolean by lazy {
        if (remoteConfigRepository.getAppConfig().isAlwaysShowIntroAndLanguageScreen) {
            true
        } else if (remoteConfigRepository.getAppConfig().isAlwaysShowIntroAndLanguageScreenWithInterval) {
            val subDate = subDate(System.currentTimeMillis(), timeShowIntro)
            subDate >= remoteConfigRepository.getAppConfig().intervalDayAlwaysShowIntroAndLanguage
        } else {
            false
        }
    }

    private fun subDate(currentTime: Long, previousTime: Long): Int {
        val calCurrent = Calendar.getInstance().apply { timeInMillis = currentTime }
        val calPrevious = Calendar.getInstance().apply { timeInMillis = previousTime }

        // Reset giờ, phút, giây, mili giây về 0 để chỉ so sánh ngày
        calCurrent.set(Calendar.HOUR_OF_DAY, 0)
        calCurrent.set(Calendar.MINUTE, 0)
        calCurrent.set(Calendar.SECOND, 0)
        calCurrent.set(Calendar.MILLISECOND, 0)

        calPrevious.set(Calendar.HOUR_OF_DAY, 0)
        calPrevious.set(Calendar.MINUTE, 0)
        calPrevious.set(Calendar.SECOND, 0)
        calPrevious.set(Calendar.MILLISECOND, 0)

        val diffMillis = calCurrent.timeInMillis - calPrevious.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(diffMillis).toInt().absoluteValue
    }

    private var openInternetConnectivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    /**Shortcut Data - Điều hướng màn hình theo shortcut*/
    protected val targetScreenFromShortCut by lazy {
        intent.extras?.getString(StartFlowShortcut.KEY_SHORTCUT_TARGET_SCREEN, "")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        splashTrackingStartedAtMs = SystemClock.elapsedRealtime()
        EventTracking.logEvent(EventTracking.EVENT_SPLASH_VIEW)
        installSplashScreen()

        appOpenAdManager.setupDefaultValue()
        hideNavigationBar()
        initView()
        initData()
    }

    abstract fun initData()
    abstract fun hideLoadingX()
    protected open fun updateSplashProgress(progress: Int, max: Int) = Unit
    protected open fun onSplashCountdownStarted(max: Int) = Unit
    protected open fun onSplashStatusChanged(status: SplashStatus) = Unit

    fun onDataReady() {
        onSplashStatusChanged(SplashStatus.FetchingRemoteConfig)
        val eventName = if (isNetworkConnected()) {
            AnalyticsEvent.NETWORK_AVAILABLE
        } else {
            AnalyticsEvent.NETWORK_NOT_AVAILABLE
        }
        analyticsManager.logEvent(eventName)

        if (isNetworkConnected()) {
            startSplashPrerequisites(enterAppOnNetworkLost = true)
        } else {
            onSplashStatusChanged(SplashStatus.WaitingForInternet)
            showRequireTurnOnNetworkBottomSheetFragment()
        }
    }


    private fun initView() {
        when (targetScreenFromShortCut) {
            StartFlowScreenType.Uninstall.screenName -> {
                if (getCurrentLanguageCode().isBlank()) {
                    analyticsManager.logEvent(AnalyticsEvent.EVENT_CLICK_SHORT_CUT_UNINSTALL_BEFORE_SET_LANGUAGE)
                } else {
                    analyticsManager.logEvent(AnalyticsEvent.EVENT_CLICK_SHORT_CUT_UNINSTALL)
                }
            }

            else -> {
                if (targetScreenFromShortCut != null) {
                    analyticsManager.logEvent(AnalyticsEvent.EVENT_CLICK_SHORT_CUT + targetScreenFromShortCut)
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hideNavigationBar()
    }

    override fun providerInterAdPlaceName(): List<IAdPlaceName> {
        return mutableListOf<IAdPlaceName>().apply {
            if (getCurrentLanguageCode().isBlank() || isAlwaysShowIntroAndLanguageScreen) {
                add(CoreAdPlaceName.ACTION_NEXT_IN_INTRODUCTION)
                add(CoreAdPlaceName.ACTION_SKIP_IN_INTRODUCTION)
            }
        }
    }

    override fun providerPreloadBannerNativeAdPlaceName(): List<IAdPlaceName> {
        val isLoadLanguage =
            getCurrentLanguageCode().isBlank() || isAlwaysShowIntroAndLanguageScreen
        return mutableListOf<IAdPlaceName>().apply {
            if (isLoadLanguage) {
                add(CoreAdPlaceName.ANCHORED_CHANGE_LANGUAGE_BOTTOM)
            }
            if (isEnableIntroductionScreen && isLoadLanguage) {
                addAll(
                    OnBoardingConfigFactory.getOnBoardingAdPlaceName(
                        remoteConfigRepository.getOnBoardingConfig(),
                        remoteConfigRepository.getAppConfig()
                    )
                )
            }

            if (targetScreenFromShortCut == StartFlowScreenType.Uninstall.screenName) {
                addAll(startFlowNavigator.uninstallPreloadAdPlaceNames())
            }

            addAll(startFlowNavigator.mainPreloadAdPlaceNames())
        }
    }

    override fun onResume() {
        super.onResume()
        baseViewModel.isActivityResume = true
        if (baseViewModel.needHandleEventWhenResume) {
            baseViewModel.needHandleEventWhenResume = false
            showRequireTurnOnNetworkBottomSheetFragment()
        }

        if (countDownTimer?.isTimerPaused() == true) {
            countDownTimer?.resumeTimer()
        }
        if (countDownTimer == null && baseViewModel.isSplashAdsFlowStarted) {
            startCountDownTimer()
        }
    }

    override fun handleObservable() {
        val ignoreSuper = true
        if (!ignoreSuper) {
            super.handleObservable()
        }
        collectFlowOn(remoteConfigRepository.fetchStateCompleteFlow) { fetchState ->
            Timber.e("fetchState: $fetchState")
            when (fetchState) {
                FetchRemoteConfigState.Loading -> {
                    onSplashStatusChanged(SplashStatus.FetchingRemoteConfig)
                }

                is FetchRemoteConfigState.Complete -> {
                    handleRemoteConfigReady()
                }
            }
        }

        collectFlowOn(adsManager.requestConsentFlow) { uiResource ->
            when (uiResource) {
                ConsentFormUiResource.Loading -> {
                    onSplashStatusChanged(SplashStatus.WaitingForConsent)
                }

                ConsentFormUiResource.Showing -> {
                    onSplashStatusChanged(SplashStatus.WaitingForConsent)
                }

                ConsentFormUiResource.Complete -> {
                    Timber.e("ConsentFormUiResource.Complete")
                    baseViewModel.isRequestEuConsentComplete = true
                    tryStartSplashAdsFlow()
                }
            }
        }

        collectFlowOn(appOpenAdManager.adOpenAppFlow) { uiResource ->
            Timber.e("appOpenAdManager.adOpenAppFlow $uiResource")
            if (uiResource.rootAdPlaceName == appOpenPlaceName) {
                when (uiResource) {
                    is AdOpenAdUiResource.AdLoaded -> {
                        handleWhenAdLoaded()
                    }

                    is AdOpenAdUiResource.AdNotValidOrLoadFailed -> {
                        handleWhenAdNotValidOrLoadFailed()
                    }

                    is AdOpenAdUiResource.AdShowing -> {
                        handleWhenAdShowing()
                    }

                    is AdOpenAdUiResource.AdDismissed -> {
                        handleWhenAdDismissed()
                    }
                }
            }
        }

        collectFlowOn(adsManager.adFullScreenFlow) { uiResource ->
            Timber.e("appOpenAdManager.adFullScreenFlow $uiResource")
            if (uiResource.rootAdPlaceName == interstitialPlaceName) {
                when (uiResource) {
                    is AdFullScreenUiResource.AdLoaded -> {
                        handleWhenAdLoaded()
                    }

                    is AdFullScreenUiResource.AdNotValidOrLoadFailed -> {
                        handleWhenAdNotValidOrLoadFailed()
                    }

                    is AdFullScreenUiResource.AdSucceedToShow -> {
                        handleWhenAdShowing()
                    }

                    is AdFullScreenUiResource.AdDismissed -> {
                        handleWhenAdDismissed()
                    }

                    else -> {}
                }
            }
        }

        collectFlowOn(baseViewModel.showRequireTurnOnNetworkWhenRetryClickedFlow) {
            if (baseViewModel.isActivityResume) {
                showRequireTurnOnNetworkBottomSheetFragment()
            } else {
                baseViewModel.needHandleEventWhenResume = true
            }
        }
    }

    open fun fetchSplashAds() {
        Log.d(TAG, "fetchSplashAds: 0")
        if (baseViewModel.isFirstOpenApp) {
            if (remoteConfigRepository.getSplashScreenConfig().adTypeFirstOpen == AdType.AppOpen) {
                Log.d(TAG, "fetchSplashAds: 1")
                fetchAppOpenAd()
            } else {
                Log.d(TAG, "fetchSplashAds: 2")
                fetchAppOpenAdTypeInterstitial()
            }
        } else {
            if (remoteConfigRepository.getSplashScreenConfig().adType == AdType.AppOpen) {
                Log.d(TAG, "fetchSplashAds: 3")
                fetchAppOpenAd()
            } else {
                Log.d(TAG, "fetchSplashAds: 4")
                fetchAppOpenAdTypeInterstitial()
            }
        }
    }

    /** Đây là hàm tải quảng cáo app open */
    open fun fetchAppOpenAd() {
        appOpenAdManager.fetchAd(this, appOpenPlaceName)
    }

    /** Đây là hàm tải quảng cáo interstitial */
    open fun fetchAppOpenAdTypeInterstitial() {
        adsManager.loadFullscreenAd(
            this,
            interstitialPlaceName,
            isNeedUpdateAdPlace = true,
            identifier = ""
        )
    }

    private fun handleRemoteConfigReady() {
        startFlowNavigator.setUpShortCut(
            this,
            remoteConfigRepository.getAppConfig().isEnableAppShortCut,
            remoteConfigRepository.getAppConfig().isEnableAppShortcutUninstall
        )
        PreventShowManyInterstitialAds.initIntervalTimeShowInterstitialMillis()
        adsManager.startDisableAdCountDownTimer()
        baseViewModel.isRemoteConfigReady = true
        tryStartSplashAdsFlow()
    }

    private fun startSplashPrerequisites(enterAppOnNetworkLost: Boolean) {
        this.enterAppOnNetworkLost = enterAppOnNetworkLost
        if (enterAppOnNetworkLost) {
            startNetworkLostFastEnterWatcher()
        } else {
            stopNetworkLostFastEnterWatcher()
        }
        remoteConfigRepository.fetchAndActive()
        if (!baseViewModel.isRequestEuConsentComplete) {
            adsManager.requestConsentInfoUpdate(this, false)
        }
    }

    private fun startNetworkLostFastEnterWatcher() {
        networkLostFastEnterJob?.cancel()
        networkLostFastEnterJob = CoroutineScope(coroutineContext).launch {
            while (this@BaseSplashActivity.enterAppOnNetworkLost) {
                delay(250)
                if (shouldEnterAppWhenNetworkLost(
                        isNetworkConnected = isNetworkConnected(),
                        enterAppOnNetworkLost = this@BaseSplashActivity.enterAppOnNetworkLost,
                        isProgressRunning = countDownTimer?.isTimerRunning() == true
                    )
                ) {
                    openNextScreenAndFinish()
                    return@launch
                }
            }
        }
    }

    private fun stopNetworkLostFastEnterWatcher() {
        enterAppOnNetworkLost = false
        networkLostFastEnterJob?.cancel()
        networkLostFastEnterJob = null
    }

    private fun tryStartSplashAdsFlow() {
        if (!baseViewModel.isRemoteConfigReady || !baseViewModel.isRequestEuConsentComplete) {
            return
        }
        if (baseViewModel.isSplashAdsFlowStarted) {
            return
        }

        baseViewModel.isSplashAdsFlowStarted = true
        reinitAdPlaceName(
            initInterstitialAdPlaceName = providerInterAdPlaceName(),
            initBannerNativeAdPlaceName = providerBannerNativeAdPlaceName(),
            initRewardAdPlaceName = providerRewardAdPlaceName(),
            initPreloadBannerNativeAdPlaceName = providerPreloadBannerNativeAdPlaceName()
        )
        readyAds()
        preloadAds()

        if (isShowAd()) {
            fetchSplashAds()
        } else {
            onSplashStatusChanged(SplashStatus.AdsUnavailable)
            handleWhenAdNotValidOrLoadFailed()
        }

        if (baseViewModel.isActivityResume) {
            onSplashStatusChanged(SplashStatus.CountdownRunning)
            startCountDownTimer()
        }
    }

    open fun isShowAd(): Boolean {
        val isShowAd = when {
            purchasePreferences.isUserVip() -> false
            targetScreenFromShortCut.isNullOrBlank() -> true
            targetScreenFromShortCut == StartFlowScreenType.Uninstall.screenName -> {
                remoteConfigRepository.getAppConfig().isEnableOpenAppAdsFromUninstallShortcut
            }

            else -> {
                remoteConfigRepository.getAppConfig().isEnableOpenAppAdsFromShortcut
            }
        }
        return isShowAd
    }

    private fun handleWhenAdLoaded() {
        Log.d(TAG, "handleWhenAdLoaded: ")
        onSplashStatusChanged(SplashStatus.AdLoaded)
        baseViewModel.handleWhenAdLoaded()
    }

    private fun handleWhenAdNotValidOrLoadFailed() {
        Log.d(TAG, "handleWhenAdNotValidOrLoadFailed: ")
        onSplashStatusChanged(SplashStatus.AdsUnavailable)
        baseViewModel.handleWhenAdNotValidOrLoadFailed()
        checkAbleToNextScreen()
    }

    private fun checkAbleToNextScreen() {
        if (isFinishing || isDestroyed) return

        if (baseViewModel.currentProgress >= baseViewModel.timeSkipAppOpenAdWhenNotAvailable && baseViewModel.isAdNotValidOrLoadFailed) {
            openNextScreenAndFinish()
            return
        }

        if (baseViewModel.isTimerComplete && !baseViewModel.isAppOpenAdLoaded && !baseViewModel.isAppOpenAdShowing) {
            openNextScreenAndFinish()
            return
        }

        if (baseViewModel.isAppOpenAdDismissed) {
            openNextScreenAndFinish()
            return
        }
    }

    abstract fun openNextScreen()

    private fun openNextScreenAndFinish() {
        if (isFinishing || isDestroyed) return
        stopNetworkLostFastEnterWatcher()
        countDownTimer?.pauseTimer()
        delayedShowAdJob?.cancel()
        delayedShowAdJob = null
        appOpenAdManager.isFirstOpenApp = false
        logSplashCompleteIfNeeded()
        openNextScreen()
        finish()
    }

    private fun handleWhenAdShowing() {
        Log.d(TAG, "handleWhenAdShowing: ")
        stopNetworkLostFastEnterWatcher()
        logSplashBeforeAdIfNeeded()
        logInterSplashViewIfNeeded()
        onSplashStatusChanged(SplashStatus.ShowingAd)
        hideLoadingX()
        baseViewModel.handleWhenAdShowing()
    }

    private fun handleWhenAdDismissed() {
        Log.d(TAG, "handleWhenAdDismissed: ")
        logInterSplashCompleteIfNeeded()
        onSplashStatusChanged(SplashStatus.ReadyToEnterApp)
        baseViewModel.handleWhenAdDismissed()
        checkAbleToNextScreen()
    }

    override fun onPause() {
        super.onPause()
        baseViewModel.isActivityResume = false
        if (countDownTimer?.isTimerRunning() == true) {
            countDownTimer?.pauseTimer()
        }
    }

    override fun onNetworkChange(isNetworkConnected: Boolean) {
        super.onNetworkChange(isNetworkConnected)
        if (shouldEnterAppWhenNetworkLost(
                isNetworkConnected = isNetworkConnected,
                enterAppOnNetworkLost = enterAppOnNetworkLost,
                isProgressRunning = countDownTimer?.isTimerRunning() == true
            )
        ) {
            openNextScreenAndFinish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appOpenAdManager.isFirstOpenApp = false
        delayedShowAdJob?.cancel()
        delayedShowAdJob = null
        stopNetworkLostFastEnterWatcher()
        coroutineContext.cancelChildren()
        stopCountDown()
    }

    private fun stopCountDown() {
        try {
            delayedShowAdJob?.cancel()
            delayedShowAdJob = null
            countDownTimer?.pauseTimer()
            countDownTimer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startCountDownTimer() {
        if (countDownTimer != null) {
            countDownTimer?.pauseTimer()
            countDownTimer = null
        }

        val minTimeWaitProgressBeforeShowAd =
            remoteConfigRepository.getSplashScreenConfig().minTimeWaitProgressBeforeShowAd * 1000L
        val timeMillisDelayBeforeShow =
            remoteConfigRepository.getAppOpenAdConfig().timeMillisDelayBeforeShow
        baseViewModel.timeSkipAppOpenAdWhenNotAvailable =
            remoteConfigRepository.getSplashScreenConfig().timeSkipAppOpenAdWhenNotAvailable * 1000L

        baseViewModel.maxProgress =
            remoteConfigRepository.getSplashScreenConfig().maxTimeToWaitAppOpenAd * 1000L
        onSplashCountdownStarted(baseViewModel.maxProgress.toInt())
        onSplashStatusChanged(SplashStatus.CountdownRunning)
        updateSplashProgress(progress = 0, max = baseViewModel.maxProgress.toInt())

        Timber.e("startCountDownTimer ${baseViewModel.maxProgress}")

        countDownTimer = object : JsgCountDownTimer(baseViewModel.maxProgress, 100) {
            override fun onTimerTick(timeRemaining: Long) {
                baseViewModel.currentProgress = baseViewModel.maxProgress - timeRemaining
                updateSplashProgress(
                    progress = baseViewModel.currentProgress.coerceAtMost(baseViewModel.maxProgress)
                        .toInt(),
                    max = baseViewModel.maxProgress.toInt()
                )
                Timber.e("startCountDownTimer ${baseViewModel.currentProgress}")
                if (baseViewModel.isAppOpenAdLoaded) {
                    Timber.e("isAppOpenAdLoaded ${baseViewModel.isAppOpenAdLoaded}")
                    if (baseViewModel.currentProgress >= minTimeWaitProgressBeforeShowAd) {
                        baseViewModel.isAppOpenAdLoaded = false
                        delayedShowAdJob?.cancel()
                        delayedShowAdJob = CoroutineScope(coroutineContext).launch {
                            delay(timeMillisDelayBeforeShow)
                            if (baseViewModel.isFirstOpenApp) {
                                if (remoteConfigRepository.getSplashScreenConfig().adTypeFirstOpen == AdType.AppOpen) {
                                    appOpenAdManager.showAdIfAvailable(
                                        this@BaseSplashActivity,
                                        appOpenPlaceName
                                    )
                                } else {
                                    adsManager.showAd(
                                        this@BaseSplashActivity,
                                        fragmentManager = supportFragmentManager,
                                        adPlaceName = interstitialPlaceName,
                                        identifier = ""
                                    )
                                }
                            } else {
                                if (remoteConfigRepository.getSplashScreenConfig().adType == AdType.AppOpen) {
                                    appOpenAdManager.showAdIfAvailable(
                                        this@BaseSplashActivity,
                                        appOpenPlaceName
                                    )
                                } else {
                                    adsManager.showAd(
                                        this@BaseSplashActivity,
                                        fragmentManager = supportFragmentManager,
                                        adPlaceName = interstitialPlaceName,
                                        identifier = ""
                                    )
                                }
                            }
                        }
                    }
                    return
                }
                checkAbleToNextScreen()
            }

            override fun onTimerFinish() {
                baseViewModel.isTimerComplete = true
                updateSplashProgress(
                    progress = baseViewModel.maxProgress.toInt(),
                    max = baseViewModel.maxProgress.toInt()
                )
                onSplashStatusChanged(SplashStatus.ReadyToEnterApp)
                checkAbleToNextScreen()
            }
        }
        countDownTimer?.startTimer()
    }

    private fun showRequireTurnOnNetworkBottomSheetFragment() {
        showRequireTurnOnNetworkBottomSheetFragment(
            onRetry = {
                CoroutineScope(coroutineContext).launch {
                    delay(1000)
                    if (isNetworkConnected()) {
                        analyticsManager.logEvent(AnalyticsEvent.ACTION_SPLASH_RETRY_TURN_ON)
                        startSplashPrerequisites(enterAppOnNetworkLost = true)
                    } else {
                        baseViewModel.showRequireTurnOnNetworkWhenRetryClickedFlow.emit(true)
                        val intentNetwork = if (Build.VERSION.SDK_INT >= 29) {
                            Intent("android.settings.panel.action.INTERNET_CONNECTIVITY")
                        } else {
                            Intent("android.settings.WIRELESS_SETTINGS")
                        }
                        openInternetConnectivityLauncher.launch(intentNetwork)
                    }
                }
            },
            onCancel = {
                startSplashPrerequisites(enterAppOnNetworkLost = false)
            }
        )
    }

    private fun logSplashBeforeAdIfNeeded() {
        if (splashBeforeAdLogged) return
        splashBeforeAdLogged = true
        EventTracking.logEngagementComplete(
            EventTracking.EVENT_SPLASH_BEFORE_AD,
            splashTrackingStartedAtMs,
            SystemClock.elapsedRealtime()
        )
    }

    private fun logSplashCompleteIfNeeded() {
        if (splashCompleteLogged) return
        splashCompleteLogged = true
        EventTracking.logEngagementComplete(
            EventTracking.EVENT_SPLASH_COMPLETE,
            splashTrackingStartedAtMs,
            SystemClock.elapsedRealtime()
        )
    }

    private fun logInterSplashViewIfNeeded() {
        if (interSplashTrackingStartedAtMs != 0L) return
        interSplashTrackingStartedAtMs = SystemClock.elapsedRealtime()
        EventTracking.logEvent(EventTracking.EVENT_INTER_SPLASH_VIEW)
    }

    private fun logInterSplashCompleteIfNeeded() {
        if (interSplashCompleteLogged || interSplashTrackingStartedAtMs == 0L) return
        interSplashCompleteLogged = true
        EventTracking.logEngagementComplete(
            EventTracking.EVENT_INTER_SPLASH_COMPLETE,
            interSplashTrackingStartedAtMs,
            SystemClock.elapsedRealtime()
        )
    }

}

enum class SplashStatus {
    FetchingRemoteConfig,
    WaitingForInternet,
    WaitingForConsent,
    CountdownRunning,
    AdLoaded,
    AdsUnavailable,
    ShowingAd,
    ReadyToEnterApp
}

internal fun shouldEnterAppWhenNetworkLost(
    isNetworkConnected: Boolean,
    enterAppOnNetworkLost: Boolean,
    isProgressRunning: Boolean
): Boolean = !isNetworkConnected && (enterAppOnNetworkLost || isProgressRunning)
