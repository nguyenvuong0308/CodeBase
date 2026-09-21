package com.core.config.data.mapper

import android.content.Context
import com.core.config.data.model.AdPlaceModel
import com.core.config.domain.data.*
import com.squareup.moshi.Moshi
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock

class NativeInterstitialAdPlaceMapperTest {
    private val adapter = Moshi.Builder().build().adapter(AdPlaceModel::class.java)
    private val mapper = AdPlaceModelMapper(
        mock(IAppProviderAdPlaceName::class.java), mock(Context::class.java),
    )

    @Test
    fun `native interstitial maps to native styling with a distinct fullscreen type`() {
        val place = mapper.toData(adapter.fromJson("""{
            "place_name":"action_app_open", "ad_type":"native_interstitial",
            "ad_id":"native-unit", "is_enable":true,
            "native_template_size":"full_interstitial_v3",
            "count_down_timer":5, "close_step_count":2,
            "step_1_count_down_timer":3, "step_2_count_down_timer":2
        }""")!!) as NativeAdPlace

        assertEquals(AdType.NativeInterstitial, place.adType)
        assertTrue(place.isNativeInterstitialType())
        assertFalse(place.isNativeType())
        assertFalse(place.isInterstitialType())
        assertEquals("native-unit", place.adId)
        assertEquals(NativeTemplateSize.FullInterstitialV3, place.nativeTemplateSize)
        assertEquals(5, place.countDownTimer)
        assertEquals(2, place.closeStepCount)
        assertEquals(3, place.step1CountDownTimer)
        assertEquals(2, place.step2CountDownTimer)
        assertTrue(place.isAutoLoadAfterDismiss)
    }

    @Test
    fun `existing native placements retain their original type`() {
        val place = mapper.toData(adapter.fromJson("""{
            "place_name":"action_app_open", "ad_type":"native", "ad_id":"native-unit"
        }""")!!) as NativeAdPlace

        assertTrue(place.isNativeType())
        assertFalse(place.isNativeInterstitialType())
    }

    @Test
    fun `native after interstitial still accepts remote-only native placement`() {
        val place = mapper.toData(adapter.fromJson("""{
            "place_name":"action_app_open", "ad_type":"interstitial",
            "is_show_native_after":true,
            "native_config":{"place_name":"remote_native_after", "ad_type":"native"}
        }""")!!) as InterstitialAdPlace

        assertEquals(RemoteAdPlaceName("remote_native_after"), place.nativeAfterInterstitial?.placeName)
        assertEquals(AdType.Native, place.nativeAfterInterstitial?.adType)
    }

    @Test
    fun `unsupported type remains disabled`() {
        assertEquals(AdType.None, AdType.getAdTypeBy("unsupported"))
        assertTrue(mapper.toData(adapter.fromJson("""{"ad_type":"unsupported"}""")!!) is NoneAdPlace)
    }
}
