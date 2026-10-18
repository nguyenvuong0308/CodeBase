package com.core.ads.customviews.ads

import android.app.Application
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.core.ads.R
import com.core.config.domain.data.NativeTemplateSize
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
class NativeInterstitialConceptViewTest {
    private fun view(version: Int): NativeInterstitialConceptView {
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar)
        return when (version) {
            4 -> NativeInterstitialV4View(context)
            5 -> NativeInterstitialV5View(context)
            else -> NativeInterstitialV6View(context)
        }
    }

    @Test fun `V4 both orientations keep assets and controls visible`() = verifyLayout(4)
    @Test fun `V5 both orientations keep assets and controls visible`() = verifyLayout(5)
    @Test fun `V6 both orientations keep assets and controls visible`() = verifyLayout(6)

    private fun verifyLayout(version: Int) {
        for (landscape in listOf(false, true)) {
            RuntimeEnvironment.setQualifiers(if (landscape) "w800dp-h360dp-land-mdpi" else "w360dp-h640dp-port-mdpi")
            val root = view(version)
            root.applyStyles(NativeTemplateStyle.Builder().withCountDownSecond(0).build())
            root.startCloseCountdown()
            root.findViewById<TextView>(R.id.primary).text = "Test Ad: A longer app title"
            root.findViewById<TextView>(R.id.body).text = "Explore all of the features of this application. Install it for free and get started today."
            root.findViewById<TextView>(R.id.cta).text = "CÀI ĐẶT"
            assertEquals(landscape, root.findViewById<View>(R.id.concept_media_end) != null)
            val sizes = if (landscape) listOf(914 to 411, 800 to 360, 640 to 320) else listOf(411 to 914, 360 to 640)
            for ((width, height) in sizes) {
                root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                root.layout(0, 0, width, height)
                val viewport = Rect(0, 0, width, height)
                val ids = listOf(R.id.primary, R.id.body, R.id.cta, R.id.tvClose, R.id.media_view, R.id.ad_notification_view)
                ids.forEach { id ->
                    val child = root.findViewById<View>(id)
                    val bounds = bounds(root, child)
                    val label = "V$version $width x $height ${root.resources.getResourceEntryName(id)}"
                    assertTrue("$label has no area", bounds.width() > 0 && bounds.height() > 0)
                    assertTrue("$label outside screen", viewport.contains(bounds))
                    var parent = child.parent as ViewGroup
                    while (parent !== root) {
                        assertTrue("$label clipped by parent", bounds(root, parent).contains(bounds))
                        parent = parent.parent as ViewGroup
                    }
                }
                val cta = bounds(root, root.findViewById(R.id.cta))
                val media = bounds(root, root.findViewById(R.id.media_view))
                assertTrue("CTA must have a 48dp touch height", cta.height() >= 48)
                assertTrue("Close must have a 48dp touch target", root.findViewById<View>(R.id.tvClose).width >= 48)
                assertTrue("Media must stay useful", media.width() >= width * 0.40f && media.height() >= height * 0.30f)
                listOf(R.id.primary, R.id.body, R.id.cta).forEach { id ->
                    assertFalse("Media must not obscure text or CTA", Rect.intersects(media, bounds(root, root.findViewById(id))))
                }
                val card = bounds(root, root.findViewById(R.id.background))
                if (version == 6) assertTrue("Spotlight CTA remains below the card", cta.top >= card.bottom)
                else assertTrue("CTA belongs inside content panel", card.contains(cta))
            }
        }
    }

    @Test fun `all colors use config exactly in both orientations including low contrast and reapply`() {
        for (landscape in listOf(false, true)) {
            RuntimeEnvironment.setQualifiers(if (landscape) "w800dp-h360dp-land-mdpi" else "w360dp-h640dp-port-mdpi")
            for (version in 4..6) {
                val root = view(version)
                for ((title, body) in listOf("#1F2937" to "#6B7280", "#80ABCDEF" to "#80987654")) {
                    root.applyStyles(NativeTemplateStyle.Builder()
                        .withBackgroundResource(R.drawable.core_bg_native_ads)
                        .withBackgroundColor("#121212").withBackgroundFullColor("#EEF0F4")
                        .withMediaBackgroundColor("#123456")
                        .withPrimaryTextTypefaceColor(title).withTertiaryTextTypefaceColor(body)
                        .withCallToActionBackgroundColor("#16A34A").withCallToActionTypefaceColor("#F0F0F0")
                        .withBackgroundColorAdsNotifyView("#FACC15").withTextColorAdsNotifyView("#111827")
                        .withControlClosePosition("left").build())
                    assertEquals(Color.parseColor(title), root.findViewById<TextView>(R.id.primary).currentTextColor)
                    assertEquals(Color.parseColor(body), root.findViewById<TextView>(R.id.body).currentTextColor)
                    assertEquals(Color.parseColor("#F0F0F0"), root.findViewById<TextView>(R.id.cta).currentTextColor)
                    assertEquals(Color.parseColor("#111827"), root.findViewById<TextView>(R.id.ad_notification_view).currentTextColor)
                    listOf(R.id.background to "#121212", R.id.controlClose to "#121212", R.id.native_ad_view to "#EEF0F4",
                        R.id.media_view to "#123456", R.id.layout_cta to "#16A34A").forEach { (id, color) ->
                        val bg = root.findViewById<View>(id).background
                        val actual = if (bg is ColorDrawable) bg.color else (bg as GradientDrawable).color!!.defaultColor
                        assertEquals(Color.parseColor(color), actual)
                    }
                    val closeParams = root.findViewById<View>(R.id.controlClose).layoutParams as FrameLayout.LayoutParams
                    assertEquals(Gravity.CENTER_VERTICAL or Gravity.START, closeParams.gravity)
                }
            }
        }
    }

    @Test fun `countdown cannot close early and pauses with the host`() {
        for (version in 4..6) {
            val root = view(version)
            var closed = 0
            root.onClose = { closed++ }
            root.applyStyles(NativeTemplateStyle.Builder().withCountDownSecond(3).build())
            root.startCloseCountdown()
            val close = root.findViewById<View>(R.id.tvClose)
            close.performClick()
            assertEquals(0, closed)
            shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
            root.onHostPause()
            shadowOf(Looper.getMainLooper()).idleFor(10, TimeUnit.SECONDS)
            assertEquals(View.INVISIBLE, close.visibility)
            root.onHostResume()
            shadowOf(Looper.getMainLooper()).idleFor(2, TimeUnit.SECONDS)
            assertEquals(View.VISIBLE, close.visibility)
            close.performClick()
            close.performClick()
            assertEquals(1, closed)
            root.startCloseCountdown()
            assertEquals(View.INVISIBLE, close.visibility)
            root.destroyNativeAd()
            shadowOf(Looper.getMainLooper()).idleFor(10, TimeUnit.SECONDS)
            assertFalse(close.isEnabled)
        }
    }

    @Test fun `zero and negative countdowns allow closing immediately`() {
        for (version in 4..6) for (seconds in listOf(0, -1)) {
            val root = view(version)
            root.applyStyles(NativeTemplateStyle.Builder().withCountDownSecond(seconds).build())
            root.startCloseCountdown()
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.tvClose).visibility)
        }
    }

    @Test fun `hidden countdown still waits before enabling close`() {
        val root = view(4)
        root.applyStyles(NativeTemplateStyle.Builder().withCountDownSecond(1).withHideTextCountDown(true).build())
        root.startCloseCountdown()
        assertEquals(View.INVISIBLE, root.findViewById<View>(R.id.lnCountDown).visibility)
        assertFalse(root.findViewById<View>(R.id.tvClose).isEnabled)
        shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
        assertTrue(root.findViewById<View>(R.id.tvClose).isEnabled)
    }

    @Test fun `new templates resolve to built in shimmer`() {
        listOf(NativeTemplateSize.FullInterstitialV4, NativeTemplateSize.FullInterstitialV5,
            NativeTemplateSize.FullInterstitialV6).forEach { template ->
            assertEquals(R.layout.gnt_full_cta_bottom_template_view_shimmer,
                resolveNativeShimmerLayout(template) { error("Must not call custom fallback") })
        }
    }

    @Test fun `close style shows disabled X and configured progress until countdown completes in both orientations`() {
        for (landscape in listOf(false, true)) {
            RuntimeEnvironment.setQualifiers(if (landscape) "w800dp-h360dp-land-mdpi" else "w360dp-h640dp-port-mdpi")
            for (version in 4..6) {
                val root = view(version)
                var closed = 0
                root.onClose = { closed++ }
                root.applyStyles(NativeTemplateStyle.Builder().withControlCloseStyle("close")
                    .withCountDownSecond(3).withPrimaryTextTypefaceColor("#ABCDEF")
                    .withProgressBarTint("#123456").build())
                root.startCloseCountdown()
                val close = root.findViewById<View>(R.id.tvClose)
                val progress = root.findViewById<CircularProgressIndicator>(R.id.actionProgress)
                assertEquals(View.VISIBLE, close.visibility)
                assertEquals(View.INVISIBLE, root.findViewById<View>(R.id.lnCountDown).visibility)
                assertFalse(close.isEnabled)
                assertEquals(0.65f, close.alpha, 0f)
                assertEquals(View.VISIBLE, progress.visibility)
                assertEquals(Color.parseColor("#123456"), progress.indicatorColor.single())
                assertEquals(0, progress.progress)
                close.performClick()
                assertEquals(0, closed)
                shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
                assertTrue(progress.progress in 1..999)
                root.onHostPause()
                val paused = progress.progress
                shadowOf(Looper.getMainLooper()).idleFor(10, TimeUnit.SECONDS)
                assertEquals(paused, progress.progress)
                assertFalse(close.isEnabled)
                root.onHostResume()
                shadowOf(Looper.getMainLooper()).idleFor(2, TimeUnit.SECONDS)
                assertEquals(1000, progress.progress)
                assertTrue(close.isEnabled)
                assertEquals(1f, close.alpha, 0f)
                close.performClick()
                close.performClick()
                assertEquals(1, closed)
                root.startCloseCountdown()
                assertEquals(0, progress.progress)
                assertFalse(close.isEnabled)
                root.destroyNativeAd()
                shadowOf(Looper.getMainLooper()).idleFor(5, TimeUnit.SECONDS)
                assertFalse(close.isEnabled)
                assertEquals(View.INVISIBLE, progress.visibility)
            }
        }
    }

    @Test fun `style can switch both ways without resetting or bypassing active countdown`() {
        for (version in 4..6) {
            val root = view(version)
            fun style(value: String?, hideProgress: Boolean = false) = NativeTemplateStyle.Builder()
                .withControlCloseStyle(value).withCountDownSecond(3)
                .withPrimaryTextTypefaceColor("#80ABCDEF").withHideProgressCountDown(hideProgress).build()
            root.applyStyles(style("countdown"))
            root.startCloseCountdown()
            shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
            val close = root.findViewById<View>(R.id.tvClose)
            val progress = root.findViewById<CircularProgressIndicator>(R.id.actionProgress)
            root.applyStyles(style(" CLOSE "))
            assertEquals(View.VISIBLE, close.visibility)
            assertFalse(close.isEnabled)
            assertEquals(Color.parseColor("#80ABCDEF"), progress.indicatorColor.single())
            assertTrue(progress.progress in 1..999)
            root.applyStyles(style("close", hideProgress = true))
            assertEquals(View.INVISIBLE, progress.visibility)
            assertFalse(close.isEnabled)
            for (value in listOf(null, "", "invalid", "countdown")) {
                root.applyStyles(style(value))
                assertEquals(View.INVISIBLE, close.visibility)
                assertEquals(View.VISIBLE, root.findViewById<View>(R.id.lnCountDown).visibility)
                assertEquals(View.INVISIBLE, progress.visibility)
                assertFalse(close.isEnabled)
            }
            root.applyStyles(style("close"))
            shadowOf(Looper.getMainLooper()).idleFor(2, TimeUnit.SECONDS)
            assertTrue(close.isEnabled)
            assertEquals(1000, progress.progress)
        }
    }

    @Test fun `close style with zero or negative duration is immediately enabled`() {
        for (version in 4..6) for (seconds in listOf(0, -1)) {
            val root = view(version)
            root.applyStyles(NativeTemplateStyle.Builder().withControlCloseStyle("close").withCountDownSecond(seconds).build())
            root.startCloseCountdown()
            assertTrue(root.findViewById<View>(R.id.tvClose).isEnabled)
            assertEquals(1000, root.findViewById<CircularProgressIndicator>(R.id.actionProgress).progress)
        }
    }

    private fun bounds(root: ViewGroup, child: View) = Rect().also {
        child.getDrawingRect(it)
        root.offsetDescendantRectToMyCoords(child, it)
    }

    @Test fun `shuffle keeps close and badge opposite and stable through styling and lifecycle`() {
        for (landscape in listOf(false, true)) for (version in 4..6) for (mode in listOf("close", "countdown")) {
            RuntimeEnvironment.setQualifiers(if (landscape) "w800dp-h360dp-land-mdpi" else "w360dp-h640dp-port-mdpi")
            val root = view(version)
            val style = NativeTemplateStyle.Builder().withControlClosePosition(" SHUFFLE ")
                .withControlCloseStyle(mode).withCountDownSecond(3).build()
            root.applyStyles(style)
            root.startCloseCountdown()
            fun gravity(id: Int) = (root.findViewById<View>(id).layoutParams as FrameLayout.LayoutParams).gravity
            val selected = gravity(R.id.controlClose)
            val left = Gravity.CENTER_VERTICAL or Gravity.START
            val right = Gravity.CENTER_VERTICAL or Gravity.END
            assertTrue(selected == left || selected == right)
            fun assertStable() {
                assertEquals(selected, gravity(R.id.controlClose))
                assertEquals(if (selected == left) right else left, gravity(R.id.ad_notification_view))
            }
            repeat(5) { root.applyStyles(style); assertStable() }
            val width = if (landscape) 800 else 360
            val height = if (landscape) 360 else 640
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            root.layout(0, 0, width, height)
            val closeBounds = bounds(root, root.findViewById(R.id.controlClose))
            val badgeBounds = bounds(root, root.findViewById(R.id.ad_notification_view))
            assertTrue(Rect(0, 0, width, height).contains(closeBounds))
            assertFalse(Rect.intersects(closeBounds, badgeBounds))
            shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
            root.onHostPause()
            shadowOf(Looper.getMainLooper()).idleFor(10, TimeUnit.SECONDS)
            assertFalse(root.findViewById<View>(R.id.tvClose).isEnabled)
            assertStable()
            root.onHostResume()
            shadowOf(Looper.getMainLooper()).idleFor(2, TimeUnit.SECONDS)
            assertTrue(root.findViewById<View>(R.id.tvClose).isEnabled)
            assertStable()
            root.destroyNativeAd()
        }
    }
}
