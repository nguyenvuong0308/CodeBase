package com.core.config.data

import android.content.Context
import android.util.Log
import com.core.analytics.AnalyticsEvent
import com.core.analytics.AnalyticsManager
import com.core.config.data.helper.ConfigParam
import com.core.config.data.mapper.AdPlaceModelMapper
import com.core.config.data.mapper.AppConfigModelMapper
import com.core.config.data.mapper.AppOpenAdConfigModelMapper
import com.core.config.data.mapper.BannerAdConfigModelMapper
import com.core.config.data.mapper.IapConfigModelMapper
import com.core.config.data.mapper.InterstitialAdConfigModelMapper
import com.core.config.data.mapper.InterstitialRewardedAdConfigModelMapper
import com.core.config.data.mapper.NativeAdConfigModelMapper
import com.core.config.data.mapper.PreventAdClickConfigModelMapper
import com.core.config.data.mapper.RequestConsentConfigModelMapper
import com.core.config.data.mapper.RewardedAdConfigModelMapper
import com.core.config.data.mapper.SplashScreenConfigModelMapper
import com.core.config.domain.GetDataFromRemoteConfigUseCase
import com.core.config.domain.RemoteConfigRepository
import com.core.config.domain.data.AdPlace
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.AdType
import com.core.config.domain.data.AppConfig
import com.core.config.domain.data.AppConfig.Companion.DEFINE_INTRO_HAVE_ADS
import com.core.config.domain.data.AppConfig.Companion.DEFINE_INTRO_NO_ADS
import com.core.config.domain.data.AppOpenAdTypeConfig
import com.core.config.domain.data.BannerAdTypeConfig
import com.core.config.domain.data.IapConfig
import com.core.config.domain.data.InterstitialAdTypeConfig
import com.core.config.domain.data.NativeAdTypeConfig
import com.core.config.domain.data.NoneAdPlace
import com.core.config.domain.data.PreventAdClickConfig
import com.core.config.domain.data.RequestConsentConfig
import com.core.config.domain.data.RewardedAdTypeConfig
import com.core.config.domain.data.RewardedInterstitialAdTypeConfig
import com.core.config.domain.data.SplashScreenConfig
import com.core.preference.AppPreferences
import com.core.utilities.isAppDebuggable
import com.core.utilities.toast
import com.core.utilities.util.toast.Toasty
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RemoteConfigRepositoryImpl @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val analyticsManager: AnalyticsManager,
    private val appPreferences: AppPreferences,
    private val appConfigModelMapper: AppConfigModelMapper,
    private val iapConfigModelMapper: IapConfigModelMapper,
    private val preventAdClickConfigModelMapper: PreventAdClickConfigModelMapper,
    private val splashScreenConfigModelMapper: SplashScreenConfigModelMapper,
    private val adPlaceModelMapper: AdPlaceModelMapper,
    private val bannerAdConfigModelMapper: BannerAdConfigModelMapper,
    private val nativeAdConfigModelMapper: NativeAdConfigModelMapper,
    private val interstitialAdConfigModelMapper: InterstitialAdConfigModelMapper,
    private val interstitialRewardedAdConfigModelMapper: InterstitialRewardedAdConfigModelMapper,
    private val rewardedAdConfigModelMapper: RewardedAdConfigModelMapper,
    private val appOpenAdConfigModelMapper: AppOpenAdConfigModelMapper,
    private val requestConsentConfigModelMapper: RequestConsentConfigModelMapper,
    private val remoteConfigService: RemoteConfigService,
    private val getRemoteConfigUseCase: GetDataFromRemoteConfigUseCase,
) : RemoteConfigRepository {

    companion object {
        const val TAG = "RemoteConfigRepository"
        const val FETCH_REMOTE_CONFIG_TIMEOUT = 10 * 1000L
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _fetchStateCompleteFlow = MutableSharedFlow<FetchRemoteConfigState>()
    override val fetchStateCompleteFlow = _fetchStateCompleteFlow.asSharedFlow()

    private var isFetchComplete = false
    private var isFetching = false

    override fun fetchAndActive() {
        if (isFetching) {
            return
        }
        isFetching = true
        isFetchComplete = false
        applicationScope.launch {
            _fetchStateCompleteFlow.emit(FetchRemoteConfigState.Loading)
        }
        analyticsManager.logEvent(AnalyticsEvent.EVENT_REMOTE_CONFIG_FETCH)
        Log.e(TAG, "fetch loading")
        remoteConfigService.fetchAndActive { isSuccess ->
            isFetching = false
            if (applicationContext.isAppDebuggable() /*|| BuildConfig.FLAVOR == "dev"*/) {
                applicationContext.toast("fetch RemoteConfig Successfully!", Toasty.SUCCESS)
            }
            if (!isFetchComplete) {
                Log.e(TAG, "fetch complete $isSuccess")
                fetchRemoteConfigData(isNotifyComplete = true, isSuccess = true)
            } else {
                fetchRemoteConfigData(isNotifyComplete = false, isSuccess = true)
            }
            isFetchComplete = true
        }
        applicationScope.launch {
            delay(FETCH_REMOTE_CONFIG_TIMEOUT)
            if (!isFetchComplete) {
                analyticsManager.logEvent(AnalyticsEvent.EVENT_REMOTE_CONFIG_FETCH_TIMEOUT)
                if (appPreferences.isRemoteConfigFirstTimeFetch) {
                    appPreferences.isRemoteConfigFirstTimeFetch = false
                    analyticsManager.logEvent(AnalyticsEvent.EVENT_REMOTE_CONFIG_FETCH_TIMEOUT_FIRST)
                }
                if (applicationContext.isAppDebuggable() /*|| BuildConfig.FLAVOR == "dev"*/) {
                    applicationContext.toast("fetch RemoteConfig Timeout!", Toasty.WARNING)
                }
                Log.e(TAG, "fetch complete timeout")
                fetchRemoteConfigData(isNotifyComplete = true, isSuccess = false)
            }
            isFetching = false
            isFetchComplete = true
        }
    }

    private fun fetchRemoteConfigData(isNotifyComplete: Boolean, isSuccess: Boolean) {
        applicationScope.launch {
            val appConfigDeferred = async { getAppConfigRaw() }
            val preventAdClickConfigDeferred = async { getPreventAdClickConfigRaw() }
            val adsDisableByCountryDeferred = async { getAdsDisableByCountryRaw() }
            val adPlacesDisableWhenDetectTestAdDeferred = async { getAdPlacesDisableWhenDetectTestAdRaw() }
            val isTurnOnAdPlacesDisabledWhenDetectTestAdDeferred =
                async { isTurnOnAdPlacesDisabledWhenDetectTestAdRaw() }
            val splashScreenConfigDeferred = async { getSplashScreenConfigRaw() }
            val adPlacesDeferred = async { getAdPlacesRaw() }
            val bannerAdConfigDeferred = async { getBannerAdConfigRaw() }
            val nativeAdConfigDeferred = async { getNativeAdConfigRaw() }
            val interstitialAdConfigDeferred = async { getInterstitialAdConfigRaw() }
            val rewardedInterstitialAdConfigDeferred =
                async { getRewardedInterstitialAdConfigRaw() }
            val rewardedAdConfigDeferred = async { getRewardedAdConfigRaw() }
            val appOpenAdConfigDeferred = async { getAppOpenAdConfigRaw() }
            val requestConsentConfigDeferred = async { getRequestConsentConfigRaw() }
            val getOtherConfig = async {
                getRemoteConfigUseCase.invoke(remoteConfigService)
            }

            appConfigCache = appConfigDeferred.await()
            preventAdClickConfigCache = preventAdClickConfigDeferred.await()
            adsDisableByCountryCache = adsDisableByCountryDeferred.await()
            adPlacesDisableWhenDetectTestAdCache = adPlacesDisableWhenDetectTestAdDeferred.await()
            isTurnOnAdPlacesDisabledWhenDetectTestAdCache =
                isTurnOnAdPlacesDisabledWhenDetectTestAdDeferred.await()
            splashScreenConfigCache = splashScreenConfigDeferred.await()
            adPlacesCache = adPlacesDeferred.await()
            bannerAdConfigCache = bannerAdConfigDeferred.await()
            nativeAdConfigCache = nativeAdConfigDeferred.await()
            interstitialAdConfigCache = interstitialAdConfigDeferred.await()
            rewardedInterstitialAdConfigCache = rewardedInterstitialAdConfigDeferred.await()
            rewardedAdConfigCache = rewardedAdConfigDeferred.await()
            appOpenAdConfigCache = appOpenAdConfigDeferred.await()
            requestConsentConfigCache = requestConsentConfigDeferred.await()
            getOtherConfig.await()

            if (isNotifyComplete) {
                _fetchStateCompleteFlow.emit(FetchRemoteConfigState.Complete(isSuccess))
            }
        }
    }

    private var appConfigCache: AppConfig? = null
    private var iapConfigCache: IapConfig? = null
    private var preventAdClickConfigCache: PreventAdClickConfig? = null
    private var adsDisableByCountryCache: List<String>? = null
    private var adPlacesDisableWhenDetectTestAdCache: List<String>? = null
    private var isTurnOnAdPlacesDisabledWhenDetectTestAdCache: Boolean? = null
    private var splashScreenConfigCache: SplashScreenConfig? = null
    private var adPlacesCache: List<AdPlace>? = null
    private var nativeAdConfigCache: NativeAdTypeConfig? = null
    private var bannerAdConfigCache: BannerAdTypeConfig? = null
    private var interstitialAdConfigCache: InterstitialAdTypeConfig? = null
    private var rewardedInterstitialAdConfigCache: RewardedInterstitialAdTypeConfig? = null
    private var rewardedAdConfigCache: RewardedAdTypeConfig? = null
    private var appOpenAdConfigCache: AppOpenAdTypeConfig? = null
    private var requestConsentConfigCache: RequestConsentConfig? = null

    private fun getAppConfigRaw(): AppConfig {
        val model = remoteConfigService.getAppConfig()
        return if (model == null) {
            AppConfig(
                isHideNavigationBar = false,
                isEnableAds = true,
                isAlwaysShowIntroAndLanguageScreen = false,
                isEnableIntroductionScreen = true,
                isEnableChangeLanguageScreen = true,
                isEnableAppShortCut = false,
                isEnableAppShortcutUninstall = false,
                isEnableOpenAppAdsFromUninstallShortcut = false,
                isEnableOpenAppAdsFromShortcut = false,
                introActionShowType = 1,
                isHideNativeBannerWhenNetworkError = false,
                introData = arrayListOf(DEFINE_INTRO_HAVE_ADS,DEFINE_INTRO_HAVE_ADS,DEFINE_INTRO_HAVE_ADS),
                isAlwaysPreloadBannerNativeAdsWhenStart = true,
                isPreloadBannerNativeExit = false,
                intervalDayAlwaysShowIntroAndLanguage = 3,
                isAlwaysShowIntroAndLanguageScreenWithInterval = false,
                introDataV2 = arrayListOf(DEFINE_INTRO_NO_ADS,DEFINE_INTRO_NO_ADS,DEFINE_INTRO_HAVE_ADS)
            )
        } else {
            appConfigModelMapper.toData(model)
        }
    }

    private fun getIapConfigRaw(): IapConfig {
        val model = remoteConfigService.getIapConfig()
        return if (model == null) {
            IapConfig(
                isShowIAPOnStart = false,
                isShowIAPFirstOpen = true,
                isShowIAPBeforeRequestPermission = true,
                isEnableIapV2 = true,
                timeWaitToShowCloseIcon = 1500,
                upgradePremiumDisableByCountry = listOf()
            )
        } else {
            iapConfigModelMapper.toData(model)
        }
    }

    private fun getPreventAdClickConfigRaw(): PreventAdClickConfig {
        val model = remoteConfigService.getPreventAdClickConfig()
        return if (model == null) {
            PreventAdClickConfig(
                maxAdClickPerSession = ConfigParam.PREVENT_AD_CLICK_CONFIG_DEFAULT_MAX_AD_CLICK_PER_SESSION,
                timePerSession = ConfigParam.PREVENT_AD_CLICK_CONFIG_DEFAULT_TIME_PER_SESSION,
                timeDisableAdsWhenReachedMaxAdClick = ConfigParam.PREVENT_AD_CLICK_CONFIG_DEFAULT_TIME_DISABLE
            )
        } else {
            preventAdClickConfigModelMapper.toData(model)
        }
    }

    private fun getAdsDisableByCountryRaw(): List<String> {
        return remoteConfigService.getAdsDisableByCountry()
    }

    private fun getAdPlacesDisableWhenDetectTestAdRaw(): List<String> {
        return remoteConfigService.getAdPlacesDisableWhenDetectTestAd()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    private fun isTurnOnAdPlacesDisabledWhenDetectTestAdRaw(): Boolean {
        return remoteConfigService.isTurnOnAdPlacesDisabledWhenDetectTestAd()
    }

    private fun getSplashScreenConfigRaw(): SplashScreenConfig {
        val model = remoteConfigService.getSplashScreenConfig()
        return if (model == null) {
            SplashScreenConfig(
                maxTimeToWaitAppOpenAd = ConfigParam.SPLASH_SCREEN_CONFIG_DEFAULT_MAX_TIME_TO_WAIT_APP_OPEN_AD,
                timeSkipAppOpenAdWhenNotAvailable = ConfigParam.SPLASH_SCREEN_CONFIG_DEFAULT_TIME_SKIP_APP_OPEN_AD_WHEN_NOT_AVAILABLE,
                adTypeFirstOpen = AdType.AppOpen,
                adType = AdType.AppOpen,
                minTimeWaitProgressBeforeShowAd = ConfigParam.SPLASH_SCREEN_CONFIG_DEFAULT_MIN_TIME_WAIT_PROGRESS_BEFORE_SHOW_AD,
                isEnableRetry = ConfigParam.SPLASH_SCREEN_CONFIG_DEFAULT_ENABLE_RETRY,
                maxRetryCount = ConfigParam.SPLASH_SCREEN_CONFIG_DEFAULT_MAX_RETRY_COUNT,
                retryFixedDelay = ConfigParam.SPLASH_SCREEN_CONFIG_DEFAULT_RETRY_FIXED_DELAY,
                isLoadBeforeEuConsent = ConfigParam.SPLASH_SCREEN_CONFIG_DEFAULT_IS_LOAD_BEFORE_CONSENT,
            )
        } else {
            splashScreenConfigModelMapper.toData(model)
        }
    }

    private fun getAdPlacesRaw(): List<AdPlace> {
        val models = buildList {
            addAll(remoteConfigService.getBannerNativeAdPlaces())
            addAll(remoteConfigService.getAppOpenAdPlaces())
            addAll(remoteConfigService.getRewardedRewardedInterInterAdPlaces())
        }
        return models.map {
            adPlaceModelMapper.toData(it)
        }
    }

    private fun getNativeAdConfigRaw(): NativeAdTypeConfig {
        val model = remoteConfigService.getNativeAdConfig()
        return if (model == null) {
            NativeAdTypeConfig(
                isEnableRetry = ConfigParam.RETRY_IS_ENABLE_RETRY,
                maxRetryCount = ConfigParam.RETRY_MAX_RETRY_COUNT,
                retryIntervalSecondList = ConfigParam.RETRY_INTERVAL_LIST,
                expiredTimeSecond = ConfigParam.EXPIRED_NATIVE_TIME_DEFAULT
            )
        } else {
            nativeAdConfigModelMapper.toData(model)
        }
    }

    private fun getBannerAdConfigRaw(): BannerAdTypeConfig {
        val model = remoteConfigService.getBannerAdConfig()
        return if (model == null) {
            BannerAdTypeConfig(
                isEnableRetry = ConfigParam.RETRY_IS_ENABLE_RETRY,
                maxRetryCount = ConfigParam.RETRY_MAX_RETRY_COUNT,
                retryIntervalSecondList = ConfigParam.RETRY_INTERVAL_LIST,
            )
        } else {
            bannerAdConfigModelMapper.toData(model)
        }
    }

    private fun getInterstitialAdConfigRaw(): InterstitialAdTypeConfig {
        val model = remoteConfigService.getInterstitialAdConfig()
        return if (model == null) {
            InterstitialAdTypeConfig(
                isWaitLoadToShow = false,
                adsPerSession = ConfigParam.INTERSTITIAL_AD_CONFIG_DEFAULT_ADS_PER_SESSION,
                timePerSession = ConfigParam.INTERSTITIAL_AD_CONFIG_DEFAULT_TIME_PER_SESSION,
                timeInterval = ConfigParam.INTERSTITIAL_AD_CONFIG_DEFAULT_TIME_INTERVAL,
                timeIntervalAfterShowOpenAd = ConfigParam.REOPEN_TO_INTERSTITIAL_AD_CONFIG_DEFAULT_TIME_INTERVAL,
                isEnableRetry = ConfigParam.RETRY_IS_ENABLE_RETRY,
                maxRetryCount = ConfigParam.RETRY_MAX_RETRY_COUNT,
                retryIntervalSecondList = ConfigParam.RETRY_INTERVAL_LIST,
            )
        } else {
            interstitialAdConfigModelMapper.toData(model)
        }
    }

    private fun getRewardedInterstitialAdConfigRaw(): RewardedInterstitialAdTypeConfig {
        val model = remoteConfigService.getRewardedInterstitialAdConfig()
        return if (model == null) {
            RewardedInterstitialAdTypeConfig(
                isWaitLoadToShow = false,
                isEnableRetry = ConfigParam.RETRY_IS_ENABLE_RETRY,
                maxRetryCount = ConfigParam.RETRY_MAX_RETRY_COUNT,
                retryIntervalSecondList = ConfigParam.RETRY_INTERVAL_LIST,
            )
        } else {
            interstitialRewardedAdConfigModelMapper.toData(model)
        }
    }

    private fun getRewardedAdConfigRaw(): RewardedAdTypeConfig {
        val model = remoteConfigService.getRewardedAdConfig()
        return if (model == null) {
            RewardedAdTypeConfig(
                isWaitLoadToShow = false,
                isEnableRetry = ConfigParam.RETRY_IS_ENABLE_RETRY,
                maxRetryCount = ConfigParam.RETRY_MAX_RETRY_COUNT,
                retryIntervalSecondList = ConfigParam.RETRY_INTERVAL_LIST,
                timeWaitRetryOnContext = ConfigParam.TIME_WAIT_RETRY_ON_CONTEXT,
                maxRetryOnContext = ConfigParam.MAX_RETRY_ON_CONTEXT
            )
        } else {
            rewardedAdConfigModelMapper.toData(model)
        }
    }

    private fun getAppOpenAdConfigRaw(): AppOpenAdTypeConfig {
        val model = remoteConfigService.getAppOpenAdConfig()
        return if (model == null) {
            AppOpenAdTypeConfig(
                timeMillisDelayBeforeShow = ConfigParam.DEFAULT_OPEN_APP_AD_TIME_MILLIS_DELAY_BEFORE_SHOW,
                timeInterval = ConfigParam.DEFAULT_OPEN_APP_AD_TIME_INTERVAL,
                isEnableRetry = ConfigParam.RETRY_IS_ENABLE_RETRY,
                maxRetryCount = ConfigParam.RETRY_MAX_RETRY_COUNT,
                retryIntervalSecondList = ConfigParam.RETRY_INTERVAL_LIST,
            )
        } else {
            appOpenAdConfigModelMapper.toData(model)
        }
    }

    private fun getRequestConsentConfigRaw(): RequestConsentConfig {
        val model = remoteConfigService.getRequestConsentConfig()
        return if (model == null) {
            RequestConsentConfig(
                isEnable = false,
                debugIsEEA = false,
                debugListTestDeviceHashedId = listOf(),
            )
        } else {
            requestConsentConfigModelMapper.toData(model)
        }
    }

    override fun getAppConfig(): AppConfig {
        return appConfigCache ?: getAppConfigRaw()
    }

    override fun getIapConfig(): IapConfig {
        return iapConfigCache ?: getIapConfigRaw()
    }

    override fun getAdPlaceBy(adPlaceName: IAdPlaceName): AdPlace {
        return getAdPlaces().find { it.placeName == adPlaceName } ?: NoneAdPlace()
    }

    override fun getPreventAdClickConfig(): PreventAdClickConfig {
        return preventAdClickConfigCache ?: getPreventAdClickConfigRaw()
    }

    override fun getAdsDisableByCountry(): List<String> {
        return adsDisableByCountryCache ?: getAdsDisableByCountryRaw()
    }

    override fun getAdPlacesDisableWhenDetectTestAd(): List<String> {
        return adPlacesDisableWhenDetectTestAdCache ?: getAdPlacesDisableWhenDetectTestAdRaw()
    }

    override fun isTurnOnAdPlacesDisabledWhenDetectTestAd(): Boolean {
        return isTurnOnAdPlacesDisabledWhenDetectTestAdCache
            ?: isTurnOnAdPlacesDisabledWhenDetectTestAdRaw()
    }

    override fun getSplashScreenConfig(): SplashScreenConfig {
        return splashScreenConfigCache ?: getSplashScreenConfigRaw()
    }

    override fun getAdPlaces(): List<AdPlace> {
        return adPlacesCache ?: getAdPlacesRaw()
    }

    override fun getBannerAdConfig(): BannerAdTypeConfig {
        return bannerAdConfigCache ?: getBannerAdConfigRaw()
    }

    override fun getNativeAdConfig(): NativeAdTypeConfig {
        return nativeAdConfigCache ?: getNativeAdConfigRaw()
    }

    override fun getInterstitialAdConfig(): InterstitialAdTypeConfig {
        return interstitialAdConfigCache ?: getInterstitialAdConfigRaw()
    }

    override fun getRewardedInterstitialAdConfig(): RewardedInterstitialAdTypeConfig {
        return rewardedInterstitialAdConfigCache ?: getRewardedInterstitialAdConfigRaw()
    }

    override fun getRewardedAdConfig(): RewardedAdTypeConfig {
        return rewardedAdConfigCache ?: getRewardedAdConfigRaw()
    }

    override fun getAppOpenAdConfig(): AppOpenAdTypeConfig {
        return appOpenAdConfigCache ?: getAppOpenAdConfigRaw()
    }

    override fun getRequestConsentConfig(): RequestConsentConfig {
        return requestConsentConfigCache ?: getRequestConsentConfigRaw()
    }
}
