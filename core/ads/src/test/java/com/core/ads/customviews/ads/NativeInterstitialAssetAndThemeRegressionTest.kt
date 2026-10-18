package com.core.ads.customviews.ads

import android.app.Application
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
class NativeInterstitialAssetAndThemeRegressionTest {
    private fun view(version: Int): NativeInterstitialConceptView {
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar)
        return when (version) { 4 -> NativeInterstitialV4View(context); 5 -> NativeInterstitialV5View(context); else -> NativeInterstitialV6View(context) }
    }

    private fun eachLayout(block: (NativeInterstitialConceptView, Boolean) -> Unit) {
        for (landscape in listOf(false, true)) for (version in 4..6) {
            RuntimeEnvironment.setQualifiers(if (landscape) "w800dp-h360dp-land-mdpi" else "w360dp-h640dp-port-mdpi")
            val root = view(version)
            try { block(root, landscape) } finally { root.destroyNativeAd() }
        }
    }

    private fun bounds(root: ViewGroup, view: View) = Rect().also {
        view.getDrawingRect(it)
        root.offsetDescendantRectToMyCoords(view, it)
    }

    @Test fun `optional icon body and CTA do not clip or overlap headline in either orientation`() = eachLayout { root, landscape ->
        root.findViewById<TextView>(R.id.primary).text = "A longer advertising title for this application"
        root.findViewById<TextView>(R.id.body).text = "Explore the features of this application and get started today."
        root.findViewById<TextView>(R.id.cta).text = "Install"
        for (hasIcon in listOf(true, false)) for (hasBody in listOf(true, false)) for (hasCta in listOf(true, false)) {
            // These are the same visibility states used by setNativeAd for optional SDK assets.
            root.findViewById<View>(R.id.icon).visibility = if (hasIcon) View.VISIBLE else View.GONE
            root.findViewById<View>(R.id.body).visibility = if (hasBody) View.VISIBLE else View.GONE
            root.findViewById<View>(R.id.layout_cta).visibility = if (hasCta) View.VISIBLE else View.GONE
            val width = if (landscape) 800 else 360
            val height = if (landscape) 360 else 640
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            root.layout(0, 0, width, height)
            val ids = listOfNotNull(R.id.primary, R.id.icon.takeIf { hasIcon }, R.id.body.takeIf { hasBody }, R.id.cta.takeIf { hasCta })
            val label = "${root.javaClass.simpleName} landscape=$landscape icon=$hasIcon body=$hasBody cta=$hasCta"
            for (id in ids) {
                val v = root.findViewById<View>(id)
                val rect = bounds(root, v)
                assertTrue("$label empty asset $id", rect.width() > 0 && rect.height() > 0)
                var parent = v.parent as ViewGroup
                while (true) {
                    assertTrue("$label $id clipped: $rect by ${bounds(root, parent)}", bounds(root, parent).contains(rect))
                    if (parent === root) break
                    parent = parent.parent as ViewGroup
                }
            }
            for (a in ids.indices) for (b in a + 1 until ids.size) {
                assertFalse("$label assets overlap", Rect.intersects(bounds(root, root.findViewById(ids[a])), bounds(root, root.findViewById(ids[b]))))
            }
        }
    }

    private val palette = listOf("background_color", "background_full_color", "media_background_color", "primary_text_color", "body_text_color", "cta_text_color", "background_cta", "cta_border_color", "border_color", "background_color_ads_notify_view", "text_color_ads_notify_view", "progress_bar_tint").associateWith { "#EC4899" }

    private fun builder(mode: String?, colors: Map<String, Map<String, String?>?>?, explicitBase: Boolean = false) = NativeTemplateStyle.Builder()
        .withNativeTheme(mode, colors).withCountDownSecond(2).withControlCloseStyle("close")
        .apply {
            if (explicitBase) {
                withBackgroundColor("#123456"); withBackgroundFullColor("#234567"); withMediaBackgroundColor("#345678")
                withPrimaryTextTypefaceColor("#456789"); withTertiaryTextTypefaceColor("#56789A")
                withCallToActionTypefaceColor("#6789AB"); withCallToActionBackgroundColor("#789ABC")
                withBackgroundColorAdsNotifyView("#89ABCD"); withTextColorAdsNotifyView("#9ABCDE"); withProgressBarTint("#ABCDEF")
            }
        }

    private fun colors(root: View): List<Any?> = buildList {
        for (id in listOf(R.id.primary, R.id.body, R.id.cta, R.id.tvCountDownTime, R.id.ad_notification_view)) {
            add(root.findViewById<TextView>(id).textColors)
        }
        for (id in listOf(R.id.native_ad_view, R.id.background, R.id.media_view, R.id.layout_cta, R.id.controlClose, R.id.ad_notification_view)) {
            add(when (val bg = root.findViewById<View>(id).background) {
                null -> null
                is ColorDrawable -> bg.color
                is GradientDrawable -> listOf(bg.color, bg.colors?.toList(), bg.cornerRadius)
                else -> error("Unexpected background $bg")
            })
        }
        add(root.findViewById<ImageView>(R.id.tvClose).colorFilter)
        add(root.findViewById<CircularProgressIndicator>(R.id.actionProgress).indicatorColor.toList())
    }

    @Test fun `leaving palette restores XML defaults or base colors including missing null and invalid palettes`() = eachLayout { root, _ ->
        for (explicitBase in listOf(false, true)) {
            val base = builder(null, null, explicitBase).build()
            root.applyStyles(base)
            root.startCloseCountdown()
            val expected = colors(root)
            val cases = listOf(
                builder(null, null, explicitBase),
                builder("unknown", mapOf("dark" to palette), explicitBase),
                builder("light", mapOf("dark" to palette), explicitBase),
                builder("light", mapOf("light" to null, "dark" to palette), explicitBase),
                builder("light", mapOf("light" to emptyMap()), explicitBase),
                builder("light", mapOf("light" to palette.mapValues { "#FFF" }), explicitBase),
            )
            for (next in cases) {
                root.applyStyles(builder("dark", mapOf("dark" to palette), explicitBase).build())
                assertNotEquals(expected, colors(root))
                root.applyStyles(next.build())
                assertEquals("All theme colors must return to base", expected, colors(root))
                assertFalse("Restyling must not bypass countdown", root.findViewById<View>(R.id.tvClose).isEnabled)
                root.startCloseCountdown()
                assertEquals("Rebind must also preserve the base", expected, colors(root))
            }
            shadowOf(Looper.getMainLooper()).idleFor(2, TimeUnit.SECONDS)
            assertTrue(root.findViewById<View>(R.id.tvClose).isEnabled)
        }
    }

    @Test fun `partial palette restores omitted colors instead of inheriting previous theme`() = eachLayout { root, _ ->
        root.applyStyles(builder(null, null).build())
        root.startCloseCountdown()
        val bodyColor = root.findViewById<TextView>(R.id.body).textColors
        root.applyStyles(builder("dark", mapOf("dark" to palette)).build())
        root.applyStyles(builder("light", mapOf("light" to mapOf("primary_text_color" to "#112233"))).build())
        assertEquals(android.graphics.Color.parseColor("#112233"), root.findViewById<TextView>(R.id.primary).currentTextColor)
        assertEquals(bodyColor, root.findViewById<TextView>(R.id.body).textColors)
    }

    @Test fun `theme restoration preserves gradients and compact close through pause resume and rebind`() = eachLayout { root, _ ->
        fun style(mode: String?) = builder(mode, mapOf("dark" to palette))
            .withBackgroundColor("#112233,#445566").withCallToActionBackgroundColor("#223344,#556677")
            .withControlCloseReadyStyle("compact").build()
        root.applyStyles(style(null))
        root.startCloseCountdown()
        val expected = colors(root)
        root.applyStyles(style("dark"))
        root.onHostPause()
        shadowOf(Looper.getMainLooper()).idleFor(3, TimeUnit.SECONDS)
        root.applyStyles(style("light"))
        assertEquals(expected, colors(root))
        assertFalse(root.findViewById<View>(R.id.tvClose).isEnabled)
        root.onHostResume()
        shadowOf(Looper.getMainLooper()).idleFor(2, TimeUnit.SECONDS)
        root.applyStyles(style("dark")); root.applyStyles(style("light"))
        assertNull(root.findViewById<View>(R.id.controlClose).background)
        root.startCloseCountdown()
        assertEquals(expected, colors(root))
    }
}
