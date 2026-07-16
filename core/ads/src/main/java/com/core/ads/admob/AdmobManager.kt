package com.core.ads.admob

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.util.Log
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.fragment.app.FragmentManager
import com.core.ads.AdsSdkInitializer
import com.core.ads.domain.AdFullScreenUiResource
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.ads.domain.AdsManager
import com.core.ads.domain.ConsentFormUiResource
import com.core.ads.domain.DialogNativeFakeInterstitial
import com.core.ads.model.AdHolder
import com.core.ads.model.AppOpenAdHolder
import com.core.ads.model.BannerAdHolder
import com.core.ads.model.InterstitialAdHolder
import com.core.ads.model.NativeAdHolder
import com.core.ads.model.PreventShowManyInterstitialAds
import com.core.ads.model.RewardedAdHolder
import com.core.ads.model.RewardedInterstitialAdHolder
import com.core.analytics.AdjustAnalytics
import com.core.analytics.AnalyticsManager
import com.core.config.domain.RemoteConfigRepository
import com.core.config.domain.data.AdPlace
import com.core.config.domain.data.AdType
import com.core.config.domain.data.BannerAdPlace
import com.core.config.domain.data.BannerSize
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.NativeAdPlace
import com.core.config.domain.data.NativeTemplateSize
import com.core.preference.AppPreferences
import com.core.preference.PurchasePreferences
import com.core.utilities.getBannerAdWidth
import com.core.utilities.getCountryCode
import com.core.utilities.getCurrentTimeInSecond
import com.core.utilities.manager.isNetworkConnected
import com.core.utilities.removeLoader
import com.core.utilities.removeLoaderNew
import com.core.utilities.showLoader
import com.core.utilities.showLoaderNew
import com.core.utilities.toMillis
import com.core.utilities.util.NetworkUtils
import com.core.utilities.util.Timber
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdapterResponseInfo
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AdmobManager"
private const val CONSENT_AND_MOBILE_ADS_TOTAL_TIMEOUT_MS = 10_000L

