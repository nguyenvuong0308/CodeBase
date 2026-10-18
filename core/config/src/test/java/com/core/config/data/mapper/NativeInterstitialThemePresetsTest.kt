package com.core.config.data.mapper

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class NativeInterstitialThemePresetsTest {
    private fun parse(json: String): List<Map<String, Any?>> = requireNotNull(
        Moshi.Builder().build().adapter<List<Map<String, Any?>>>(Types.newParameterizedType(
            List::class.java, Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        )).fromJson(json)
    )
    private val root = generateSequence(File(".").absoluteFile) { it.parentFile }
        .first { File(it, "app/src/dev/res/xml/remote_config_defaults.xml").isFile }

    @Test fun `six exported presets match dev config and retain template and close settings`() {
        val xml = File(root, "app/src/dev/res/xml/remote_config_defaults.xml").readText()
        val json = Regex("""<key>rewarded_rewardedinter_inter_ad_places</key>\s*<value>(.*?)</value>""",
            RegexOption.DOT_MATCHES_ALL).find(xml)!!.groupValues[1]
        val defaults = parse(json)
        val presets = parse(File(root, "docs/native-interstitial-theme-presets.json").readText().removePrefix("\uFEFF"))

        assertEquals(6, presets.size)
        assertEquals(6, presets.map { it["place_name"] }.distinct().size)
        for (version in 4..6) for (theme in listOf("dark", "light")) {
            val name = "fullscreen_native_interstitial_v${version}_$theme"
            val preset = presets.single { it["place_name"] == name }
            assertEquals(preset, defaults.single { it["place_name"] == name })
            assertEquals("full_interstitial_v$version", preset["native_template_size"])
            assertEquals("native_interstitial", preset["ad_type"])
            assertEquals("left", preset["control_close_position"])
            assertEquals("close", preset["control_close_style"])
            assertEquals(5.0, preset["count_down_timer"])
            assertEquals("ca-app-pub-3940256099942544/2247696110", preset["ad_id"])
            assertEquals(true, preset["is_enable"])
            val dark = theme == "dark"
            val card = if (dark) { if (version == 6) "#202329" else "#121212" } else "#FFFFFF"
            val full = if (dark) { if (version == 6) "#101216" else "#121212" }
                else if (version == 6) "#EEF0F4" else "#FFFFFF"
            assertEquals(card, preset["background_color"])
            assertEquals(full, preset["background_full_color"])
            assertEquals(if (dark) "#121212" else "#FFFFFF", preset["media_background_color"])
            assertEquals(if (dark) "#FFFFFF" else "#111827", preset["primary_text_color"])
            assertEquals(if (dark) "#D1D5DB" else "#4B5563", preset["body_text_color"])
            assertEquals(preset["primary_text_color"], preset["progress_bar_tint"])
            assertEquals(if (dark) "#16A34A" else "#15803D", preset["background_cta"])
            assertEquals("#FFFFFF", preset["cta_text_color"])
        }
    }
}
