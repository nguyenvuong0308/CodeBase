package com.codebasetemplate.features.feature_splash.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
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
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            baseViewModel.needHandleEventWhenResume = false
            handleCurrentNetworkAvailability()
        }

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
        val isNetworkAvailable = networkConnectionManager.isNetworkAvailable
        val eventName = if (isNetworkAvailable) {
            AnalyticsEvent.NETWORK_AVAILABLE
        } else {
            AnalyticsEvent.NETWORK_NOT_AVAILABLE
        }
        analyticsManager.logEvent(eventName)
        val action = baseViewModel.networkGate.onDataReady(isNetworkAvailable)
        Log.d(TAG, "[SplashNetwork] dataReady available=$isNetworkAvailable action=$action")
        handleSplashNetworkAction(action)
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
                if(remoteConfigRepository.getLanguageActivityConfig().isV2) {
                    add(CoreAdPlaceName.ANCHORED_CHANGE_LANGUAGE_V2_NATIVE_1)
                    add(CoreAdPlaceName.ANCHORED_CHANGE_LANGUAGE_V2_NATIVE_2)
                    add(CoreAdPlaceName.ANCHORED_CHANGE_LANGUAGE_V2_NATIVE_3)
                } else {
                    add(CoreAdPlaceName.ANCHORED_CHANGE_LANGUAGE_V1_STEP_1)
                    add(CoreAdPlaceName.ANCHORED_CHANGE_LANGUAGE_V1_STEP_2)
                }
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
            handleCurrentNetworkAvailability()
        }
        if (baseViewModel.networkGate.isWaitingForNetwork) {
            showRequireTurnOnNetworkBottomSheetFragment()
        }

        if (!baseViewModel.networkGate.isWaitingForNetwork &&
            !baseViewModel.isTimerComplete &&
            countDownTimer?.isTimerPaused() == true
        ) {
            countDownTimer?.resumeTimer()
        }
        if (!baseViewModel.networkGate.isWaitingForNetwork &&
            countDownTimer == null &&
            baseViewModel.isSplashAdsFlowStarted
        ) {
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

        collectFlowOn(
            networkConnectionManager.isNetworkAvailableFlow,
            Lifecycle.State.RESUMED
        ) { isNetworkAvailable ->
            val shouldGuardNetwork = shouldGuardSplashNetwork()
            val action = baseViewModel.networkGate.onNetworkAvailabilityChanged(
                isNetworkAvailable = isNetworkAvailable,
                shouldGuardAds = shouldGuardNetwork,
                isAdsFlowStarted = baseViewModel.isSplashAdsFlowStarted,
            )
            Log.d(
                TAG,
                "[SplashNetwork] flow available=$isNetworkAvailable " +
                        "guard=$shouldGuardNetwork action=$action"
            )
            handleSplashNetworkAction(action)
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
        if (!baseViewModel.isRemoteConfigReady) {
            startFlowNavigator.setUpShortCut(
                this,
                remoteConfigRepository.getAppConfig().isEnableAppShortCut,
                remoteConfigRepository.getAppConfig().isEnableAppShortcutUninstall
            )
            PreventShowManyInterstitialAds.initIntervalTimeShowInterstitialMillis()
            adsManager.startDisableAdCountDownTimer()
            baseViewModel.isRemoteConfigReady = true
        }
        tryStartSplashAdsFlow()
    }

    private fun startSplashPrerequisites() {
        if (!baseViewModel.isRemoteConfigReady) {
            remoteConfigRepository.fetchAndActive()
        }
        if (!baseViewModel.isRequestEuConsentComplete) {
            adsManager.requestConsentInfoUpdate(this, false)
        }
        tryStartSplashAdsFlow()
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
            if (!baseViewModel.networkGate.isWaitingForNetwork) {
                requestSplashAd()
            } else {
                onSplashStatusChanged(SplashStatus.WaitingForInternet)
            }
        } else {
            baseViewModel.networkGate.releaseNetworkWait()
            dismissRequireTurnOnNetworkBottomSheetFragment()
            onSplashStatusChanged(SplashStatus.AdsUnavailable)
            handleWhenAdNotValidOrLoadFailed()
        }

        if (baseViewModel.isActivityResume && !baseViewModel.networkGate.isWaitingForNetwork) {
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
        onSplashStatusChanged(
            if (baseViewModel.networkGate.isWaitingForNetwork) {
                SplashStatus.WaitingForInternet
            } else {
                SplashStatus.AdsUnavailable
            }
        )
        baseViewModel.handleWhenAdNotValidOrLoadFailed()
        checkAbleToNextScreen()
    }

    private fun checkAbleToNextScreen() {
        if (isFinishing || isDestroyed) return
        if (!networkConnectionManager.isNetworkAvailable && shouldGuardSplashNetwork()) {
            val action = baseViewModel.networkGate.onNetworkAvailabilityChanged(
                isNetworkAvailable = false,
                shouldGuardAds = true,
                isAdsFlowStarted = baseViewModel.isSplashAdsFlowStarted,
            )
            if (action != SplashNetworkAction.None) {
                Log.d(TAG, "[SplashNetwork] navigation guard action=$action")
                handleSplashNetworkAction(action)
            }
        }
        if (baseViewModel.networkGate.isWaitingForNetwork) return
        val nextScreen = {
            countDownTimer?.pauseTimer()
            appOpenAdManager.isFirstOpenApp = false
            logSplashCompleteIfNeeded()
            openNextScreen()
            finish()
        }

        if (baseViewModel.currentProgress >= baseViewModel.timeSkipAppOpenAdWhenNotAvailable && baseViewModel.isAdNotValidOrLoadFailed) {
            nextScreen()
            return
        }

        if (baseViewModel.isTimerComplete && !baseViewModel.isAppOpenAdLoaded && !baseViewModel.isAppOpenAdShowing) {
            nextScreen()
            return
        }

        if (baseViewModel.isAppOpenAdDismissed) {
            nextScreen()
            return
        }
    }

    abstract fun openNextScreen()

    private fun handleWhenAdShowing() {
        Log.d(TAG, "handleWhenAdShowing: ")
        baseViewModel.networkGate.releaseNetworkWait()
        dismissRequireTurnOnNetworkBottomSheetFragment()
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

    override fun onDestroy() {
        super.onDestroy()
        appOpenAdManager.isFirstOpenApp = false
        delayedShowAdJob?.cancel()
        delayedShowAdJob = null
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
                    if (baseViewModel.currentProgress >= minTimeWaitProgressBeforeShowAd &&
                        delayedShowAdJob?.isActive != true
                    ) {
                        delayedShowAdJob = CoroutineScope(coroutineContext).launch {
                            delay(timeMillisDelayBeforeShow)
                            if (!networkConnectionManager.isNetworkAvailable) {
                                handleCurrentNetworkAvailability()
                                return@launch
                            }
                            if (!baseViewModel.isActivityResume || isFinishing || isDestroyed) {
                                return@launch
                            }
                            baseViewModel.isAppOpenAdLoaded = false
                            showSplashAd()
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
                    Log.d(
                        TAG,
                        "[SplashNetwork] retry available=${networkConnectionManager.isNetworkAvailable}"
                    )
                    if (networkConnectionManager.isNetworkAvailable) {
                        analyticsManager.logEvent(AnalyticsEvent.ACTION_SPLASH_RETRY_TURN_ON)
                        handleCurrentNetworkAvailability()
                    } else {
                        baseViewModel.needHandleEventWhenResume = true
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
                val action = baseViewModel.networkGate.continueWithoutNetwork(
                    isAdsFlowStarted = baseViewModel.isSplashAdsFlowStarted
                )
                Log.d(TAG, "[SplashNetwork] cancel action=$action")
                handleSplashNetworkAction(action)
            }
        )
    }

    private fun handleCurrentNetworkAvailability() {
        val isNetworkAvailable = networkConnectionManager.isNetworkAvailable
        val shouldGuardNetwork = shouldGuardSplashNetwork()
        val action = baseViewModel.networkGate.onNetworkAvailabilityChanged(
            isNetworkAvailable = isNetworkAvailable,
            shouldGuardAds = shouldGuardNetwork,
            isAdsFlowStarted = baseViewModel.isSplashAdsFlowStarted,
        )
        Log.d(
            TAG,
            "[SplashNetwork] snapshot available=$isNetworkAvailable " +
                    "guard=$shouldGuardNetwork action=$action"
        )
        handleSplashNetworkAction(action)
    }

    private fun shouldGuardSplashNetwork(): Boolean {
        return !baseViewModel.isSplashAdsFlowStarted ||
                (isShowAd() &&
                        !baseViewModel.isAppOpenAdShowing &&
                        !baseViewModel.isAppOpenAdDismissed)
    }

    private fun handleSplashNetworkAction(action: SplashNetworkAction) {
        Log.d(
            TAG,
            "[SplashNetwork] handle action=$action " +
                    "waiting=${baseViewModel.networkGate.isWaitingForNetwork} " +
                    "adsFlow=${baseViewModel.isSplashAdsFlowStarted} " +
                    "requested=${baseViewModel.isSplashAdLoadRequested} " +
                    "loaded=${baseViewModel.isAppOpenAdLoaded} " +
                    "showing=${baseViewModel.isAppOpenAdShowing} " +
                    "timerComplete=${baseViewModel.isTimerComplete}"
        )
        when (action) {
            SplashNetworkAction.None -> Unit
            SplashNetworkAction.ShowNetworkPrompt -> {
                onSplashStatusChanged(SplashStatus.WaitingForInternet)
                showRequireTurnOnNetworkBottomSheetFragment()
            }

            SplashNetworkAction.StartPrerequisites,
            SplashNetworkAction.RetryPrerequisites -> {
                dismissRequireTurnOnNetworkBottomSheetFragment()
                startSplashPrerequisites()
            }

            SplashNetworkAction.PauseAds -> pauseSplashAdsForNetwork()
            SplashNetworkAction.ResumeAds -> resumeSplashAdsAfterNetworkWait()
        }
    }

    private fun pauseSplashAdsForNetwork() {
        Log.d(TAG, "[SplashNetwork] pause ads and countdown")
        delayedShowAdJob?.cancel()
        delayedShowAdJob = null
        countDownTimer?.pauseTimer()
        onSplashStatusChanged(SplashStatus.WaitingForInternet)
        showRequireTurnOnNetworkBottomSheetFragment()
    }

    private fun resumeSplashAdsAfterNetworkWait() {
        Log.d(TAG, "[SplashNetwork] resume ads flow")
        dismissRequireTurnOnNetworkBottomSheetFragment()
        if (baseViewModel.isTimerComplete) {
            checkAbleToNextScreen()
            return
        }

        if (isShowAd() &&
            !baseViewModel.isSplashAdLoadRequested &&
            !baseViewModel.isAppOpenAdLoaded &&
            !baseViewModel.isAppOpenAdShowing &&
            !baseViewModel.isAppOpenAdDismissed
        ) {
            requestSplashAd()
        }

        if (!baseViewModel.isActivityResume) return
        if (countDownTimer == null) {
            startCountDownTimer()
        } else if (countDownTimer?.isTimerPaused() == true) {
            onSplashStatusChanged(SplashStatus.CountdownRunning)
            countDownTimer?.resumeTimer()
        }
        checkAbleToNextScreen()
    }

    private fun requestSplashAd() {
        if (baseViewModel.isSplashAdLoadRequested ||
            baseViewModel.isAppOpenAdLoaded ||
            baseViewModel.isAppOpenAdShowing ||
            baseViewModel.isAppOpenAdDismissed
        ) {
            return
        }
        Log.d(TAG, "[SplashNetwork] request splash ad")
        baseViewModel.prepareForSplashAdRetry()
        fetchSplashAds()
    }

    private fun showSplashAd() {
        val adType = if (baseViewModel.isFirstOpenApp) {
            remoteConfigRepository.getSplashScreenConfig().adTypeFirstOpen
        } else {
            remoteConfigRepository.getSplashScreenConfig().adType
        }
        Log.d(TAG, "[SplashNetwork] show splash ad type=$adType")
        if (adType == AdType.AppOpen) {
            appOpenAdManager.showAdIfAvailable(this, appOpenPlaceName)
        } else {
            adsManager.showAd(
                this,
                fragmentManager = supportFragmentManager,
                adPlaceName = interstitialPlaceName,
                identifier = ""
            )
        }
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