@Singleton
class AdmobManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val purchasePreferences: PurchasePreferences,
    private val analyticsManager: AnalyticsManager,
    private val appPref: AppPreferences,
    private val adsSdkInitializer: AdsSdkInitializer,
    private val adjustAnalytics: AdjustAnalytics
) : AdsManager {
    companion object {
        val adPlacesDisabledWhenDetectTestAd = ConcurrentHashMap.newKeySet<String>()
        var isTestAd: Boolean = false
            private set
    }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val isTurnOnAdPlacesDisabledWhenDetectTestAd: Boolean
        get() = remoteConfigRepository.isTurnOnAdPlacesDisabledWhenDetectTestAd()

    private val _isDisableAdDueManyClickFlow = MutableStateFlow(false)
    override val isDisableAdDueManyClickFlow = _isDisableAdDueManyClickFlow.asStateFlow()

    private val _adLoadBannerNativeFlow = MutableSharedFlow<AdLoadBannerNativeUiResource>()
    override val adLoadBannerNativeFlow = _adLoadBannerNativeFlow.asSharedFlow()

    private val _adFullScreenFlow = MutableSharedFlow<AdFullScreenUiResource>()
    override val adFullScreenFlow = _adFullScreenFlow.asSharedFlow()

    private val _requestConsentFlow = MutableSharedFlow<ConsentFormUiResource>()
    override val requestConsentFlow = _requestConsentFlow.asSharedFlow()

    private val adHolderBannerNativeMap = mutableMapOf<IAdPlaceName, AdHolder>()
    private val adHolderFullScreenMap = mutableMapOf<String, AdHolder>()
    private val adHolderAppOpenMap = mutableMapOf<IAdPlaceName, AppOpenAdHolder>()

    private val consentInformation: ConsentInformation by lazy {
        UserMessagingPlatform.getConsentInformation(context)
    }

    @Volatile
    private var isConsentCompleted = false

    @Volatile
    private var isConsentCompletionNotified = false

    @Volatile
    private var consentFlowStartElapsedRealtime = 0L

    private var isConsentRequesting = false

    private var disableAdCountDownTimer: CountDownTimer? = null

    override fun isHasFullscreenAdShowing(): Boolean {
        adHolderFullScreenMap.values.forEach { adHolder ->
            if (adHolder.isShowing) {
                return true
            }
        }

//        adHolderBannerNativeMap.values.forEach { adHolder ->
//            if (adHolder.isShowing) {
//                return true
//            }
//        }
        return isHasAppOpenAdShowing()
    }

    override fun isHasAppOpenAdShowing(): Boolean {
        adHolderAppOpenMap.values.forEach { adHolder ->
            if (adHolder.isShowing) {
                return true
            }
        }
        return false
    }

    override fun isNotAbleToVisibleAdsToUser(adPlaceName: IAdPlaceName): Boolean {
        if (!remoteConfigRepository.getAppConfig().isEnableAds) {
            Log.d(TAG, "isNotAbleToVisibleAdsToUser: isEnableAds = false")
            return true
        }

        if (isDisableByTestAd(adPlaceName.name)) {
            Log.d(
                TAG,
                "isNotAbleToVisibleAdsToUser: disabled by detected test ad ${adPlaceName.name}"
            )
            return true
        }

        if (purchasePreferences.isUserVip()) {
            Log.d(TAG, "isNotAbleToVisibleAdsToUser: isUserVip")
            return true
        }

        if (isCountryNotAvailableToShowAd()) {
            Log.d(TAG, "isNotAbleToVisibleAdsToUser: isCountryNotAvailableToShowAd")
            return true
        }

        if (isPreventShowAdsDueManyAdsClicked()) {
            Log.d(TAG, "isNotAbleToVisibleAdsToUser: isPreventShowAdsDueManyAdsClicked")
            return true
        }

        val adPlace = remoteConfigRepository.getAdPlaceBy(adPlaceName)
        if (adPlace.isNotValidToLoad()) {
            Log.d(TAG, "isNotAbleToVisibleAdsToUser: isNotValidToLoad")
            return true
        }

        return false
    }

    override fun increaseAdClickedCount() {
        val preventAdClickConfig = remoteConfigRepository.getPreventAdClickConfig()
        val currentTimeInSecond = getCurrentTimeInSecond()
        if (currentTimeInSecond - appPref.timeOfFirstAdClicked >= preventAdClickConfig.timePerSession) {
            // Reset click count if the current click time is out of ad session
            appPref.adClickedCount = 1
            appPref.timeOfFirstAdClicked = currentTimeInSecond
        } else {
            // Count ad clicked
            appPref.adClickedCount++
        }

        //  Reach max click in period of time, then all ads will be disabled
        if (appPref.adClickedCount >= preventAdClickConfig.maxAdClickPerSession) {
            // Add a time to the last time clicked to disable ads
            appPref.timeOfFirstAdClicked =
                currentTimeInSecond + preventAdClickConfig.timeDisableAdsWhenReachedMaxAdClick
            appPref.adClickedCount = 0
            startDisableAdCountDownTimer()
        }
    }

    override fun isRequestLocationInEeaOrUnknown(): Boolean {
        return consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    override fun requestConsentInfoUpdate(activity: Activity, isForceShow: Boolean) {
        if (isConsentRequesting) return
        isConsentRequesting = true
        isConsentCompleted = false
        isConsentCompletionNotified = false
        consentFlowStartElapsedRealtime = SystemClock.elapsedRealtime()
        applicationScope.launch {
            _requestConsentFlow.emit(ConsentFormUiResource.Loading)
        }
//        val requestConsentConfig = remoteConfigRepository.getRequestConsentConfig()

//        val debugSettingsBuilder = ConsentDebugSettings.Builder(context).apply {
//            if (requestConsentConfig.debugIsEEA) {
//                setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
//            } else {
//                setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_NOT_EEA)
//            }
//            requestConsentConfig.debugListTestDeviceHashedId.forEach { addTestDeviceHashedId(it) }
//            addTestDeviceHashedId("ED37AE717C4AA9D6D00A240AABBB250A")
//        }

        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
//            .setConsentDebugSettings(debugSettingsBuilder.build())
            .build()

        if (NetworkUtils.isInternetAvailable(activity)) {
            applicationScope.launch {
                val success = withTimeoutOrNull(CONSENT_AND_MOBILE_ADS_TOTAL_TIMEOUT_MS) {
                    suspendCancellableCoroutine<Boolean> { cont ->
                        consentInformation.requestConsentInfoUpdate(
                            activity,
                            params,
                            {
                                Timber.e("requestConsentInfoUpdate running")
//                                if (!requestConsentConfig.isEnable) {
//                                    onEuConsentComplete()
//                                    cont.resume(true) { _, _, _ ->}
//                                    return@requestConsentInfoUpdate
//                                }
                                if (consentInformation.isConsentFormAvailable) {
                                    displayConsentForm(activity, isForceShow)
                                    Timber.d("requestConsentInfoUpdate displayConsentForm")
                                } else {
                                    onEuConsentComplete()
                                    Timber.d("requestConsentInfoUpdate onEuConsentComplete")
                                }
                                cont.resume(true) { _, _, _ ->}
                            },
                            {
                                Timber.e("requestConsentInfoUpdate onEuConsentComplete")
                                onEuConsentComplete()
                                cont.resume(false) { _, _, _ ->}
                            }
                        )
                    }
                }

                if (success == null) { // timeout
                    Timber.e("requestConsentInfoUpdate timeout")
                    onEuConsentComplete()
                }
            }
        } else {
            onEuConsentComplete()
        }
    }

    override fun displayConsentForm(activity: Activity, isForceShow: Boolean) {
        UserMessagingPlatform.loadConsentForm(
            /* context = */ context,
            /* successListener = */ { consentForm ->
                isConsentRequesting = false
                if (isForceShow) {
                    if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED
                        || consentInformation.consentStatus == ConsentInformation.ConsentStatus.OBTAINED
                    ) {
                        applicationScope.launch {
                            _requestConsentFlow.emit(ConsentFormUiResource.Showing)
                        }
                        consentForm.show(activity) {
                            syncStoredGdprConsent()
                            applicationScope.launch {
                                _requestConsentFlow.emit(ConsentFormUiResource.Complete)
                            }
                        }
                    }
                    return@loadConsentForm
                }
                when (consentInformation.consentStatus) {
                    ConsentInformation.ConsentStatus.REQUIRED -> {
                        applicationScope.launch {
                            _requestConsentFlow.emit(ConsentFormUiResource.Showing)
                        }
                        consentForm.show(activity) {
                            syncStoredGdprConsent()
                            onEuConsentComplete()
                        }
                    }

                    else -> {
                        syncStoredGdprConsent()
                        onEuConsentComplete()
                    }
                }
            },
            /* failureListener = */ {
                syncStoredGdprConsent()
                onEuConsentComplete()
            }
        )
    }

    override fun isCanNotShowInterAd(adPlace: AdPlace): Boolean {
        if (adPlace.isIgnoreInterval) {
            return false
        }
        if (PreventShowManyInterstitialAds.isNotValidTimeToShow(remoteConfigRepository.getInterstitialAdConfig(), adPlace)) {
            return true
        }
        return false
    }

    override fun getAdRequest(isCollapsible: Boolean): AdRequest {
        val networkExtrasBundle = Bundle()
        if (isCollapsible) {
            networkExtrasBundle.putString("collapsible", "bottom")
        }
        return AdRequest.Builder()
            .addNetworkExtrasBundle(
                AdMobAdapter::class.java,
                networkExtrasBundle
            )
            .build()
    }

    override fun showAd(
        activity: Activity,
        fragmentManager: FragmentManager,
        adPlaceName: IAdPlaceName,
        identifier: String,
        isWaitLoadToShow: Boolean
    ) {
        val adPlace = remoteConfigRepository.getAdPlaceBy(adPlaceName)

        if (isNotAbleToVisibleAdsToUser(adPlaceName) || isHasFullscreenAdShowing()) {
            notifyAdFullScreenCompleted(adPlaceName, isHasFullscreenAdShowing())
            return
        }

        val adHolder = getOrCreateAdHolderFullScreenBy(adPlace, true)
        adHolder.needRetry = false
        if (adPlace.isRewardedVideoType()) {
            adHolder.isWaitLoadToShow =
                remoteConfigRepository.getRewardedAdConfig().isWaitLoadToShow
            adHolder.adPlace = adPlace
            showRewardedVideo(
                activity = activity,
                adHolder = adHolder as RewardedAdHolder,
            )
            return
        }

        if (adPlace.isRewardedInterstitialType()) {
            adHolder.isWaitLoadToShow =
                remoteConfigRepository.getRewardedInterstitialAdConfig().isWaitLoadToShow
            adHolder.adPlace = adPlace
            showRewardedInterstitialVideo(
                activity = activity,
                adHolder = adHolder as RewardedInterstitialAdHolder,
            )
            return
        }

        if (adPlace.isInterstitialType()) {
            adHolder.isWaitLoadToShow =
                remoteConfigRepository.getInterstitialAdConfig().isWaitLoadToShow || isWaitLoadToShow
            adHolder.adPlace = adPlace
            showInterstitial(
                activity = activity,
                adHolder = adHolder as InterstitialAdHolder,
            )
            return
        }

        if (adPlace.isNativeType()) {
            val adHolder = (getOrCreateAdBannerNativeHolderBy(adPlace) as? NativeAdHolder)
            if (adHolder == null || isDisableByTestAd(adPlaceName.name)) {
                notifyAdFullScreenCompleted(
                    adPlaceName = adPlaceName,
                    isShown = false,
                    isEarnedReward = false
                )
                return
            }
            val placeName = adHolder.adPlace.placeName

            val nativeAd = adHolder.nativeAd
            val nativePlace = adHolder.adPlace as? NativeAdPlace
            if (nativeAd == null) {
                notifyAdFullScreenCompleted(
                    adPlaceName = adPlaceName,
                    isShown = false,
                    isEarnedReward = false
                )
                return
            }
            adHolder.isShowing = true
            activity.showLoaderNew(backgroundColor = nativePlace?.backgroundFullColor?.toColorInt())
            DialogNativeFakeInterstitial.newInstance(adPlaceName).apply {
                onClose = {
                    adHolder.isShowing = false
                    notifyAdFullScreenCompleted(
                        adPlaceName = placeName,
                        isShown = true,
                        isEarnedReward = false
                    )
                    if (adHolder.adPlace.isAutoLoadAfterDismiss) {
                        loadBannerNativeAd(
                            activity = activity,
                            isReload = true,
                            identifier = identifier,
                            adPlaceName = adPlaceName,
                            isPreload = false
                        )
                    }
                    PreventShowManyInterstitialAds.increaseNumberOfShowingInterAdInSession()
                    PreventShowManyInterstitialAds.startCountDownTimerIfNeed(
                        remoteConfigRepository.getInterstitialAdConfig().timePerSession
                    )
                    PreventShowManyInterstitialAds.updateLastTimeShowedInterAd()
                    activity.removeLoaderNew()
                }
            }.show(fragmentManager, "DialogNativeFakeInterstitial")
            return
        }
    }

    override fun loadFullscreenAd(
        activity: Activity,
        adPlaceName: IAdPlaceName,
        identifier: String,
        isNeedUpdateAdPlace: Boolean,
        isRequestFromExternal: Boolean
    ) {
        val adPlace = remoteConfigRepository.getAdPlaceBy(adPlaceName)
        notifyAdFullScreenRequestShowing(adPlace)

        val adHolder = getOrCreateAdHolderFullScreenBy(adPlace, isNeedUpdateAdPlace)
        if (isRequestFromExternal) {
            adHolder.isWaitLoadToShow = false
            if (!context.isNetworkConnected()) {
                notifyAdFullScreenNotValidOrLoadFailed(adPlaceName)
                return
            }
        } else {
            if (!context.isNetworkConnected() && adHolder.isWaitLoadToShow) {
                notifyAdFullScreenCompleted(adPlaceName, false)
                return
            }
        }
        if (isNotAbleToVisibleAdsToUser(adPlaceName)) {
            if (!isRequestFromExternal && adHolder.isWaitLoadToShow) {
                notifyAdFullScreenCompleted(adPlaceName, false)
            }
            return
        }
        if (adPlace.isRewardedVideoType()) {
            loadRewardedIfNeed(
                activity = activity,
                adHolder = adHolder as RewardedAdHolder,
            )
            return
        }

        if (adPlace.isRewardedInterstitialType()) {
            loadRewardedInterstitialIfNeed(
                activity = activity,
                adHolder = adHolder as RewardedInterstitialAdHolder,
            )
            return
        }

        if (adPlace.isInterstitialType()) {
            loadInterstitialIfNeed(
                activity = activity,
                adHolder = adHolder as InterstitialAdHolder,
            )
            return
        }

        if (adPlace.isNativeType()) {
            loadBannerNativeAd(
                activity = activity,
                isReload = false,
                identifier = identifier,
                adPlaceName = adPlaceName,
                isPreload = false
            )
            return
        }
    }

    override fun loadBannerNativeAd(
        activity: Activity,
        adPlaceName: IAdPlaceName,
        identifier: String,
        isPreload: Boolean,
        isReload: Boolean
    ) {
        Log.d(TAG, "loadBannerNativeAd: ")
        val adPlace = remoteConfigRepository.getAdPlaceBy(adPlaceName)

        val adHolder = getOrCreateAdBannerNativeHolderBy(adPlace)
        adHolder.needRetry = true
        if (adPlace.isNativeType()) {
            loadNativeAdIfNeed(
                activity = activity,
                adHolder = adHolder as NativeAdHolder,
                isReload = isReload
            )
            return
        }

        if (adPlace.isBannerType()) {
            loadBannerAdIfNeed(
                activity = activity,
                adHolder = adHolder as BannerAdHolder,
                isPreload = isPreload,
                identifier = identifier
            )
            return
        }

    }

    override fun releaseBannerNative(adPlaceName: IAdPlaceName) {
        val adPlace = remoteConfigRepository.getAdPlaceBy(adPlaceName)
        val adHolder = getOrCreateAdBannerNativeHolderBy(adPlace)
        adHolder.reset()
        adHolder.needRetry = false
    }

    private fun showInterstitial(
        activity: Activity,
        adHolder: InterstitialAdHolder
    ) {
        val interstitialAd = adHolder.interstitialAd
        if (interstitialAd != null) {
            if (isCanNotShowInterAd(adHolder.adPlace) || isDisableByTestAd(adHolder.adPlace.placeName.name)) {
                notifyAdFullScreenCompleted(adHolder.adPlace.placeName, false)
            } else {
                adHolder.isShowing = true
                interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        super.onAdFailedToShowFullScreenContent(adError)
                        val placeName = adHolder.adPlace.placeName
                        Log.i(TAG, "Interstitial failed to show $placeName")
                        Firebase.crashlytics.log("Interstitial failed to show $placeName: ${adError.message}")
                        activity.removeLoader()
                        adHolder.reset()

                        if (adHolder.adPlace.isAutoLoadAfterDismiss) {
                            loadInterstitialIfNeed(activity, adHolder)
                        }

                        notifyAdFullScreenNotValidOrLoadFailed(placeName)
                        notifyAdFullScreenCompleted(placeName, false)
                    }

                    override fun onAdShowedFullScreenContent() {
                        super.onAdShowedFullScreenContent()
                        val placeName = adHolder.adPlace.placeName
                        Log.i(TAG, "Interstitial showed $placeName")
                        adHolder.isShowing = true
                        activity.showLoader()
                        notifyAdFullScreenSucceedToShow(placeName)
                        if(adHolder.adPlace.isTrackingShow) {
                            sendEventShow(adHolder.adPlace.placeName)
                        }
                    }

                    override fun onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent()
                        val placeName = adHolder.adPlace.placeName
                        Log.i(TAG, "Interstitial dismissed $placeName")
                        adHolder.isShowing = false
                        PreventShowManyInterstitialAds.increaseNumberOfShowingInterAdInSession()
                        PreventShowManyInterstitialAds.startCountDownTimerIfNeed(
                            remoteConfigRepository.getInterstitialAdConfig().timePerSession
                        )
                        PreventShowManyInterstitialAds.updateLastTimeShowedInterAd()
                        activity.removeLoader()

                        adHolder.reset()

                        if (adHolder.adPlace.isAutoLoadAfterDismiss) {
                            loadInterstitialIfNeed(activity, adHolder)
                        }

                        notifyAdFullScreenDismissed(
                            adPlaceName = placeName,
                            isEarnedReward = true,
                            amount = 0
                        )
                        notifyAdFullScreenCompleted(placeName, true)

                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        if(adHolder.adPlace.isTrackingClick) {
                            sendEventClick(adHolder.adPlace.placeName)
                        }
                        increaseAdClickedCount()
                    }
                }
                interstitialAd.show(activity)
            }
        } else {
            handleWhenRequestShowAdIfAdUnavailable(
                activity = activity,
                adHolder = adHolder,
            )
        }
    }

    private fun showRewardedInterstitialVideo(
        activity: Activity,
        adHolder: RewardedInterstitialAdHolder,
    ) {
        val rewardedInterstitialAd = adHolder.rewardedInterstitialAd
        if (rewardedInterstitialAd != null) {
            adHolder.isEarnedReward = false
            adHolder.isShowing = true
            rewardedInterstitialAd.fullScreenContentCallback =
                object : FullScreenContentCallback() {

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        super.onAdFailedToShowFullScreenContent(adError)
                        val placeName = adHolder.adPlace.placeName
                        Firebase.crashlytics.log("RewardedInterstitial failed to show $placeName: ${adError.message}")
                        Log.i(TAG, "RewardedInterstitial load failed $placeName")
                        activity.removeLoader()
                        adHolder.reset()

                        if (adHolder.adPlace.isAutoLoadAfterDismiss) {
                            loadRewardedInterstitialIfNeed(activity, adHolder)
                        }

                        notifyAdFullScreenNotValidOrLoadFailed(placeName)
                        notifyAdFullScreenCompleted(placeName, false)
                    }

                    override fun onAdShowedFullScreenContent() {
                        super.onAdShowedFullScreenContent()
                        val placeName = adHolder.adPlace.placeName
                        Log.i(TAG, "RewardedInterstitial showed $placeName")
                        adHolder.isShowing = true
                        activity.showLoader()
                        notifyAdFullScreenSucceedToShow(placeName)
                        if(adHolder.adPlace.isTrackingShow) {
                            sendEventShow(adHolder.adPlace.placeName)
                        }
                    }

                    override fun onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent()
                        val placeName = adHolder.adPlace.placeName
                        Log.i(TAG, "RewardedInterstitial dismissed $placeName")
                        adHolder.isShowing = false

                        if (adHolder.adPlace.isAutoLoadAfterDismiss) {
                            loadRewardedInterstitialIfNeed(activity, adHolder)
                        }
                        notifyAdFullScreenDismissed(
                            adPlaceName = placeName,
                            isEarnedReward = adHolder.isEarnedReward,
                            amount = adHolder.amount
                        )
                        notifyAdFullScreenCompleted(placeName, true, adHolder.isEarnedReward)
                        activity.removeLoader()
                        adHolder.reset()
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        if(adHolder.adPlace.isTrackingClick) {
                            sendEventClick(adHolder.adPlace.placeName)
                        }
                        increaseAdClickedCount()
                    }

                }
            rewardedInterstitialAd.show(activity) { rewardedItem ->
                Log.d(TAG, "showRewardedInterstitialVideo: $rewardedItem")
                adHolder.isEarnedReward = true
                adHolder.amount = rewardedItem.amount
            }
        } else {
            handleWhenRequestShowAdIfAdUnavailable(
                activity = activity,
                adHolder = adHolder
            )
        }
    }

    private fun showRewardedVideo(
        activity: Activity,
        adHolder: RewardedAdHolder,
    ) {
        val rewardedAd = adHolder.rewardedAd
        if (rewardedAd != null) {
            adHolder.isEarnedReward = false
            adHolder.isShowing = true
            rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    super.onAdFailedToShowFullScreenContent(adError)
                    val placeName = adHolder.adPlace.placeName
                    Log.i(TAG, "Rewarded failed to show $placeName")
                    Firebase.crashlytics.log("Rewarded failed to show $placeName: ${adError.message}")
                    activity.removeLoader()
                    adHolder.reset()

                    if (adHolder.adPlace.isAutoLoadAfterDismiss) {
                        loadRewardedIfNeed(activity, adHolder)
                    }

                    notifyAdFullScreenNotValidOrLoadFailed(placeName)
                    notifyAdFullScreenCompleted(placeName, false)
                }

                override fun onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent()
                    val placeName = adHolder.adPlace.placeName
                    Log.i(TAG, "Rewarded showed $placeName")
                    adHolder.isShowing = true
                    activity.showLoader()
                    notifyAdFullScreenSucceedToShow(placeName)
                    if(adHolder.adPlace.isTrackingShow) {
                        sendEventShow(adHolder.adPlace.placeName)
                    }
                }

                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    val placeName = adHolder.adPlace.placeName
                    Log.i(TAG, "Rewarded dismissed $placeName")
                    adHolder.isShowing = false
                    notifyAdFullScreenDismissed(
                        adPlaceName = placeName,
                        isEarnedReward = adHolder.isEarnedReward,
                        amount = adHolder.amount
                    )
                    notifyAdFullScreenCompleted(placeName, true, adHolder.isEarnedReward)
                    adHolder.reset()
                    activity.removeLoader()
                    if (adHolder.adPlace.isAutoLoadAfterDismiss) {
                        loadRewardedIfNeed(activity, adHolder)
                    }
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    if(adHolder.adPlace.isTrackingClick) {
                        sendEventClick(adHolder.adPlace.placeName)
                    }
                    increaseAdClickedCount()
                }

            }
            rewardedAd.show(activity) { rewardedItem ->
                adHolder.isEarnedReward = true
                adHolder.amount = rewardedItem.amount
            }
        } else {
            handleWhenRequestShowAdIfAdUnavailable(
                activity = activity,
                adHolder = adHolder
            )
        }
    }

    private fun handleWhenRequestShowAdIfAdUnavailable(
        activity: Activity,
        adHolder: AdHolder
    ) {
        val placeName = adHolder.adPlace.placeName

        if (adHolder.isLoading) {
            if (adHolder.isWaitLoadToShow) {
                activity.showLoader()
            } else {
                notifyAdFullScreenCompleted(placeName, false)
            }
        } else {
            if (adHolder.isWaitLoadToShow) {
                if (!context.isNetworkConnected()) {
                    notifyAdFullScreenCompleted(placeName, false)
                    adHolder.reset()
                    return
                }
                loadFullscreenAd(
                    activity,
                    placeName,
                    isRequestFromExternal = false,
                    identifier = ""
                )
            } else {
                notifyAdFullScreenCompleted(placeName, false)
            }
        }
    }

    private fun loadInterstitialIfNeed(
        activity: Activity,
        adHolder: InterstitialAdHolder,
    ) {
        if (adHolder.interstitialAd != null) {
            notifyAdFullScreenLoaded(adHolder.adPlace.placeName)
            return
        }
        if (adHolder.isLoading) {
            if (adHolder.isWaitLoadToShow) {
                activity.showLoader()
            }
            return
        }
        adHolder.isLoading = true

        val loadCallback = object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                val placeName = adHolder.adPlace.placeName
                Log.i(TAG, "Interstitial load failed $placeName ${p0.message}")
                adHolder.isLoading = false
                if (adHolder.isWaitLoadToShow) {
                    activity.removeLoader()
                    notifyAdFullScreenNotValidOrLoadFailed(placeName)
                    notifyAdFullScreenCompleted(placeName, false)
                    adHolder.reset()
                    return
                }
                if (!activity.isNetworkConnected()) {
                    notifyAdFullScreenNotValidOrLoadFailed(placeName)
                    adHolder.reset()
                    return
                }

                val interstitialAdConfig = remoteConfigRepository.getInterstitialAdConfig()
                val isEnableRetry = interstitialAdConfig.isEnableRetry
                val maxRetryCount = interstitialAdConfig.maxRetryCount
                val retryIntervalSecondList = interstitialAdConfig.retryIntervalSecondList
                if (isEnableRetry && adHolder.needRetry && retryIntervalSecondList.isNotEmpty()) {
                    when {
                        (adHolder.retryCount in 0 until maxRetryCount) -> {
                            Log.i(TAG, "Interstitial retry begin ${adHolder.retryCount} $placeName")
                            applicationScope.launch {
                                adHolder.retryCount++
                                val currentRetryCount = adHolder.retryCount
                                val retryDelay =
                                    if (currentRetryCount >= retryIntervalSecondList.size) {
                                        retryIntervalSecondList[retryIntervalSecondList.size - 1]
                                    } else {
                                        retryIntervalSecondList[currentRetryCount - 1]
                                    }
                                delay(retryDelay.toMillis())

                                if (adHolder.needRetry && !isFullScreenAdPlacedLoaded(adHolder.adPlace) && !adHolder.isLoading && !activity.isFinishing) {
                                    Log.i(
                                        TAG,
                                        "Interstitial retry load ${adHolder.retryCount} $placeName"
                                    )
                                    loadInterstitialIfNeed(activity, adHolder)
                                } else {
                                    Log.i(
                                        TAG,
                                        "Interstitial retry not valid ${adHolder.retryCount} $placeName"
                                    )
                                    notifyAdFullScreenNotValidOrLoadFailed(placeName)
                                }
                            }
                        }

                        else -> {
                            Log.i(TAG, "Interstitial retry exceeded count$placeName")
                            notifyAdFullScreenNotValidOrLoadFailed(placeName)
                            adHolder.reset()
                        }
                    }
                } else {
                    Log.i(TAG, "Interstitial not retry $placeName")
                    notifyAdFullScreenNotValidOrLoadFailed(placeName)
                    adHolder.reset()
                }

            }

            override fun onAdLoaded(p0: InterstitialAd) {
                super.onAdLoaded(p0)
                val placeName = adHolder.adPlace.placeName
                Log.i(TAG, "Interstitial loaded $placeName")
                adHolder.isLoading = false
                adHolder.interstitialAd = p0
                p0.setOnPaidEventListener { adValue ->
                    trackAdjustAdRevenue(
                        adUnitId = adHolder.adPlace.adId,
                        loadedAdapterResponseInfo = p0.responseInfo?.loadedAdapterResponseInfo,
                        adValueMicros = adValue.valueMicros,
                        adValueCurrencyCode = adValue.currencyCode
                    )
                }
                p0.setImmersiveMode(true)
                notifyAdFullScreenLoaded(placeName)
                if (adHolder.isWaitLoadToShow) {
                    activity.removeLoader()
                    adHolder.isWaitLoadToShow = false
                    showInterstitial(activity, adHolder)
                    return
                }
            }
        }
        InterstitialAd.load(
            activity,
            adHolder.adPlace.adId,
            getAdRequest(),
            loadCallback
        )
    }

    private fun loadRewardedInterstitialIfNeed(
        activity: Activity,
        adHolder: RewardedInterstitialAdHolder,
    ) {
        if (adHolder.rewardedInterstitialAd != null) {
            return
        }
        if (adHolder.isLoading) {
            return
        }
        adHolder.isLoading = true

        val loadCallback = object : RewardedInterstitialAdLoadCallback() {

            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                val placeName = adHolder.adPlace.placeName
                Log.i(TAG, "RewardedInterstitial load failed $placeName ${p0.message}")
                adHolder.isLoading = false
                if (adHolder.isWaitLoadToShow) {
                    activity.removeLoader()
                    notifyAdFullScreenNotValidOrLoadFailed(placeName)
                    notifyAdFullScreenCompleted(placeName, false)
                    adHolder.reset()
                    return
                }
                if (!activity.isNetworkConnected()) {
                    notifyAdFullScreenNotValidOrLoadFailed(placeName)
                    adHolder.reset()
                    return
                }

                val rewardedInterstitialAdConfig =
                    remoteConfigRepository.getRewardedInterstitialAdConfig()
                val isEnableRetry = rewardedInterstitialAdConfig.isEnableRetry
                val maxRetryCount = rewardedInterstitialAdConfig.maxRetryCount
                val retryIntervalSecondList = rewardedInterstitialAdConfig.retryIntervalSecondList
                if (isEnableRetry && adHolder.needRetry && retryIntervalSecondList.isNotEmpty()) {
                    when {
                        (adHolder.retryCount in 0 until maxRetryCount) -> {
                            Log.i(
                                TAG,
                                "RewardedInterstitial retry begin ${adHolder.retryCount} $placeName"
                            )
                            applicationScope.launch {
                                adHolder.retryCount++
                                val currentRetryCount = adHolder.retryCount
                                val retryDelay =
                                    if (currentRetryCount >= retryIntervalSecondList.size) {
                                        retryIntervalSecondList[retryIntervalSecondList.size - 1]
                                    } else {
                                        retryIntervalSecondList[currentRetryCount - 1]
                                    }
                                delay(retryDelay.toMillis())

                                if (adHolder.needRetry && !isFullScreenAdPlacedLoaded(adHolder.adPlace) && !adHolder.isLoading && !activity.isFinishing) {
                                    Log.i(
                                        TAG,
                                        "RewardedInterstitial retry load ${adHolder.retryCount} $placeName"
                                    )
                                    loadRewardedInterstitialIfNeed(activity, adHolder)
                                } else {
                                    Log.i(
                                        TAG,
                                        "RewardedInterstitial retry not valid ${adHolder.retryCount} $placeName"
                                    )
                                    notifyAdFullScreenNotValidOrLoadFailed(placeName)
                                }
                            }
                        }

                        else -> {
                            Log.i(TAG, "RewardedInterstitial retry exceeded count$placeName")
                            notifyAdFullScreenNotValidOrLoadFailed(placeName)
                            adHolder.reset()
                        }
                    }
                } else {
                    Log.i(TAG, "RewardedInterstitial not retry $placeName")
                    notifyAdFullScreenNotValidOrLoadFailed(placeName)
                    adHolder.reset()
                }

            }

            override fun onAdLoaded(p0: RewardedInterstitialAd) {
                super.onAdLoaded(p0)
                val placeName = adHolder.adPlace.placeName
                Log.i(TAG, "RewardedInterstitial loaded $placeName")
                adHolder.isLoading = false
                adHolder.rewardedInterstitialAd = p0
                p0.setOnPaidEventListener { adValue ->
                    trackAdjustAdRevenue(
                        adUnitId = adHolder.adPlace.adId,
                        loadedAdapterResponseInfo = p0.responseInfo?.loadedAdapterResponseInfo,
                        adValueMicros = adValue.valueMicros,
                        adValueCurrencyCode = adValue.currencyCode
                    )
                }
                p0.setImmersiveMode(true)
                notifyAdFullScreenLoaded(placeName)
                if (adHolder.isWaitLoadToShow) {
                    adHolder.isWaitLoadToShow = false
                    showRewardedInterstitialVideo(activity, adHolder)
                    return
                }
            }
        }

        RewardedInterstitialAd.load(
            activity,
            adHolder.adPlace.adId,
            getAdRequest(),
            loadCallback
        )
    }

    private fun loadRewardedIfNeed(
        activity: Activity,
        adHolder: RewardedAdHolder,
    ) {
        if (adHolder.rewardedAd != null) {
            return
        }
        if (adHolder.isLoading) {
            return
        }
        adHolder.isLoading = true

        val loadCallback = object : RewardedAdLoadCallback() {

            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                val placeName = adHolder.adPlace.placeName
                Log.i(TAG, "Rewarded load failed $placeName ${p0.message}")
                adHolder.isLoading = false
                if (adHolder.isWaitLoadToShow) {
                    notifyAdFullScreenNotValidOrLoadFailed(placeName)
                    notifyAdFullScreenCompleted(placeName, false)
                    adHolder.reset()
                    return
                }
                if (!activity.isNetworkConnected()) {
                    notifyAdFullScreenNotValidOrLoadFailed(placeName)
                    adHolder.reset()
                    return
                }
                val rewardedAdConfig = remoteConfigRepository.getRewardedAdConfig()
                val isEnableRetry = rewardedAdConfig.isEnableRetry
                val maxRetryCount = rewardedAdConfig.maxRetryCount
                val retryIntervalSecondList = rewardedAdConfig.retryIntervalSecondList
                if (isEnableRetry && adHolder.needRetry && retryIntervalSecondList.isNotEmpty()) {
                    when {
                        (adHolder.retryCount in 0 until maxRetryCount) -> {
                            Log.i(TAG, "Rewarded retry begin ${adHolder.retryCount} $placeName")
                            applicationScope.launch {
                                adHolder.retryCount++
                                val currentRetryCount = adHolder.retryCount
                                val retryDelay =
                                    if (currentRetryCount >= retryIntervalSecondList.size) {
                                        retryIntervalSecondList[retryIntervalSecondList.size - 1]
                                    } else {
                                        retryIntervalSecondList[currentRetryCount - 1]
                                    }
                                delay(retryDelay.toMillis())

                                if (adHolder.needRetry && !isFullScreenAdPlacedLoaded(adHolder.adPlace) && !adHolder.isLoading && !activity.isFinishing) {
                                    Log.i(
                                        TAG,
                                        "Rewarded retry load ${adHolder.retryCount} $placeName"
                                    )
                                    loadRewardedIfNeed(activity, adHolder)
                                } else {
                                    Log.i(
                                        TAG,
                                        "Rewarded retry not valid ${adHolder.retryCount} $placeName"
                                    )
                                    notifyAdFullScreenNotValidOrLoadFailed(placeName)
                                }
                            }
                        }

                        else -> {
                            Log.i(TAG, "Rewarded retry exceeded count$placeName")
                            notifyAdFullScreenNotValidOrLoadFailed(placeName)
                            adHolder.reset()
                        }
                    }
                } else {
                    Log.i(TAG, "Rewarded not retry $placeName")
                    notifyAdFullScreenNotValidOrLoadFailed(placeName)
                    adHolder.reset()
                }
            }

            override fun onAdLoaded(p0: RewardedAd) {
                super.onAdLoaded(p0)
                val placeName = adHolder.adPlace.placeName
                Log.i(TAG, "Rewarded loaded $placeName")
                adHolder.isLoading = false
                adHolder.rewardedAd = p0
                p0.setOnPaidEventListener { adValue ->
                    trackAdjustAdRevenue(
                        adUnitId = adHolder.adPlace.adId,
                        loadedAdapterResponseInfo = p0.responseInfo?.loadedAdapterResponseInfo,
                        adValueMicros = adValue.valueMicros,
                        adValueCurrencyCode = adValue.currencyCode
                    )
                }
                p0.setImmersiveMode(true)
                notifyAdFullScreenLoaded(placeName)
                if (adHolder.isWaitLoadToShow) {
                    activity.removeLoader()
                    adHolder.isWaitLoadToShow = false
                    showRewardedVideo(activity, adHolder)
                    return
                }
            }

        }

        RewardedAd.load(
            activity,
            adHolder.adPlace.adId,
            getAdRequest(),
            loadCallback
        )
    }

    private fun loadNativeAdIfNeed(
        activity: Activity,
        adHolder: NativeAdHolder,
        isReload: Boolean
    ) {
        val placeName = adHolder.adPlace.placeName
        if (isNotAbleToVisibleAdsToUser(placeName)) {
            notifyBannerNativeFailedToLoad(placeName)
            adHolder.reset()
            return
        }
        val nativeAdPlace = adHolder.adPlace as NativeAdPlace

        val nativeAd = adHolder.nativeAd
        val isAdExpired = adHolder.isAdExpired((nativeAdPlace.expiredTimeSecond ?: remoteConfigRepository.getNativeAdConfig().expiredTimeSecond).toLong() * 1_000L) || isReload
        if (nativeAd != null) {
            notifyNativeLoaded(nativeAd, nativeAdPlace)
            if(!isAdExpired) {
                return
            }
        }
        Log.d(TAG, "loadNativeAdIfNeed: isReload $isReload")

        if (activity.isDestroyed) {
//            notifyBannerNativeFailedToLoad(placeName)
            adHolder.reset()
            return
        }
        if(nativeAd == null) {
            notifyBannerNativeLoading(
                placeName,
                AdType.Native,
                BannerSize.Anchored,
                nativeAdPlace.nativeTemplateSize
            )
        }
        /*if (!context.isNetworkConnected()) {
            Log.d(TAG, "loadNativeAdIfNeed: isNetworkDisconnect")
            notifyBannerNativeNetworkError(placeName)
            return
        }*/
        if (adHolder.isLoading) {
            Log.d(TAG, "loadNativeAdIfNeed: isLoading")
            return
        }
        adHolder.isLoading = true

        applicationScope.launch {
            val adLoader = withContext(Dispatchers.IO) {
                AdLoader.Builder(activity, adHolder.adPlace.adId)
                    .forNativeAd { ad: NativeAd ->
                        Log.i(TAG, "Native loaded $placeName")
                        Log.i(TAG, "Native loaded $placeName ${ad.headline}")
                        val isCurrentAdTest = TestNativeAdHeadlineUtils.containsBlockedHeadline(ad.headline)
                        if (isTurnOnAdPlacesDisabledWhenDetectTestAd && isCurrentAdTest) {
                            markAdPlacesDisabledWhenDetectTestAd()
                            Log.i(TAG, "Native loaded block ad test $placeName ${ad.headline}")
                            isTestAd = true
                        }
                        adHolder.nativeAd = ad
                        adHolder.loadedAtMs = SystemClock.elapsedRealtime()
                        ad.setOnPaidEventListener { adValue ->
                            trackAdjustAdRevenue(
                                adUnitId = adHolder.adPlace.adId,
                                loadedAdapterResponseInfo = ad.responseInfo?.loadedAdapterResponseInfo,
                                adValueMicros = adValue.valueMicros,
                                adValueCurrencyCode = adValue.currencyCode
                            )
                        }

                        if (isNotAbleToVisibleAdsToUser(placeName)) {
                            notifyBannerNativeFailedToLoad(placeName)
                            adHolder.reset()
                        } else {
                            notifyNativeLoaded(ad, nativeAdPlace)
                            if (adHolder.adPlace.isTrackingShow) {
                                sendEventShow(adHolder.adPlace.placeName)
                            }
                        }
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdLoaded() {
                            super.onAdLoaded()
                            adHolder.isLoading = false
                        }

                        override fun onAdFailedToLoad(p0: LoadAdError) {
                            super.onAdFailedToLoad(p0)
                            Log.i(TAG, "Native load failed $placeName ${p0.message} $p0")
                            adHolder.isLoading = false

                            val nativeAdConfig = remoteConfigRepository.getNativeAdConfig()
                            val isEnableRetry = nativeAdConfig.isEnableRetry
                            val maxRetryCount = nativeAdConfig.maxRetryCount
                            val retryIntervalSecondList = nativeAdConfig.retryIntervalSecondList
                            if (isEnableRetry && adHolder.needRetry && retryIntervalSecondList.isNotEmpty()) {
                                when {
                                    (adHolder.retryCount in 0 until maxRetryCount) -> {
                                        Log.i(
                                            TAG,
                                            "Native retry begin ${adHolder.retryCount} $placeName"
                                        )
                                        applicationScope.launch {
                                            adHolder.retryCount++
                                            val currentRetryCount = adHolder.retryCount
                                            val retryDelay =
                                                if (currentRetryCount >= retryIntervalSecondList.size) {
                                                    retryIntervalSecondList[retryIntervalSecondList.size - 1]
                                                } else {
                                                    retryIntervalSecondList[currentRetryCount - 1]
                                                }
                                            delay(retryDelay.toMillis())

                                            if (adHolder.needRetry && !isBannerNativeAdPlacedLoaded(
                                                    adHolder.adPlace
                                                ) && !adHolder.isLoading && !activity.isFinishing
                                            ) {
                                                Log.i(
                                                    TAG,
                                                    "Native retry load ${adHolder.retryCount} $placeName"
                                                )
                                                loadNativeAdIfNeed(activity, adHolder, isReload)
                                            } else {
                                                Log.i(
                                                    TAG,
                                                    "Native retry not valid ${adHolder.retryCount} $placeName"
                                                )
//                                        notifyBannerNativeFailedToLoad(placeName)
                                            }
                                        }
                                    }

                                    else -> {
                                        Log.i(TAG, "Native retry exceeded count$placeName")
                                        if (nativeAdConfig.isHideWhenError) {
                                            notifyBannerNativeFailedToLoad(placeName)
                                        }
                                        adHolder.reset()
                                    }
                                }
                            } else {
                                Log.i(TAG, "Native not retry $placeName")
//                        notifyBannerNativeFailedToLoad(placeName)
                                adHolder.reset()
                            }
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            if(adHolder.adPlace.isTrackingClick) {
                                analyticsManager.logEvent(adHolder.adPlace.placeName.name + "_Clicked")
                            }
                            increaseAdClickedCount()
                        }
                    })
                    .withNativeAdOptions(
                        NativeAdOptions.Builder()
                            .setRequestCustomMuteThisAd(true)
                            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                            .setVideoOptions(
                                VideoOptions.Builder().setStartMuted(true).setCustomControlsRequested(true).build()
                            )
                            .build()
                    )
                    .build()
            }
            adLoader.loadAd(getAdRequest())
        }
    }

    private fun markAdPlacesDisabledWhenDetectTestAd() {
        val adPlaceNames = remoteConfigRepository.getAdPlacesDisableWhenDetectTestAd()
        if (adPlaceNames.isEmpty() || adPlacesDisabledWhenDetectTestAd.isNotEmpty()) {
            Log.d(TAG, "markAdPlacesDisabledWhenDetectTestAd: no ad place configured")
            return
        }
        adPlacesDisabledWhenDetectTestAd.addAll(adPlaceNames)
        Log.d(TAG, "markAdPlacesDisabledWhenDetectTestAd: $adPlaceNames")
    }

    private fun loadBannerAdIfNeed(
        activity: Activity,
        adHolder: BannerAdHolder,
        isPreload: Boolean,
        identifier: String
    ) {
        val placeName = adHolder.adPlace.placeName
        if (isNotAbleToVisibleAdsToUser(placeName)) {
            notifyBannerNativeFailedToLoad(placeName)
            adHolder.reset()
            return
        }

        val bannerAdPlace = adHolder.adPlace as BannerAdPlace
        var bannerAd = adHolder.bannerAd
        Timber.d("loadBannerAdIfNeed: ${adHolder.bannerAd} ${bannerAd?.parent} $this")
//        if (bannerAd != null && bannerAd.parent == null && !bannerAd.isShown) {
        if (bannerAd != null && !bannerAdPlace.isCollapsible) {
            val parentAdView = bannerAd.parent as? ViewGroup
            if (parentAdView == null) {
                notifyBannerLoaded(bannerAd, bannerAdPlace)
                return
            }
            if (parentAdView.isAttachedToWindow) {
                Timber.d("loadBannerAdIfNeed: banner already attached $placeName")
                return
            }
            runCatching { parentAdView.endViewTransition(bannerAd) }
            parentAdView.layoutTransition = null
            parentAdView.removeView(bannerAd)
            adHolder.bannerAd = null
            bannerAd = null
        }

        if (bannerAd != null && bannerAd.parent == null) {
            notifyBannerLoaded(bannerAd, bannerAdPlace)
            return
        }

        // nếu là collapsible banner thì không được preload banner vì nó sẽ không hiện collapsible khi load trước
        if(bannerAdPlace.isCollapsible && isPreload) {
            adHolder.reset()
            return
        }

        if(bannerAdPlace.isCollapsible && bannerAd != null && identifier == adHolder.identifier && !bannerAdPlace.autoReloadCollapsible) {
            return
        }


        if (activity.isDestroyed) {
//            notifyBannerNativeFailedToLoad(placeName)
            adHolder.reset()
            return
        }


        notifyBannerNativeLoading(
            placeName,
            AdType.Banner,
            bannerAdPlace.bannerSize,
            NativeTemplateSize.Small
        )
        /*if (!context.isNetworkConnected()) {
            notifyBannerNativeNetworkError(placeName)
            return
        }*/
        if (adHolder.isLoading) {
            return
        }
        adHolder.isLoading = true

        val adSize = when (bannerAdPlace.bannerSize) {
            BannerSize.Anchored -> AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                context,
                activity.getBannerAdWidth()
            )

            BannerSize.AnchoredLarge -> AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, activity.getBannerAdWidth())

            BannerSize.Inline -> AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(
                context,
                activity.getBannerAdWidth()
            )

            BannerSize.StandardMedium -> AdSize.MEDIUM_RECTANGLE
            BannerSize.StandardLarge -> AdSize.LARGE_BANNER
        }

        adHolder.identifier = identifier
        bannerAd = AdView(activity).apply {
            adUnitId = adHolder.adPlace.adId
            setAdSize(adSize)
            setOnPaidEventListener { adValue ->
                trackAdjustAdRevenue(
                    adUnitId = adHolder.adPlace.adId,
                    loadedAdapterResponseInfo = responseInfo?.loadedAdapterResponseInfo,
                    adValueMicros = adValue.valueMicros,
                    adValueCurrencyCode = adValue.currencyCode
                )
            }
            adListener = object : AdListener() {
                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    Log.i(TAG, "Banner loaded failed $placeName ${p0.message} $p0")
                    adHolder.isLoading = false

                    val bannerAdConfig = remoteConfigRepository.getBannerAdConfig()
                    val isEnableRetry = bannerAdConfig.isEnableRetry
                    val maxRetryCount = bannerAdConfig.maxRetryCount
                    val retryIntervalSecondList = bannerAdConfig.retryIntervalSecondList
                    if (isEnableRetry && adHolder.needRetry && retryIntervalSecondList.isNotEmpty()) {
                        when {
                            (adHolder.retryCount in 0 until maxRetryCount) -> {
                                Log.i(TAG, "Banner retry begin ${adHolder.retryCount} $placeName")
                                applicationScope.launch {
                                    adHolder.retryCount++
                                    val currentRetryCount = adHolder.retryCount
                                    val retryDelay =
                                        if (currentRetryCount >= retryIntervalSecondList.size) {
                                            retryIntervalSecondList[retryIntervalSecondList.size - 1]
                                        } else {
                                            retryIntervalSecondList[currentRetryCount - 1]
                                        }
                                    delay(retryDelay.toMillis())

                                    if (adHolder.needRetry && !isBannerNativeAdPlacedLoaded(adHolder.adPlace) && !adHolder.isLoading && !activity.isFinishing) {
                                        Log.i(
                                            TAG,
                                            "Banner retry load ${adHolder.retryCount} $placeName"
                                        )
                                        loadBannerAdIfNeed(activity, adHolder, isPreload, identifier)
                                    } else {
                                        Log.i(
                                            TAG,
                                            "Banner retry not valid ${adHolder.retryCount} $placeName"
                                        )
//                                        notifyBannerNativeFailedToLoad(placeName)
                                    }
                                }
                            }

                            else -> {
                                Log.i(TAG, "Banner retry exceeded count$placeName")
                                if (bannerAdConfig.isHideWhenError) {
                                    notifyBannerNativeFailedToLoad(placeName)
                                }
                                adHolder.reset()
                            }
                        }
                    } else {
                        Log.i(TAG, "Banner not retry $placeName")
//                        notifyBannerNativeFailedToLoad(placeName)
                        adHolder.reset()
                    }
                }

                override fun onAdLoaded() {
                    super.onAdLoaded()
                    Log.i(TAG, "Banner loaded $placeName")
                    adHolder.isLoading = false
                    adHolder.bannerAd = bannerAd

                    if (isNotAbleToVisibleAdsToUser(placeName)) {
                        notifyBannerNativeFailedToLoad(placeName)
                        adHolder.reset()
                    } else {
                        notifyBannerLoaded(this@apply, bannerAdPlace)
                        if (adHolder.adPlace.isTrackingShow) {
                            sendEventShow(adHolder.adPlace.placeName)
                        }
                    }
                }

                override fun onAdClosed() {
                    super.onAdClosed()
                    //adHolder.reset()
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    if(adHolder.adPlace.isTrackingClick) {
                        sendEventClick(adHolder.adPlace.placeName)
                    }
                    increaseAdClickedCount()
                }
            }
        }
        bannerAd.loadAd(getAdRequest(bannerAdPlace.isCollapsible))
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

    private fun sendAdEvent(adPlaceName: IAdPlaceName, action: String) {
        runCatching {
            val suffix = "_$action"
            val maxLength = 40 - suffix.length // Firebase limit = 40
            val baseName = adPlaceName.name

            // Cắt từ cuối, đảm bảo không vượt quá maxLength
            val safeBase = if (baseName.length > maxLength) {
                baseName.takeLast(maxLength)
            } else {
                baseName
            }

            // Chỉ giữ lại ký tự hợp lệ [A-Za-z0-9_], thay cái khác bằng "_"
            val safeName = safeBase.replace(Regex("[^A-Za-z0-9_]"), "_")

            analyticsManager.logEvent("${safeName}$suffix")
        }
    }

    fun sendEventClick(adPlaceName: IAdPlaceName) =
        sendAdEvent(adPlaceName, "Clicked")

    fun sendEventShow(adPlaceName: IAdPlaceName) =
        sendAdEvent(adPlaceName, "Showed")

    private fun notifyConsentCompleteOnce() {
        if (isConsentCompletionNotified) return
        isConsentCompletionNotified = true
        applicationScope.launch {
            _requestConsentFlow.emit(ConsentFormUiResource.Complete)
        }
    }

    private fun onEuConsentComplete() {
        if (isConsentCompleted) return
        isConsentCompleted = true
        isConsentRequesting = false
        val elapsed = SystemClock.elapsedRealtime() - consentFlowStartElapsedRealtime
        val remainingTimeout =
            (CONSENT_AND_MOBILE_ADS_TOTAL_TIMEOUT_MS - elapsed).coerceAtLeast(0L)
        applicationScope.launch {
            delay(remainingTimeout)
            if (!isConsentCompletionNotified) {
                Log.w(TAG,"Consent + MobileAds.initialize timeout, continue splash flow without waiting")
                notifyConsentCompleteOnce()
            }
        }
        MobileAds.initialize(context) { initializationStatus ->
            val statusMap =
                initializationStatus.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val status = statusMap[adapterClass]
                Log.d(TAG, "Adapter name: $adapterClass, Description: ${status!!.description}, Latency: ${status.initializationState}")
            }
            adsSdkInitializer.onAdInitCompleted()
            notifyConsentCompleteOnce()
        }
        /* MobileAds.openAdInspector(context) {
             Log.d(TAG, "openAdInspector ${it?.domain}")
             // Error will be non-null if ad inspector closed due to an error.
         }*/

