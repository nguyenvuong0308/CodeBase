package com.core.config.data

import com.core.config.domain.data.AdPlace
import com.core.config.domain.data.AppOpenAdPlace
import com.core.config.domain.data.BannerAdPlace
import com.core.config.domain.data.CoreAdPlaceName
import com.core.config.domain.data.InterstitialAdPlace
import com.core.config.domain.data.NativeAdPlace
import com.core.config.domain.data.NoneAdPlace
import com.core.config.domain.data.RewardedInterstitialAdPlace
import com.core.config.domain.data.RewardedVideoAdPlace
import com.core.config.domain.data.SplashScreenConfig

/**
 * Áp dụng isEnableFirstOpen / isEnableOpen của splash_screen_config lên các ad place splash.
 * Flag có giá trị sẽ ghi đè is_enable của ad place; flag null giữ nguyên config ad place.
 */
internal object SplashAdPlaceEnableOverrideResolver {

    private val FIRST_OPEN_PLACE_NAMES = setOf(
        CoreAdPlaceName.ACTION_OPEN_APP_FIRST_OPEN.name,
        CoreAdPlaceName.APP_OPEN_FIRST_OPEN.name,
    )

    private val OPEN_PLACE_NAMES = setOf(
        CoreAdPlaceName.ACTION_OPEN_APP.name,
        CoreAdPlaceName.APP_OPEN.name,
    )

    fun resolve(adPlaces: List<AdPlace>, splashScreenConfig: SplashScreenConfig): List<AdPlace> {
        val isEnableFirstOpen = splashScreenConfig.isEnableFirstOpen
        val isEnableOpen = splashScreenConfig.isEnableOpen
        if (isEnableFirstOpen == null && isEnableOpen == null) return adPlaces

        return adPlaces.map { adPlace ->
            val override = when (adPlace.placeName.name) {
                in FIRST_OPEN_PLACE_NAMES -> isEnableFirstOpen
                in OPEN_PLACE_NAMES -> isEnableOpen
                else -> null
            }
            if (override == null || override == adPlace.isEnable) adPlace else adPlace.withEnable(override)
        }
    }

    private fun AdPlace.withEnable(isEnable: Boolean): AdPlace = when (this) {
        is RewardedVideoAdPlace -> copy(isEnable = isEnable)
        is RewardedInterstitialAdPlace -> copy(isEnable = isEnable)
        is InterstitialAdPlace -> copy(isEnable = isEnable)
        is NativeAdPlace -> copy(isEnable = isEnable)
        is BannerAdPlace -> copy(isEnable = isEnable)
        is AppOpenAdPlace -> copy(isEnable = isEnable)
        is NoneAdPlace -> copy(isEnable = isEnable)
    }
}
