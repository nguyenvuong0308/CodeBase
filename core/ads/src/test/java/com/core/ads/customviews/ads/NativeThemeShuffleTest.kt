package com.core.ads.customviews.ads

import android.app.Application
import android.graphics.Color
import android.os.Looper
import android.view.ContextThemeWrapper
import android.widget.TextView
import com.core.ads.R
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class NativeThemeShuffleTest {
    @Test fun `template factory draws once and creates each supported orientation layout`() {
        for (landscape in listOf(false, true)) for (index in 0..2) {
            RuntimeEnvironment.setQualifiers(if (landscape) "w800dp-h360dp-land-mdpi" else "w360dp-h800dp-port-mdpi")
            val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar)
            var draws = 0
            val root = createShuffledNativeInterstitial(context) { draws++; index }
            assertEquals(listOf(NativeInterstitialV4View::class.java, NativeInterstitialV5View::class.java, NativeInterstitialV6View::class.java)[index], root.javaClass)
            assertEquals(landscape, root.findViewById<android.view.View>(R.id.concept_media_end) != null)
            root.applyStyles(style("shuffle"))
            root.startCloseCountdown()
            root.onHostPause(); root.onHostResume()
            shadowOf(Looper.getMainLooper()).idleFor(3, TimeUnit.SECONDS)
            root.applyStyles(style("shuffle"))
            assertEquals(1, draws)
            root.destroyNativeAd()
        }
        assertEquals(R.layout.gnt_full_cta_bottom_template_view_shimmer,
            resolveNativeShimmerLayout(com.core.config.domain.data.NativeTemplateSize.FullInterstitialShuffle) { error("No custom layout") })
    }

    @Test fun `resolver chooses once per bind and covers both random branches`() {
        var draws = 0
        val resolver = NativeThemeResolver { draws++ % 2 == 0 }
        repeat(10) { assertEquals("dark", resolver.resolve(" ShUfFlE ")) }
        assertEquals(1, draws)
        resolver.beginAd()
        repeat(10) { assertEquals("light", resolver.resolve("shuffle")) }
        assertEquals(2, draws)
        assertEquals("light", resolver.resolve(" LIGHT "))
        assertEquals("dark", resolver.resolve("DARK"))
        listOf(null, "", "auto", "invalid").forEach { assertNull(resolver.resolve(it)) }
        assertEquals(2, draws)
    }

    private val palettes = mapOf(
        "dark" to mapOf("primary_text_color" to "#FAFAFA", "body_text_color" to "#CBD5E1", "background_color" to "#101827", "media_background_color" to "#101827"),
        "light" to mapOf("primary_text_color" to "#121212", "body_text_color" to "#4B5563", "background_color" to "#FFFFFF", "media_background_color" to "#FFFFFF"),
    )

    private fun style(mode: String?) = NativeTemplateStyle.Builder()
        .withNativeTheme(mode, palettes).withCountDownSecond(2)
        .withControlClosePosition("shuffle").withControlCloseStyle("close")
        .withControlCloseReadyStyle("compact")
        .withBackgroundColor("#334455").withPrimaryTextTypefaceColor("#123456")
        .withMediaBackgroundColor("#334455")
        .withTertiaryTextTypefaceColor("#654321").withProgressBarTint("#AABBCC").build()

    @Test fun `theme overrides are isolated and invalid or missing colors retain base`() {
        val base = style("shuffle")
        val dark = base.forTheme("dark")
        assertEquals("#FAFAFA", dark.primaryTextTypefaceColor)
        assertEquals("#FAFAFA", dark.progressBarTint)
        assertEquals("#123456", base.primaryTextTypefaceColor)
        assertEquals("#AABBCC", base.progressBarTint)
        assertSame(base, base.forTheme(null))
        assertSame(base, base.forTheme("missing"))
        val bad = NativeTemplateStyle.Builder().withBackgroundColor("#123456")
            .withNativeTheme("light", mapOf("light" to mapOf("background_color" to "not-a-color", "primary_text_color" to null)))
            .build().forTheme("light")
        assertEquals("#123456", bad.backgroundColor)
        assertNull(bad.primaryTextTypefaceColor)
    }

    @Test fun `all supported palette fields override their corresponding style fields`() {
        val keys = listOf("background_color", "background_full_color", "media_background_color",
            "primary_text_color", "body_text_color", "cta_text_color", "background_cta",
            "cta_border_color", "border_color", "background_color_ads_notify_view",
            "text_color_ads_notify_view", "progress_bar_tint")
        val s = NativeTemplateStyle.Builder().withNativeTheme("light",
            mapOf("light" to keys.associateWith { "#A1B2C3" })).build().forTheme("light")
        listOf(s.backgroundColor, s.backgroundFullColor, s.mediaBackgroundColor,
            s.primaryTextTypefaceColor, s.tertiaryTextTypefaceColor, s.callToActionBackgroundColor,
            s.callToActionBorderColor, s.borderColor, s.backgroundColorAdsNotifyView,
            s.textColorAdsNotifyView, s.progressBarTint).forEach { assertEquals("#A1B2C3", it) }
        assertEquals(Color.parseColor("#A1B2C3"), s.callToActionTypefaceColor)
    }

    @Test fun `V4 V5 V6 keep selected palette across countdown restyle and resume in both orientations`() {
        for (landscape in listOf(false, true)) for (version in 4..6) for (mode in listOf("shuffle", "light", "dark")) {
            RuntimeEnvironment.setQualifiers(if (landscape) "w800dp-h360dp-land-mdpi" else "w360dp-h800dp-port-mdpi")
            val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar)
            val root = when (version) { 4 -> NativeInterstitialV4View(context); 5 -> NativeInterstitialV5View(context); else -> NativeInterstitialV6View(context) }
            val base = style(mode)
            root.applyStyles(base)
            assertNull(root.resolvedNativeTheme)
            root.startCloseCountdown()
            val chosen = requireNotNull(root.resolvedNativeTheme)
            assertTrue(chosen in palettes.keys)
            if (mode != "shuffle") assertEquals(mode, chosen)
            fun check() {
                assertEquals(chosen, root.resolvedNativeTheme)
                assertEquals(Color.parseColor(palettes.getValue(chosen).getValue("primary_text_color")), root.findViewById<TextView>(R.id.primary).currentTextColor)
                assertEquals(Color.parseColor(palettes.getValue(chosen).getValue("body_text_color")), root.findViewById<TextView>(R.id.body).currentTextColor)
                val mediaBackground = root.findViewById<android.view.View>(R.id.media_view).background
                val mediaColor = when (mediaBackground) {
                    is android.graphics.drawable.ColorDrawable -> mediaBackground.color
                    is android.graphics.drawable.GradientDrawable -> mediaBackground.color!!.defaultColor
                    else -> error("Unsupported media background")
                }
                assertEquals(Color.parseColor(palettes.getValue(chosen).getValue("media_background_color")), mediaColor)
                assertEquals(root.findViewById<TextView>(R.id.primary).currentTextColor, root.findViewById<CircularProgressIndicator>(R.id.actionProgress).indicatorColor.single())
            }
            check()
            root.onHostPause()
            shadowOf(Looper.getMainLooper()).idleFor(3, TimeUnit.SECONDS)
            root.applyStyles(base)
            check()
            root.onHostResume()
            shadowOf(Looper.getMainLooper()).idleFor(3, TimeUnit.SECONDS)
            check()
            root.applyStyles(style(null))
            assertNull(root.resolvedNativeTheme)
            assertEquals(Color.parseColor("#123456"), root.findViewById<TextView>(R.id.primary).currentTextColor)
            root.destroyNativeAd()
        }
    }
}