//        val requestConfiguration = MobileAds.getRequestConfiguration()
//            .toBuilder()
//            .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
//            .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
//            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_MA)
//            .build()
//        MobileAds.setRequestConfiguration(requestConfiguration)

//        val userSetting = AppLovinSdkSettings(context)
//        AppLovinSdk.getInstance(
//            "msLyqWhAdgsOcgT0IvrtaoBq3Mw8_pAvcNOf7N4hs8G5kAMKLJORQBlbwIGbd7OPop6t_oP2uzkSlcm3zyEsa4",
//            userSetting,
//            context
//        ).initializeSdk()
//        AppLovinPrivacySettings.setIsAgeRestrictedUser(false, context)
//        AppLovinPrivacySettings.setDoNotSell(true, context)
//
//        val ccpaMetaData = MetaData(context)
//        ccpaMetaData["privacy.consent"] = true
//        ccpaMetaData.commit()

//        MBridgeSDKFactory.getMBridgeSDK().apply {
//            setConsentStatus(context, MBridgeConstans.IS_SWITCH_ON)
//            setDoNotTrackStatus(false)
//        }

    }

    private fun syncStoredGdprConsent() {
        val consentGranted = when (consentInformation.consentStatus) {
            ConsentInformation.ConsentStatus.NOT_REQUIRED,
            ConsentInformation.ConsentStatus.OBTAINED -> true
            ConsentInformation.ConsentStatus.REQUIRED,
            ConsentInformation.ConsentStatus.UNKNOWN -> false
            else -> false
        }
        adsSdkInitializer.onUpdateGdprConsent(consentGranted)
    }

    private fun isPreventShowAdsDueManyAdsClicked() =
        getCurrentTimeInSecond() < appPref.timeOfFirstAdClicked

    private fun isCountryNotAvailableToShowAd(): Boolean {
        return remoteConfigRepository.getAdsDisableByCountry().contains(context.getCountryCode())
    }

    override fun setupAppOpenAdDefaultValue() {
        adHolderAppOpenMap.values.forEach { adHolder ->
            adHolder.isShowing = false
        }
    }

    override fun getNativeHolder(activity: Activity, adPlaceName: IAdPlaceName): NativeAdHolder? {
        if (activity.isDestroyed) return null
        return adHolderBannerNativeMap[adPlaceName] as? NativeAdHolder
    }

    override fun getOrCreateAppOpenAdHolderBy(adPlace: AdPlace): AppOpenAdHolder {
        var adHolder = adHolderAppOpenMap[adPlace.placeName]
        if (adHolder == null || adHolder.adPlace.adType != adPlace.adType) {
            adHolder = AppOpenAdHolder(adPlace = adPlace)
            adHolderAppOpenMap[adPlace.placeName] = adHolder
        }
        adHolder.adPlace = adPlace
        return adHolder
    }

    private fun getOrCreateAdBannerNativeHolderBy(adPlace: AdPlace): AdHolder {
        var adHolder = adHolderBannerNativeMap[adPlace.placeName]
        if (adHolder == null || adHolder.adPlace.adType != adPlace.adType) {
            adHolder = when (adPlace.adType) {
                AdType.Banner -> BannerAdHolder(adPlace = adPlace)
                AdType.Native -> NativeAdHolder(adPlace = adPlace)
//                AdType.AppOpen -> AppOpenAdHolder(adPlace = adPlace)
//                else -> RewardedAdHolder(adPlace = adPlace)
                else -> NativeAdHolder(adPlace = adPlace)
            }
            adHolderBannerNativeMap[adPlace.placeName] = adHolder
        }
        adHolder.adPlace = adPlace
        return adHolder
    }

    private fun getOrCreateAdHolderFullScreenBy(
        adPlace: AdPlace,
        isNeedUpdateAdPlace: Boolean
    ): AdHolder {
        var adHolder = adHolderFullScreenMap[adPlace.adId]
        if (adHolder == null) {
            adHolder = when (adPlace.adType) {
                AdType.Interstitial -> InterstitialAdHolder(adPlace = adPlace)
                AdType.RewardedInterstitial -> RewardedInterstitialAdHolder(adPlace = adPlace)
                AdType.RewardedVideo -> RewardedAdHolder(adPlace = adPlace)
                else -> RewardedAdHolder(adPlace = adPlace)
            }
            adHolderFullScreenMap[adPlace.adId] = adHolder
        }
        if (isNeedUpdateAdPlace) {
            adHolder.adPlace = adPlace
        }
        return adHolder
    }

    private fun isFullScreenAdPlacedLoaded(adPlace: AdPlace): Boolean {
        val adHolder = adHolderFullScreenMap[adPlace.adId] ?: return false
        return when (adPlace.adType) {
            AdType.Interstitial -> adHolder is InterstitialAdHolder && adHolder.interstitialAd != null
            AdType.RewardedInterstitial -> adHolder is RewardedInterstitialAdHolder && adHolder.rewardedInterstitialAd != null
            AdType.RewardedVideo -> adHolder is RewardedAdHolder && adHolder.rewardedAd != null
            else -> false
        }
    }

    private fun isBannerNativeAdPlacedLoaded(adPlace: AdPlace): Boolean {
        val adHolder = adHolderBannerNativeMap[adPlace.placeName] ?: return false
        return when (adPlace.adType) {
            AdType.Banner -> adHolder is BannerAdHolder && adHolder.bannerAd != null
            AdType.Native -> adHolder is NativeAdHolder && adHolder.nativeAd != null
            else -> false
        }
    }

    private fun notifyBannerNativeLoading(
        placeName: IAdPlaceName,
        adType: AdType,
        bannerSize: BannerSize,
        nativeTemplateSize: NativeTemplateSize
    ) {
        applicationScope.launch {
            _adLoadBannerNativeFlow.emit(
                AdLoadBannerNativeUiResource.Loading(
                    placeName,
                    adType,
                    bannerSize,
                    nativeTemplateSize
                )
            )
        }
    }

    private fun notifyBannerNativeFailedToLoad(placeName: IAdPlaceName) {
        applicationScope.launch {
            _adLoadBannerNativeFlow.emit(AdLoadBannerNativeUiResource.AdFailed(placeName))
        }
    }

    private fun notifyBannerLoaded(bannerAd: AdView, bannerAdPlace: BannerAdPlace) {
        applicationScope.launch {
            _adLoadBannerNativeFlow.emit(
                AdLoadBannerNativeUiResource.BannerAdLoaded(
                    bannerAd,
                    bannerAdPlace.placeName,
                    bannerAdPlace
                )
            )
        }
    }

    private fun notifyNativeLoaded(nativeAd: NativeAd, nativeAdPlace: NativeAdPlace) {
        applicationScope.launch {
            _adLoadBannerNativeFlow.emit(
                AdLoadBannerNativeUiResource.NativeAdLoaded(
                    nativeAd,
                    nativeAdPlace.placeName,
                    nativeAdPlace
                )
            )
        }
    }

    private fun notifyBannerNativeNetworkError(adPlaceName: IAdPlaceName) {
        applicationScope.launch {
            _adLoadBannerNativeFlow.emit(
                AdLoadBannerNativeUiResource.AdNetworkError(
                    adPlaceName
                )
            )
        }
    }

    private fun notifyAdFullScreenRequestShowing(adPlace: AdPlace) {
        applicationScope.launch {
            _adFullScreenFlow.emit(AdFullScreenUiResource.AdRequestInfo(adPlace))
        }
    }

    private fun notifyAdFullScreenLoaded(adPlaceName: IAdPlaceName) {
        applicationScope.launch {
            _adFullScreenFlow.emit(AdFullScreenUiResource.AdLoaded(adPlaceName))
        }
    }

    private fun notifyAdFullScreenNotValidOrLoadFailed(adPlaceName: IAdPlaceName) {
        applicationScope.launch {
            _adFullScreenFlow.emit(AdFullScreenUiResource.AdNotValidOrLoadFailed(adPlaceName))
        }
    }

    private fun notifyAdFullScreenSucceedToShow(adPlaceName: IAdPlaceName) {
        applicationScope.launch {
            _adFullScreenFlow.emit(AdFullScreenUiResource.AdSucceedToShow(adPlaceName))
        }
    }

    private fun notifyAdFullScreenDismissed(
        adPlaceName: IAdPlaceName,
        isEarnedReward: Boolean,
        amount: Int
    ) {
        applicationScope.launch {
            _adFullScreenFlow.emit(
                AdFullScreenUiResource.AdDismissed(
                    adPlaceName = adPlaceName,
                    isEarnedReward = isEarnedReward,
                    amount = amount
                )
            )
        }
    }

    private fun notifyAdFullScreenCompleted(adPlaceName: IAdPlaceName, isShown: Boolean, isEarnedReward: Boolean = true) {
        applicationScope.launch {
            _adFullScreenFlow.emit(AdFullScreenUiResource.AdCompleted(adPlaceName, isShown, isEarnedReward))
        }
    }

    override fun startDisableAdCountDownTimer() {
        if (!isPreventShowAdsDueManyAdsClicked()) {
            _isDisableAdDueManyClickFlow.value = false
            return
        }
        _isDisableAdDueManyClickFlow.value = true
        if (disableAdCountDownTimer != null) {
            disableAdCountDownTimer?.cancel()
            disableAdCountDownTimer = null
        }
        val timer = (appPref.timeOfFirstAdClicked - getCurrentTimeInSecond()) * 1000L
        disableAdCountDownTimer = object : CountDownTimer(timer, 1000) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                _isDisableAdDueManyClickFlow.value = false
            }
        }
        disableAdCountDownTimer?.start()
    }

    override fun removeNativeAds(adPlaceName: IAdPlaceName) {
        adHolderBannerNativeMap[adPlaceName]?.reset()
        adHolderBannerNativeMap.remove(adPlaceName)
    }

    fun isDisableByTestAd(adName: String): Boolean {
        Log.d(TAG, "isDisableByTestAd: isTurnOnAdPlacesDisabledWhenDetectTestAd $isTurnOnAdPlacesDisabledWhenDetectTestAd isTestAd $isTestAd adPlacesDisabledWhenDetectTestAd $adPlacesDisabledWhenDetectTestAd ")
        return isTurnOnAdPlacesDisabledWhenDetectTestAd && isTestAd && adPlacesDisabledWhenDetectTestAd.contains(
            adName
        )
    }

    override fun isRewardReady(adPlaceName: IAdPlaceName): Boolean {
        val adPlace = remoteConfigRepository.getAdPlaceBy(adPlaceName)
        val adHolder = getOrCreateAdHolderFullScreenBy(adPlace, true)
        return (adHolder as? RewardedAdHolder)?.rewardedAd != null
    }
}
