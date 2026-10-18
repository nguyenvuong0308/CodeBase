package com.core.config.data.mapper

import android.content.Context
import com.core.config.data.model.AdPlaceModel
import com.core.config.domain.data.AdType
import com.core.config.domain.data.IAppProviderAdPlaceName
import com.core.config.domain.data.NativeAdPlace
import com.core.config.domain.data.RemoteAdPlaceName
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock

class NativeInterstitialGuideCasesTest {
    @Test fun `all guide JSON configs preserve their styling through production mapper`() {
        val root = generateSequence(File(".").absoluteFile) { it.parentFile }
            .first { File(it, "docs/firebase-ads-guide/native-interstitial-cases.json").isFile }
        val moshi = Moshi.Builder().build()
        val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val adapter = moshi.adapter<List<Map<String, Any?>>>(Types.newParameterizedType(List::class.java, mapType))
        val cases = requireNotNull(adapter.fromJson(File(root, "docs/firebase-ads-guide/native-interstitial-cases.json").readText())) +
            requireNotNull(adapter.fromJson(File(root, "docs/firebase-ads-guide/native-theme-shuffle-cases.json").readText())) +
            requireNotNull(adapter.fromJson(File(root, "docs/firebase-ads-guide/native-template-shuffle-cases.json").readText()))
        val mapAdapter = moshi.adapter<Map<String, Any?>>(mapType)
        val placeAdapter = moshi.adapter(AdPlaceModel::class.java)
        val names = cases.map { (it["config"] as Map<*, *>)["place_name"] as String }.toSet()
        val provider = object : IAppProviderAdPlaceName {
            override fun findAdPlaceName(key: String) = key.takeIf { it in names }?.let(::RemoteAdPlaceName)
        }
        val mapper = AdPlaceModelMapper(provider, mock(Context::class.java))
        assertEquals(76, cases.size)
        cases.forEach { case ->
            @Suppress("UNCHECKED_CAST")
            val json = case["config"] as Map<String, Any?>
            val place = mapper.toData(requireNotNull(placeAdapter.fromJson(mapAdapter.toJson(json)))) as NativeAdPlace
            assertEquals(AdType.NativeInterstitial, place.adType)
            assertEquals(json["place_name"], place.placeName.name)
            assertEquals(json["ad_id"], place.adId)
            assertEquals(json["native_template_size"], place.nativeTemplateSize.key)
            val actual = mapOf(
                "is_enable" to place.isEnable,
                "is_tracking_click" to place.isTrackingClick,
                "is_auto_load_after_dismiss" to place.isAutoLoadAfterDismiss,
                "background_color" to place.backgroundColor,
                "background_full_color" to place.backgroundFullColor,
                "media_background_color" to place.mediaBackgroundColor,
                "primary_text_color" to place.primaryTextColor,
                "body_text_color" to place.bodyTextColor,
                "cta_text_color" to place.ctaTextColor,
                "background_cta" to place.backgroundCta,
                "border_color" to place.borderColor,
                "background_color_ads_notify_view" to place.backgroundColorAdsNotifyView,
                "text_color_ads_notify_view" to place.textColorAdsNotifyView,
                "control_close_position" to place.controlClosePosition,
                "control_close_style" to place.controlCloseStyle,
                "control_close_ready_style" to place.controlCloseReadyStyle,
                "native_theme" to place.nativeTheme,
                "native_theme_colors" to place.nativeThemeColors,
                "progress_bar_tint" to place.progressBarTint,
                "hide_text_count_down" to place.hideTextCountDown,
                "hide_progress_count_down" to place.hideProgressCountDown,
            )
            actual.forEach { (key, value) -> assertEquals("${case["id"]}: $key", json[key], value) }
            assertEquals((json["count_down_timer"] as Number).toInt(), place.countDownTimer)
            assertEquals((json["background_radius"] as Number).toInt(), place.backgroundRadius)
            assertEquals((json["cta_radius_dp"] as Number).toInt(), place.ctaRadius)
        }
    }
}
