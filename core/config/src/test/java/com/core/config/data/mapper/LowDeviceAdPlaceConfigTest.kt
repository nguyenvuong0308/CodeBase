package com.core.config.data.mapper

import android.content.Context
import com.core.config.data.AdPlaceAbTestOverrideResolver
import com.core.config.data.model.AdPlaceModel
import com.core.config.domain.data.*
import com.squareup.moshi.Moshi
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock

class LowDeviceAdPlaceConfigTest {
    private val adapter = Moshi.Builder().build().adapter(AdPlaceModel::class.java)
    private val mapper = AdPlaceModelMapper(
        mock(IAppProviderAdPlaceName::class.java), mock(Context::class.java),
    )
    private val types = listOf("banner", "native", "native_interstitial", "interstitial",
        "rewarded", "rewarded_interstitial", "app_open")

    private fun model(type: String, flag: String = "", name: String = "action_app_open") =
        adapter.fromJson("""{
            "place_name":"$name", "ad_type":"$type", "ad_id":"shared-unit", "is_enable":true $flag
        }""")!!

    @Test
    fun `all placement types default to false when field is missing or null`() {
        for (type in types) {
            assertFalse(mapper.toData(model(type)).isDisableForLowDevice)
            assertFalse(mapper.toData(model(type, ""","is_disable_for_low_device":null""")).isDisableForLowDevice)
        }
    }

    @Test
    fun `all placement types preserve explicit true and false`() {
        for (type in types) for (flag in listOf(true, false)) {
            val place = mapper.toData(model(type, ""","is_disable_for_low_device":$flag"""))
            assertEquals("$type flag=$flag", flag, place.isDisableForLowDevice)
        }
    }

    @Test
    fun `same type and unit can have different flags per placement`() {
        for (type in types) {
            val blocked = mapper.toData(model(type, ""","is_disable_for_low_device":true"""))
            val allowed = mapper.toData(model(type, name = "action_app_open_first_open"))
            assertTrue(blocked.isDisableForLowDevice)
            assertFalse(allowed.isDisableForLowDevice)
            assertEquals(blocked.adId, allowed.adId)
            assertEquals(blocked.adType, allowed.adType)
            assertNotEquals(blocked.placeName, allowed.placeName)
        }
    }

    @Test
    fun `nested native config owns its flag independently of parent interstitial`() {
        for (parentFlag in listOf(true, false)) for (nativeFlag in listOf(true, false)) {
            val model = adapter.fromJson("""{
                "place_name":"action_app_open", "ad_type":"interstitial",
                "is_disable_for_low_device":$parentFlag,
                "is_show_native_after":true,
                "native_config":{
                    "place_name":"remote_native_after", "ad_type":"native",
                    "is_disable_for_low_device":$nativeFlag
                }
            }""")!!
            val parent = mapper.toData(model) as InterstitialAdPlace
            assertEquals(parentFlag, parent.isDisableForLowDevice)
            assertEquals(nativeFlag, parent.nativeAfterInterstitial!!.isDisableForLowDevice)
        }
    }

    @Test
    fun `AB override changes only the selected placement flag`() {
        val base = model("native", ""","is_disable_for_low_device":false""")
        val other = model("native", name = "action_app_open_first_open")
        val override = model("native", ""","is_disable_for_low_device":true""")
        val resolved = AdPlaceAbTestOverrideResolver.resolve(
            listOf(base, other), listOf("action_app_open"), { override },
        ).map(mapper::toData)
        assertTrue(resolved[0].isDisableForLowDevice)
        assertFalse(resolved[1].isDisableForLowDevice)
    }

    @Test
    fun `placement copies preserve the flag`() {
        val blocked = mapper.toData(model("native", ""","is_disable_for_low_device":true""")) as NativeAdPlace
        assertTrue(blocked.copy(isEnable = false).isDisableForLowDevice)
    }
}
