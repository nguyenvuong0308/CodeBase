package com.codebasetemplate.features.feature_splash.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.viewbinding.ViewBinding
import com.codebasetemplate.core.base_ui.CoreActivity
import com.codebasetemplate.features.feature_onboarding.ui.helper.OnBoardingConfigFactory
import com.codebasetemplate.required.ads.AppAdPlaceName
import com.codebasetemplate.required.firebase.GetDataFromRemoteUseCaseImpl
import com.codebasetemplate.required.shortcut.AppScreenType
import com.codebasetemplate.required.shortcut.AppShortCut
import com.core.ads.domain.AdFullScreenUiResource
import com.core.ads.domain.AdOpenAdUiResource
import com.core.ads.domain.ConsentFormUiResource
import com.core.ads.model.PreventShowManyInterstitialAds
import com.core.analytics.AnalyticsEvent
import com.core.baseui.countdown.JsgCountDownTimer
import com.core.baseui.ext.collectFlowOn
import com.core.config.data.FetchRemoteConfigState
import com.core.config.domain.data.AdType
import com.core.config.domain.data.IAdPlaceName
import com.core.preference.SharedPrefs
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

abstract class BaseSplashActivity<VB : ViewBinding> : CoreActivity<VB>() {

    @Inject
    lateinit var getDataFromRemoteUseCase: GetDataFromRemoteUseCaseImpl
    protected val baseViewModel by viewModels<SplashViewModel>()
    override val isWaitingAds: Boolean
        get() = true
    protected var timeShowIntro by SharedPrefs.instance.preference(
        defaultValue = 0L,
        key = "timeShowIntro"
    )

    protected var countDownTimer: JsgCountDownTimer? = null
    protected var delayedShowAdJob: Job? = null

    private val appOpenPlaceName by lazy {
        if (baseViewModel.isFirstOpenApp) {
            AppAdPlaceName.APP_OPEN_FIRST_OPEN
        } else {
            AppAdPlaceName.APP_OPEN
        }
    }

    private val interstitialPlaceName by lazy {
        if (baseViewModel.isFirstOpenApp) {
            AppAdPlaceName.ACTION_OPEN_APP_FIRST_OPEN
        } else {
            AppAdPlaceName.ACTION_OPEN_APP
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
        intent.extras?.getString(AppShortCut.KEY_SHORTCUT_TARGET_SCREEN, "")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            startSplashPrerequisites()
        } else {
            onSplashStatusChanged(SplashStatus.WaitingForInternet)
            showRequireTurnOnNetworkBottomSheetFragment()
        }
    }


    private fun initView() {
        when (targetScreenFromShortCut) {
            AppScreenType.Uninstall.screenName -> {
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
                add(AppAdPlaceName.ACTION_NEXT_IN_INTRODUCTION)
                add(AppAdPlaceName.ACTION_SKIP_IN_INTRODUCTION)
            }
        }
    }

    override fun providerPreloadBannerNativeAdPlaceName(): List<IAdPlaceName> {
        val isLoadLanguage =
            getCurrentLanguageCode().isBlank() || isAlwaysShowIntroAndLanguageScreen
        return mutableListOf<IAdPlaceName>().apply {
            if (isLoadLanguage) {
                add(AppAdPlaceName.ANCHORED_CHANGE_LANGUAGE_BOTTOM)
            }
            if (isEnableIntroductionScreen && isLoadLanguage) {
                addAll(
                    OnBoardingConfigFactory.getOnBoardingAdPlaceName(
                        getDataFromRemoteUseCase.onBoardingConfig,
                        remoteConfigRepository.getAppConfig()
                    )
                )
            }

            if (targetScreenFromShortCut == AppScreenType.Uninstall.screenName) {
                add(AppAdPlaceName.ANCHORED_UNINSTALL_BOTTOM_STEP_1)
                add(AppAdPlaceName.ANCHORED_UNINSTALL_BOTTOM_STEP_2)
            }

            add(AppAdPlaceName.ANCHORED_BOTTOM_HOME)
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

    private fun fetchSplashAds() {
        Log.d(TAG, "fetchSplashAds: 0")
        if (baseViewModel.isFirstOpenApp) {
            if (remoteConfigRepository.getSplashScreenConfig().adTypeFirstOpen == AdType.AppOpen) {
                Log.d(TAG, "fetchSplashAds: 1")
                appOpenAdManager.fetchAd(this, appOpenPlaceName)
            } else {
                Log.d(TAG, "fetchSplashAds: 2")
                adsManager.loadFullscreenAd(
                    this,
                    interstitialPlaceName,
                    isNeedUpdateAdPlace = true,
                    identifier = ""
                )
            }
        } else {
            if (remoteConfigRepository.getSplashScreenConfig().adType == AdType.AppOpen) {
                Log.d(TAG, "fetchSplashAds: 3")
                appOpenAdManager.fetchAd(this, appOpenPlaceName)
            } else {
                Log.d(TAG, "fetchSplashAds: 4")
                adsManager.loadFullscreenAd(
                    this,
                    interstitialPlaceName,
                    isNeedUpdateAdPlace = true,
                    identifier = ""
                )
            }
        }
    }

    private fun handleRemoteConfigReady() {
        AppShortCut.setUpShortCut(
            this,
            remoteConfigRepository.getAppConfig().isEnableAppShortCut,
            remoteConfigRepository.getAppConfig().isEnableAppShortcutUninstall
        )
        PreventShowManyInterstitialAds.initIntervalTimeShowInterstitialMillis()
        adsManager.startDisableAdCountDownTimer()
        baseViewModel.isRemoteConfigReady = true
        tryStartSplashAdsFlow()
    }

    private fun startSplashPrerequisites() {
        remoteConfigRepository.fetchAndActive()
        if (!baseViewModel.isRequestEuConsentComplete) {
            adsManager.requestConsentInfoUpdate(this, false)
        }
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
            targetScreenFromShortCut == AppScreenType.Uninstall.screenName -> {
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
        val nextScreen = {
            countDownTimer?.pauseTimer()
            appOpenAdManager.isFirstOpenApp = false
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
        onSplashStatusChanged(SplashStatus.ShowingAd)
        hideLoadingX()
        baseViewModel.handleWhenAdShowing()
    }

    private fun handleWhenAdDismissed() {
        Log.d(TAG, "handleWhenAdDismissed: ")
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
                        startSplashPrerequisites()
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
                startSplashPrerequisites()
            }
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
