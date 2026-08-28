package com.codebasetemplate.features.feature_demo_native_collapsible.ui

import com.codebasetemplate.required.ads.AppAdPlaceName
import com.core.config.domain.data.AdType
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.NativeAdPlace
import com.core.config.domain.data.NativeExpandTemplate
import com.core.config.domain.data.NativeTemplateSize
import com.google.gson.JsonParser
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CollapsibleNativeTestConfigTest {

    @Test
    fun `uses a dedicated ad place for collapsible testing`() {
        assertEquals(
            "anchored_native_collapsible_test",
            AppAdPlaceName.ANCHORED_NATIVE_COLLAPSIBLE_TEST.name,
        )
    }

    @Test
    fun `default remote configs contain the dedicated collapsible test place`() {
        listOf("dev", "prod").forEach { flavor ->
            val places = JsonParser.parseString(readBannerNativePlaces(flavor)).asJsonArray
            val matches = places
                .map { it.asJsonObject }
                .filter { it["place_name"].asString == "anchored_native_collapsible_test" }

            assertEquals("$flavor should contain one test place", 1, matches.size)
            with(matches.single()) {
                assertEquals("ca-app-pub-3940256099942544/2247696110", get("ad_id").asString)
                assertEquals("native", get("ad_type").asString)
                assertEquals("small_cta_bottom", get("native_template_size").asString)
                assertTrue(get("is_native_collapsible").asBoolean)
                assertEquals("native_expand_v2", get("native_expand_template").asString)
                assertEquals("right", get("control_close_position").asString)
                assertEquals(10, get("collapsible_expand_cooldown_second").asInt)
                assertTrue(get("is_enable").asBoolean)
            }
        }
    }

    @Test
    fun `forces collapsible without changing remote config fields`() {
        val original = nativePlace(isCollapsible = false)

        val result = original.asForcedCollapsibleTestPlace()

        assertNotSame(original, result)
        assertFalse(original.isNativeCollapsible)
        assertTrue(result.isNativeCollapsible)
        assertEquals(original.nativeTemplateSize, result.nativeTemplateSize)
        assertEquals(original.nativeExpandTemplate, result.nativeExpandTemplate)
        assertEquals(original.controlClosePosition, result.controlClosePosition)
        assertEquals(original.collapsibleExpandCooldownSecond, result.collapsibleExpandCooldownSecond)
    }

    @Test
    fun `keeps an already collapsible place unchanged by value`() {
        val original = nativePlace(isCollapsible = true)

        assertEquals(original, original.asForcedCollapsibleTestPlace())
    }

    private fun nativePlace(isCollapsible: Boolean): NativeAdPlace {
        return NativeAdPlace(
            isTrackingShow = false,
            isTrackingClick = false,
            isNativeCollapsible = isCollapsible,
            nativeExpandTemplate = NativeExpandTemplate.V2,
            nativeTemplateSize = NativeTemplateSize.SmallCtaBottom,
            backgroundCta = null,
            ctaRadius = null,
            ctaTextColor = null,
            ctaTextSizeDp = null,
            ctaBorderColor = null,
            borderColor = null,
            backgroundColor = null,
            countDownTimer = null,
            closeStepCount = null,
            step1CountDownTimer = null,
            step2CountDownTimer = null,
            backgroundFullColor = null,
            backgroundColorAdsNotifyView = null,
            textColorAdsNotifyView = null,
            mediaBackgroundColor = null,
            backgroundRadius = null,
            primaryTextColor = null,
            primaryTextSizeDp = null,
            bodyTextColor = null,
            bodyTextSizeDp = null,
            isEnableFullScreenImmersive = null,
            expiredTimeSecond = null,
            refreshTimeSecond = 0,
            hideTextSkipCountDown = null,
            hideTextCountDown = null,
            hideProgressCountDown = null,
            progressBarTint = null,
            controlClosePosition = "left",
            collapsibleExpandCooldownSecond = 10,
            pipAnchorMode = null,
            pipLayoutFormat = null,
            pipMarginDp = null,
            pipTopMarginDp = null,
            placeName = TestPlace,
            adId = "test-ad-id",
            highFloorAdIds = emptyList(),
            isEnable = true,
            adType = AdType.Native,
            isAutoLoadAfterDismiss = true,
            isIgnoreInterval = false,
            isTutorialFlow = false,
        )
    }

    private object TestPlace : IAdPlaceName {
        override val name: String = "collapsible_native_test"
    }

    private fun readBannerNativePlaces(flavor: String): String {
        val relativePath = "src/$flavor/res/xml/remote_config_defaults.xml"
        val configFile = sequenceOf(File(relativePath), File("app/$relativePath"))
            .firstOrNull(File::isFile)
            ?: error("Cannot find $relativePath")
        val entryRegex = Regex(
            """<entry>\s*<key>banner_native_ad_places</key>\s*<value>(.*?)</value>\s*</entry>""",
            RegexOption.DOT_MATCHES_ALL,
        )
        return requireNotNull(entryRegex.find(configFile.readText())) {
            "Cannot find banner_native_ad_places in ${configFile.path}"
        }.groupValues[1]
    }
}
