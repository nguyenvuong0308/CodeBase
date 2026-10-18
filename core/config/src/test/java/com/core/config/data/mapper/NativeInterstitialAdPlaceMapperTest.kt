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

    @Test fun `Moshi accepts null palette entries and mapper preserves nullable contract`() {
        for (type in listOf("native", "native_interstitial")) {
            val model = adapter.fromJson("""{
                "place_name":"action_app_open", "ad_type":"$type",
                "native_template_size":"full_interstitial_v4", "native_theme":"shuffle",
                "native_theme_colors":{"light":null,"dark":{"primary_text_color":"#112233","body_text_color":null}}
            }""")!!
            assertTrue(model.nativeThemeColors!!.containsKey("light"))
            assertNull(model.nativeThemeColors!!["light"])
            val place = mapper.toData(model) as NativeAdPlace
            assertEquals(model.nativeThemeColors, place.nativeThemeColors)
            assertEquals("#112233", place.nativeThemeColors!!["dark"]!!["primary_text_color"])
            assertNull(place.nativeThemeColors!!["dark"]!!["body_text_color"])
        }
    }

    @Test fun `theme mode and nested remote palettes survive JSON mapping`() {
        val model = adapter.fromJson("""{
            "place_name":"action_app_open", "ad_type":"native_interstitial",
            "native_template_size":"full_interstitial_v4", "native_theme":"shuffle",
            "native_theme_colors":{"light":{"primary_text_color":"#112233"},"dark":{"primary_text_color":"#FAFAFA","body_text_color":null}}
        }""")!!
        val place = mapper.toData(model) as NativeAdPlace
        assertEquals("shuffle", place.nativeTheme)
        assertEquals(model.nativeThemeColors, place.nativeThemeColors)
        assertEquals("#112233", place.nativeThemeColors!!["light"]!!["primary_text_color"])
        assertNull(place.nativeThemeColors!!["dark"]!!["body_text_color"])
        val missing = mapper.toData(adapter.fromJson("""{"place_name":"action_app_open","ad_type":"native_interstitial"}""")!!) as NativeAdPlace
        assertNull(missing.nativeTheme)
        assertNull(missing.nativeThemeColors)
    }

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
    fun `native interstitial preserves all supported fullscreen templates`() {
        fullscreenTemplates.forEach { template ->
            assertSame(template, mapTemplate("native_interstitial", template.key).nativeTemplateSize)
        }
    }

    @Test
    fun `native interstitial falls back to V3 for missing null blank and unknown templates`() {
        val missing = mapper.toData(adapter.fromJson("""{
            "place_name":"action_app_open", "ad_type":"native_interstitial"
        }""")!!) as NativeAdPlace
        assertSame(NativeTemplateSize.FullInterstitialV3, missing.nativeTemplateSize)
        listOf(null, "", " ", "unknown", "full_interstitial_v7", "FULL_INTERSTITIAL_V1").forEach { key ->
            assertSame("Template: $key", NativeTemplateSize.FullInterstitialV3,
                mapTemplate("native_interstitial", key).nativeTemplateSize)
        }
    }

    @Test
    fun `native interstitial rejects every built in non interstitial template`() {
        NativeTemplateSize.builtInTemplates.filterNot { it in fullscreenTemplates }.forEach { template ->
            assertSame("Template: ${template.key}", NativeTemplateSize.FullInterstitialV3,
                mapTemplate("native_interstitial", template.key).nativeTemplateSize)
        }
    }

    @Test
    fun `ordinary native keeps built in and custom template resolution`() {
        NativeTemplateSize.builtInTemplates.forEach { template ->
            assertSame(template, mapTemplate("native", template.key).nativeTemplateSize)
        }
        listOf(null, "", "custom_native").forEach { key ->
            val template = mapTemplate("native", key).nativeTemplateSize as NativeTemplateSize.CustomKey
            assertEquals(key.orEmpty(), template.customKey)
        }
    }

    @Test
    fun `nested native interstitial uses the same fullscreen template fallback`() {
        val place = mapper.toData(adapter.fromJson("""{
            "place_name":"action_app_open", "ad_type":"interstitial",
            "is_show_native_after":true,
            "native_config":{"place_name":"remote_native_after", "ad_type":"native_interstitial",
                "native_template_size":"small"}
        }""")!!) as InterstitialAdPlace
        assertSame(NativeTemplateSize.FullInterstitialV3, place.nativeAfterInterstitial?.nativeTemplateSize)
    }

    private val fullscreenTemplates = listOf(
        NativeTemplateSize.FullInterstitialV1,
        NativeTemplateSize.FullInterstitialV2,
        NativeTemplateSize.FullInterstitialV3,
        NativeTemplateSize.FullInterstitialV4,
        NativeTemplateSize.FullInterstitialV5,
        NativeTemplateSize.FullInterstitialV6,
        NativeTemplateSize.FullInterstitialShuffle,
    )

    @Test
    fun `remote close style is preserved for all concept templates`() {
        for (version in 4..6) for (style in listOf("close", "countdown", "unknown", "", null)) {
            val styleJson = style?.let { "\"$it\"" } ?: "null"
            val place = mapper.toData(adapter.fromJson("""{
                "place_name":"action_app_open", "ad_type":"native_interstitial",
                "native_template_size":"full_interstitial_v$version",
                "control_close_style":$styleJson, "control_close_position":"left",
                "count_down_timer":3, "progress_bar_tint":"#123456"
            }""")!!) as NativeAdPlace
            assertEquals(style, place.controlCloseStyle)
            assertEquals("left", place.controlClosePosition)
            assertEquals(3, place.countDownTimer)
            assertEquals("#123456", place.progressBarTint)
        }
        assertNull(mapTemplate("native_interstitial", "full_interstitial_v4").controlCloseStyle)
    }

    @Test fun `ready close style maps for V4 V5 V6 without inventing a value when absent`() {
        for (version in 4..6) for (style in listOf("compact", "default", " COMPACT ", "wrong", "", null)) {
            val json = style?.let { "\"$it\"" } ?: "null"
            val place = mapper.toData(adapter.fromJson("""{
                "place_name":"action_app_open", "ad_type":"native_interstitial",
                "native_template_size":"full_interstitial_v$version", "control_close_ready_style":$json
            }""")!!) as NativeAdPlace
            assertEquals(style, place.controlCloseReadyStyle)
        }
        assertNull(mapTemplate("native_interstitial", "full_interstitial_v4").controlCloseReadyStyle)
    }

    private fun mapTemplate(type: String, key: String?): NativeAdPlace {
        val model = adapter.fromJson("""{
            "place_name":"action_app_open", "ad_type":"$type"
        }""")!!.copy(nativeTemplateSize = key)
        return mapper.toData(model) as NativeAdPlace
    }

    @Test fun `remote shuffle position reaches all concept templates`() {
        for (version in 4..6) {
            val place = mapper.toData(adapter.fromJson("""{
                "place_name":"action_app_open", "ad_type":"native_interstitial",
                "native_template_size":"full_interstitial_v$version",
                "control_close_position":"shuffle"
            }""")!!) as NativeAdPlace
            assertEquals("shuffle", place.controlClosePosition)
            assertEquals("full_interstitial_v$version", place.nativeTemplateSize.key)
        }
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
