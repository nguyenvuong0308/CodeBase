package com.core.config.data

import android.content.Context
import android.os.Build
import com.squareup.moshi.Moshi
import com.core.config.R
import com.core.config.data.model.AppOpenAdConfigModel
import com.core.config.data.model.AdPlacesVersionKeyModel
import com.core.config.data.model.InterstitialAdConfigModel
import com.core.config.data.model.NativeAdConfigModel
import com.core.config.data.model.RewardedAdConfigModel
import com.core.config.data.model.RewardedInterstitialAdConfigModel
import com.core.config.data.model.PreventAdClickConfigModel
import com.core.config.data.model.SplashScreenConfigModel
import com.core.config.data.helper.ConfigParam
import com.core.config.data.helper.read
import com.core.config.data.helper.readList
import com.core.config.data.model.AdPlaceModel
import com.core.config.data.model.AppConfigModel
import com.core.config.data.model.BannerAdConfigModel
import com.core.config.data.model.IapConfigModel
import com.core.config.data.model.RequestConsentConfigModel
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.core.utilities.isAppDebuggable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigService @Inject constructor(
    private val moshi: Moshi,
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CONFIG_CACHE_EXPIRATION_SECONDS = 1L
    }
//
//    private val config = FirebaseRemoteConfig.getInstance().apply {
//        val configSettings = FirebaseRemoteConfigSettings.Builder()
//            .setMinimumFetchIntervalInSeconds(CONFIG_CACHE_EXPIRATION_SECONDS)
//            .build()
//        setConfigSettingsAsync(configSettings)
//        fetch(CONFIG_CACHE_EXPIRATION_SECONDS)
//            .addOnCompleteListener {
//                if (it.isSuccessful) {
//                    fetchAndActivate()
//                }
//            }
//    }

    private val remoteConfig by lazy {
        val settings = remoteConfigSettings {
            fetchTimeoutInSeconds = 30
            minimumFetchIntervalInSeconds = if (context.isAppDebuggable()) {
                0
            } else {
                CONFIG_CACHE_EXPIRATION_SECONDS
            }
        }
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(settings)
            setDefaultsAsync(R.xml.remote_config_defaults)
        }
    }

    // Extension reified để gọi gọn
    inline fun <reified T> fetchOtherConfig(key: String): T? {
        return fetchOtherConfig(key, T::class.java)
    }

    fun <T> fetchOtherConfig(key: String, clazz: Class<T>): T? {
        return try {
            when (clazz) {
                String::class.java -> clazz.cast(remoteConfig.getString(key))
                Boolean::class.java -> clazz.cast(remoteConfig.getBoolean(key))
                Long::class.java -> clazz.cast(remoteConfig.getLong(key))
                Double::class.java -> clazz.cast(remoteConfig.getDouble(key))
                else -> {
                    val json = remoteConfig.getString(key)
                    if (json.isNotEmpty()) {
                        moshi.adapter(clazz).fromJson(json)
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            // Log error or handle appropriately
            null
        }
    }

    fun fetchAndActive(onComplete: (isSuccess: Boolean) -> Unit) {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener {
                onComplete.invoke(it.isSuccessful)
            }
    }

    internal fun getAppConfig(): AppConfigModel? {
        return remoteConfig.read(
            moshi,
            ConfigParam.AppConfig
        )
    }

    internal fun getIapConfig(): IapConfigModel? {
        return remoteConfig.read(
            moshi,
            ConfigParam.IapConfig
        )
    }

    internal fun getPreventAdClickConfig(): PreventAdClickConfigModel? {
        return remoteConfig.read(
            moshi,
            ConfigParam.PreventAdClickConfigParam
        )
    }

    internal fun getAdsDisableByCountry(): List<String> {
        return remoteConfig.readList(
            moshi,
            ConfigParam.AdsDisabledByCountryParam
        )
    }

    internal fun getAdPlacesDisableWhenDetectTestAd(): List<String> {
        return remoteConfig.readList(
            moshi,
            ConfigParam.AdPlacesDisableWhenDetectTestAdParam
        )
    }

    internal fun isTurnOnAdPlacesDisabledWhenDetectTestAd(): Boolean {
        return remoteConfig.getBoolean(
            ConfigParam.IsTurnOnAdPlacesDisabledWhenDetectTestAdParam.key
        )
    }

    internal fun getSplashScreenConfig(): SplashScreenConfigModel? {
        return remoteConfig.read(
            moshi, ConfigParam.SplashScreenConfigParam
        )
    }

    internal fun getBannerNativeAdPlaces(): List<AdPlaceModel> {
        return getAdPlacesByConfigKeys(
            versionedKeySelector = { it.bannerNative },
            fallbackOverrideKey = ConfigParam.BannerNativeAdPlaces2.key,
            defaultKey = ConfigParam.BannerNativeAdPlaces.key,
        )
    }

    internal fun getAppOpenAdPlaces(): List<AdPlaceModel> {
        return getAdPlacesByConfigKeys(
            versionedKeySelector = { it.appOpen },
            fallbackOverrideKey = ConfigParam.AppOpenAdPlaces2.key,
            defaultKey = ConfigParam.AppOpenAdPlaces.key,
        )
    }

    internal fun getRewardedRewardedInterInterAdPlaces(): List<AdPlaceModel> {
        return getAdPlacesByConfigKeys(
            versionedKeySelector = { it.rewardInter },
            fallbackOverrideKey = ConfigParam.RewardedRewardedInterInterAdPlaces2.key,
            defaultKey = ConfigParam.RewardedRewardedInterInterAdPlaces.key,
        )
    }

    private fun getAdPlacesByConfigKeys(
        versionedKeySelector: (AdPlacesVersionKeyModel) -> String?,
        fallbackOverrideKey: String,
        defaultKey: String,
    ): List<AdPlaceModel> {
        val versionedKey = getVersionedAdPlacesKey(versionedKeySelector)
        if (!versionedKey.isNullOrBlank()) {
            val versionedList = readAdPlaceList(versionedKey)
            if (versionedList.isNotEmpty()) {
                return versionedList
            }
        }

        val fallbackOverrideList = readAdPlaceList(fallbackOverrideKey)
        if (fallbackOverrideList.isNotEmpty()) {
            return fallbackOverrideList
        }

        return readAdPlaceList(defaultKey)
    }

    private fun getVersionedAdPlacesKey(
        versionedKeySelector: (AdPlacesVersionKeyModel) -> String?,
    ): String? {
        val currentVersionCode = getCurrentVersionCode()
        return remoteConfig.readList(
            moshi,
            ConfigParam.AdPlacesVersionConfigParam
        ).firstOrNull { config ->
            config.versionCodes?.contains(currentVersionCode) == true
        }?.key?.let(versionedKeySelector)
    }

    private fun readAdPlaceList(key: String): List<AdPlaceModel> {
        return try {
            val json = remoteConfig.getString(key)
            if (json.isBlank()) {
                emptyList()
            } else {
                val listType = com.squareup.moshi.Types.newParameterizedType(
                    List::class.java,
                    AdPlaceModel::class.java
                )
                moshi.adapter<List<AdPlaceModel>>(listType).fromJson(json).orEmpty()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun getCurrentVersionCode(): Long {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }

    internal fun getBannerAdConfig(): BannerAdConfigModel? {
        return remoteConfig.read(
            moshi, ConfigParam.BannerAdsParam
        )
    }

    internal fun getNativeAdConfig(): NativeAdConfigModel? {
        return remoteConfig.read(
            moshi, ConfigParam.NativeAdsParam
        )
    }

    internal fun getInterstitialAdConfig(): InterstitialAdConfigModel? {
        return remoteConfig.read(
            moshi, ConfigParam.InterstitialAdsParam
        )
    }

    internal fun getRewardedInterstitialAdConfig(): RewardedInterstitialAdConfigModel? {
        return remoteConfig.read(
            moshi, ConfigParam.InterstitialRewardedAdsParam
        )
    }

    internal fun getRewardedAdConfig(): RewardedAdConfigModel? {
        return remoteConfig.read(
            moshi, ConfigParam.RewardedAdsParam
        )
    }

    internal fun getAppOpenAdConfig(): AppOpenAdConfigModel? {
        return remoteConfig.read(
            moshi, ConfigParam.AppOpenAdsParam
        )
    }

    internal fun getRequestConsentConfig(): RequestConsentConfigModel? {
        return remoteConfig.read(
            moshi, ConfigParam.RequestConsentConfigParam
        )
    }

}
