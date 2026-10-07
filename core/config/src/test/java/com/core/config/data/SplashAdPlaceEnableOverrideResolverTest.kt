package com.core.config.data

import com.core.config.data.mapper.SplashScreenConfigModelMapper
import com.core.config.data.model.SplashScreenConfigModel
import com.core.config.domain.data.AdPlace
import com.core.config.domain.data.AdType
import com.core.config.domain.data.AppOpenAdPlace
import com.core.config.domain.data.CoreAdPlaceName
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.RemoteAdPlaceName
import com.core.config.domain.data.SplashScreenConfig
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SplashAdPlaceEnableOverrideResolverTest {

    private val places = listOf(
        place(CoreAdPlaceName.ACTION_OPEN_APP_FIRST_OPEN, isEnable = true),
        place(CoreAdPlaceName.APP_OPEN_FIRST_OPEN, isEnable = false),
        place(CoreAdPlaceName.ACTION_OPEN_APP, isEnable = false),
        place(CoreAdPlaceName.APP_OPEN, isEnable = true),
        place(RemoteAdPlaceName("home_native"), isEnable = true),
    )

    @Test
    fun `null flags keep ad place config`() {
        val result = SplashAdPlaceEnableOverrideResolver.resolve(places, splashConfig(null, null))

        assertSame(places, result)
    }

    @Test
    fun `first open flag overrides only first open places`() {
        val disabled = SplashAdPlaceEnableOverrideResolver.resolve(places, splashConfig(false, null))
        assertEquals(listOf(false, false, false, true, true), disabled.map { it.isEnable })

        val enabled = SplashAdPlaceEnableOverrideResolver.resolve(places, splashConfig(true, null))
        assertEquals(listOf(true, true, false, true, true), enabled.map { it.isEnable })
    }

    @Test
    fun `open flag overrides only open places`() {
        val disabled = SplashAdPlaceEnableOverrideResolver.resolve(places, splashConfig(null, false))
        assertEquals(listOf(true, false, false, false, true), disabled.map { it.isEnable })

        val enabled = SplashAdPlaceEnableOverrideResolver.resolve(places, splashConfig(null, true))
        assertEquals(listOf(true, false, true, true, true), enabled.map { it.isEnable })
    }

    @Test
    fun `override keeps other ad place fields`() {
        val result = SplashAdPlaceEnableOverrideResolver.resolve(places, splashConfig(false, false))

        assertEquals(places.map { (it as AppOpenAdPlace).copy(isEnable = false) }.take(4), result.take(4))
        assertSame(places[4], result[4])
    }

    @Test
    fun `mapper parses splash flags and keeps null when missing`() {
        val adapter = Moshi.Builder().build().adapter(SplashScreenConfigModel::class.java)
        val mapper = SplashScreenConfigModelMapper()

        val withFlags = mapper.toData(
            requireNotNull(adapter.fromJson("""{"is_enable_first_open":false,"is_enable_open":true}"""))
        )
        assertEquals(false, withFlags.isEnableFirstOpen)
        assertEquals(true, withFlags.isEnableOpen)

        val withoutFlags = mapper.toData(requireNotNull(adapter.fromJson("{}")))
        assertNull(withoutFlags.isEnableFirstOpen)
        assertNull(withoutFlags.isEnableOpen)
    }

    private fun place(name: IAdPlaceName, isEnable: Boolean): AdPlace = AppOpenAdPlace(
        isTrackingShow = true,
        isTrackingClick = true,
        limitShow = 3,
        placeName = name,
        adId = "id-${name.name}",
        highFloorAdIds = emptyList(),
        isEnable = isEnable,
        adType = AdType.AppOpen,
        isAutoLoadAfterDismiss = false,
        isIgnoreInterval = false,
        isTutorialFlow = false,
    )

    private fun splashConfig(isEnableFirstOpen: Boolean?, isEnableOpen: Boolean?) = SplashScreenConfig(
        maxTimeToWaitAppOpenAd = 30L,
        timeSkipAppOpenAdWhenNotAvailable = 5L,
        adTypeFirstOpen = AdType.AppOpen,
        adType = AdType.AppOpen,
        minTimeWaitProgressBeforeShowAd = 5L,
        isEnableRetry = true,
        maxRetryCount = 10,
        retryFixedDelay = 1000L,
        isLoadBeforeEuConsent = true,
        isEnableFirstOpen = isEnableFirstOpen,
        isEnableOpen = isEnableOpen,
    )
}
