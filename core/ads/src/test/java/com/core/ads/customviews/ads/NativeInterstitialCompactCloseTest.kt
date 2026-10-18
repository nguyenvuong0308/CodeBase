package com.core.ads.customviews.ads

import android.app.Application
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.core.ads.R
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
class NativeInterstitialCompactCloseTest {
    private fun view(version: Int): NativeInterstitialConceptView {
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar)
        return when (version) {
            4 -> NativeInterstitialV4View(context)
            5 -> NativeInterstitialV5View(context)
            else -> NativeInterstitialV6View(context)
        }
    }

    private fun style(ready: String? = "compact", mode: String = "close", seconds: Int = 3,
                      position: String = "shuffle", color: String = "#202329") = NativeTemplateStyle.Builder()
        .withControlCloseReadyStyle(ready).withControlCloseStyle(mode).withCountDownSecond(seconds)
        .withControlClosePosition(position).withBackgroundColor(color)
        .withPrimaryTextTypefaceColor("#ABCDEF").build()

    private fun layout(root: View, landscape: Boolean = false) {
        val width = if (landscape) 800 else 360
        val height = if (landscape) 360 else 800
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, width, height)
    }

    private fun assertShape(root: View, compact: Boolean, mode: String = "close") {
        val close = root.findViewById<View>(R.id.tvClose)
        val container = root.findViewById<View>(R.id.controlClose)
        val size = if (compact) 28 else 48
        val padding = if (compact) 4 else 12
        assertEquals(size, close.width)
        assertEquals(size, close.height)
        listOf(close.paddingLeft, close.paddingTop, close.paddingRight, close.paddingBottom)
            .forEach { assertEquals(padding, it) }
        assertEquals(48, container.width)
        assertEquals(48, container.height)
        assertEquals((48 - size) / 2, close.left)
        assertEquals((48 - size) / 2, close.top)
        if (compact) assertNull(container.background) else assertNotNull(container.background)
        assertEquals(if (!compact && mode == "close") View.VISIBLE else View.INVISIBLE,
            root.findViewById<View>(R.id.actionProgress).visibility)
    }

    @Test fun `compact starts only after countdown in all templates orientations styles and positions`() {
        for (landscape in listOf(false, true)) for (version in 4..6)
            for (mode in listOf("close", "countdown")) for (position in listOf("left", "right", "shuffle")) {
                RuntimeEnvironment.setQualifiers(if (landscape) "w800dp-h360dp-land-mdpi" else "w360dp-h800dp-port-mdpi")
                val root = view(version)
                var callbacks = 0
                root.onClose = { callbacks++ }
                root.applyStyles(style(mode = mode, position = position))
                root.startCloseCountdown()
                layout(root, landscape)
                val close = root.findViewById<ImageView>(R.id.tvClose)
                val container = root.findViewById<View>(R.id.controlClose)
                val gravity = (container.layoutParams as FrameLayout.LayoutParams).gravity
                assertShape(root, compact = false, mode = mode)
                assertFalse(close.isEnabled)
                assertFalse(container.isEnabled)
                close.performClick()
                container.performClick()
                assertEquals(0, callbacks)
                shadowOf(Looper.getMainLooper()).idleFor(3, TimeUnit.SECONDS)
                layout(root, landscape)
                assertShape(root, compact = true, mode = mode)
                assertEquals(View.VISIBLE, close.visibility)
                assertEquals(View.INVISIBLE, root.findViewById<View>(R.id.lnCountDown).visibility)
                assertTrue(close.isEnabled)
                assertTrue(container.isEnabled)
                assertTrue(container.isClickable)
                assertEquals(1f, close.alpha, 0f)
                assertEquals(PorterDuffColorFilter(Color.parseColor("#ABCDEF"), PorterDuff.Mode.SRC_ATOP), close.colorFilter)
                assertEquals(gravity, (container.layoutParams as FrameLayout.LayoutParams).gravity)
                val badgeGravity = (root.findViewById<View>(R.id.ad_notification_view).layoutParams as FrameLayout.LayoutParams).gravity
                assertNotEquals(gravity, badgeGravity)
                container.performClick() // The transparent area must activate the same guarded close.
                close.performClick()
                container.performClick()
                assertEquals(1, callbacks)
                root.startCloseCountdown()
                layout(root, landscape)
                assertShape(root, compact = false, mode = mode)
                assertFalse(close.isEnabled)
                root.destroyNativeAd()
            }
    }

    @Test fun `reapply can restore default and recolor hidden surface without resetting ready state`() {
        RuntimeEnvironment.setQualifiers("w360dp-h800dp-port-mdpi")
        for (version in 4..6) {
            val root = view(version)
            root.applyStyles(style())
            root.startCloseCountdown()
            shadowOf(Looper.getMainLooper()).idleFor(3, TimeUnit.SECONDS)
            for (value in listOf(null, "", "wrong", "default", " DEFAULT ")) {
                root.applyStyles(style(ready = value))
                layout(root)
                assertShape(root, compact = false)
                assertTrue(root.findViewById<View>(R.id.tvClose).isEnabled)
                assertFalse(root.findViewById<View>(R.id.controlClose).isClickable)
                root.applyStyles(style(ready = " CoMpAcT ", color = "#123456"))
                layout(root)
                assertShape(root, compact = true)
            }
            root.startCloseCountdown()
            layout(root)
            assertShape(root, compact = false)
            val background = root.findViewById<View>(R.id.controlClose).background as GradientDrawable
            assertEquals(Color.parseColor("#123456"), background.color!!.defaultColor)
            root.destroyNativeAd()
        }
    }

    @Test fun `pause resume preserves waiting duration and compact ready state`() {
        RuntimeEnvironment.setQualifiers("w360dp-h800dp-port-mdpi")
        for (version in 4..6) {
            val root = view(version)
            root.applyStyles(style())
            root.startCloseCountdown()
            shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
            root.onHostPause()
            shadowOf(Looper.getMainLooper()).idleFor(10, TimeUnit.SECONDS)
            layout(root)
            assertShape(root, compact = false)
            assertFalse(root.findViewById<View>(R.id.tvClose).isEnabled)
            root.onHostResume()
            shadowOf(Looper.getMainLooper()).idleFor(2, TimeUnit.SECONDS)
            root.onHostPause()
            root.onHostResume()
            layout(root)
            assertShape(root, compact = true)
            assertTrue(root.findViewById<View>(R.id.tvClose).isEnabled)
            root.destroyNativeAd()
        }
    }

    @Test fun `zero and negative timer bind directly to compact with correct pixel rounding`() {
        RuntimeEnvironment.setQualifiers("w360dp-h800dp-port-420dpi")
        for (version in 4..6) for (seconds in listOf(0, -1)) {
            val root = view(version)
            root.applyStyles(style(seconds = seconds))
            root.startCloseCountdown()
            layout(root)
            val close = root.findViewById<View>(R.id.tvClose)
            assertEquals(root.resources.getDimensionPixelSize(R.dimen.native_interstitial_close_compact_size), close.width)
            assertEquals(root.resources.getDimensionPixelSize(R.dimen.native_interstitial_close_compact_padding), close.paddingLeft)
            assertTrue(close.isEnabled)
            assertNull(root.findViewById<View>(R.id.controlClose).background)
            assertEquals(View.INVISIBLE, root.findViewById<View>(R.id.actionProgress).visibility)
            root.destroyNativeAd()
        }
    }

    @Test fun `destroy during countdown cannot enable or compact the close`() {
        RuntimeEnvironment.setQualifiers("w360dp-h800dp-port-mdpi")
        val root = view(6)
        root.applyStyles(style())
        root.startCloseCountdown()
        val close = root.findViewById<View>(R.id.tvClose)
        val container = root.findViewById<View>(R.id.controlClose)
        root.destroyNativeAd()
        shadowOf(Looper.getMainLooper()).idleFor(10, TimeUnit.SECONDS)
        // SDK destroy removes children from NativeAdView, so retain the control references.
        assertEquals(48, close.layoutParams.width)
        assertNotNull(container.background)
        assertFalse(close.isEnabled)
        assertFalse(container.isClickable)
    }
}
