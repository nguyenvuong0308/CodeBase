package com.core.ads.customviews.ads

import android.app.Application
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.TextView
import com.core.ads.R
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class NativeRemoteColorSafetyTest {
    private val invalid = listOf("#FFF", "#FFFFF", "", "#ff 0000", "not-a-color")
    private val setters: List<NativeTemplateStyle.Builder.(String) -> NativeTemplateStyle.Builder> = listOf(
        { withBackgroundFullColor(it) }, { withMediaBackgroundColor(it) }, { withBorderColor(it) },
        { withPrimaryTextTypefaceColor(it) }, { withTertiaryTextTypefaceColor(it) },
        { withCtaBorderColor(it) }, { withProgressBarTint(it) },
    )

    private fun view(version: Int): NativeInterstitialConceptView {
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar)
        return when (version) { 4 -> NativeInterstitialV4View(context); 5 -> NativeInterstitialV5View(context); else -> NativeInterstitialV6View(context) }
    }

    private fun valid() = NativeTemplateStyle.Builder().withBackgroundColor("#112233")
        .withBackgroundFullColor("#223344").withMediaBackgroundColor("#334455")
        .withBorderColor("#445566").withPrimaryTextTypefaceColor("#556677")
        .withTertiaryTextTypefaceColor("#667788").withCtaBorderColor("#778899")
        .withCallToActionBackgroundColor("#123456").withProgressBarTint("#8899AA")
        .withControlCloseStyle("close").withControlCloseReadyStyle("compact").withCountDownSecond(1)

    private fun background(root: View, id: Int) = when (val bg = root.findViewById<View>(id).background) {
        is ColorDrawable -> bg.color
        is GradientDrawable -> bg.color!!.defaultColor
        else -> error("Unsupported background: $bg")
    }

    @Test fun `each malformed base color is ignored without skipping later fields in V4 V5 V6`() {
        for (landscape in listOf(false, true)) for (version in 4..6) {
            RuntimeEnvironment.setQualifiers(if (landscape) "w800dp-h360dp-land-mdpi" else "w360dp-h800dp-port-mdpi")
            val root = view(version)
            for ((field, setter) in setters.withIndex()) for (bad in invalid) {
                root.applyStyles(valid().build())
                root.applyStyles(valid().apply { setter(bad) }.withCallToActionBackgroundColor("#ABCDEF").build())
                assertEquals(Color.parseColor("#223344"), background(root, R.id.native_ad_view))
                assertEquals(Color.parseColor("#334455"), background(root, R.id.media_view))
                assertEquals(Color.parseColor("#556677"), root.findViewById<TextView>(R.id.primary).currentTextColor)
                assertEquals(Color.parseColor("#667788"), root.findViewById<TextView>(R.id.body).currentTextColor)
                assertEquals("A bad field must not abort the rest of styling", Color.parseColor("#ABCDEF"), background(root, R.id.layout_cta))
                val expectedProgress = if (field == 6) "#556677" else "#8899AA"
                assertEquals(Color.parseColor(expectedProgress), root.findViewById<CircularProgressIndicator>(R.id.actionProgress).indicatorColor.single())
            }
            root.destroyNativeAd()
        }
    }

    @Test fun `invalid colors on first bind and repeat bind do not break countdown or close`() {
        for (version in 4..6) for (bad in invalid) {
            val root = view(version)
            var closes = 0
            root.onClose = { closes++ }
            val builder = valid()
            setters.forEach { setter -> setter(builder, bad) }
            val style = builder.build()
            root.applyStyles(style)
            repeat(2) {
                root.startCloseCountdown() // Reapplies the same malformed remote style.
                val close = root.findViewById<View>(R.id.tvClose)
                assertFalse(close.isEnabled)
                close.performClick()
                assertEquals(it, closes)
                shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
                assertTrue(close.isEnabled)
                assertNull(root.findViewById<View>(R.id.controlClose).background)
                close.performClick()
                assertEquals(it + 1, closes)
            }
            root.destroyNativeAd()
        }
    }

    @Test fun `progress uses valid title then current title when both remote colors are invalid`() {
        val root = view(4)
        root.applyStyles(valid().build())
        val progress = root.findViewById<CircularProgressIndicator>(R.id.actionProgress)
        for (bad in invalid) {
            root.applyStyles(valid().withPrimaryTextTypefaceColor("#AABBCC").withProgressBarTint(bad).build())
            assertEquals(Color.parseColor("#AABBCC"), progress.indicatorColor.single())
            root.applyStyles(valid().withPrimaryTextTypefaceColor(bad).withProgressBarTint(bad).build())
            assertEquals(Color.parseColor("#AABBCC"), progress.indicatorColor.single())
        }
        root.destroyNativeAd()
    }

    @Test fun `null palettes are dropped and valid palettes are defensively copied`() {
        val dark = mutableMapOf<String, String?>("primary_text_color" to "#AABBCC", "body_text_color" to null)
        val palettes = mutableMapOf<String, Map<String, String?>?>("light" to null, "dark" to dark)
        val style = valid().withNativeTheme("shuffle", palettes).build()
        dark["primary_text_color"] = "#000000"
        palettes.clear()
        assertEquals(setOf("dark"), style.nativeThemeColors.keys)
        assertEquals("#AABBCC", style.forTheme("dark").primaryTextTypefaceColor)
        assertSame(style, style.forTheme("light"))
        assertTrue(valid().withNativeTheme("shuffle", mapOf("light" to null, "dark" to null)).build().nativeThemeColors.isEmpty())
        assertTrue(valid().withNativeTheme("shuffle", null).build().nativeThemeColors.isEmpty())
    }

    @Test fun `valid selected palette can override malformed base and null palette falls back safely`() {
        for (version in 4..6) for (mode in listOf("light", "dark", "shuffle")) {
            val root = view(version)
            val builder = valid()
            setters.forEach { setter -> setter(builder, "#FFFFF") }
            root.applyStyles(builder.withNativeTheme(mode, mapOf(
                "light" to null, "dark" to mapOf("primary_text_color" to "#CCDDFF", "media_background_color" to "#010203")
            )).build())
            root.startCloseCountdown()
            if (root.resolvedNativeTheme == "dark") {
                assertEquals(Color.parseColor("#CCDDFF"), root.findViewById<TextView>(R.id.primary).currentTextColor)
                assertEquals(Color.parseColor("#010203"), background(root, R.id.media_view))
            }
            shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
            assertTrue(root.findViewById<View>(R.id.tvClose).isEnabled)
            root.destroyNativeAd()
        }
    }
}
