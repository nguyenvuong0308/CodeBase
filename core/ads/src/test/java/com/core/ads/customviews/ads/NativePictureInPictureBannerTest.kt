package com.core.ads.customviews.ads

import android.app.Application
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.core.ads.R
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.core.dimens.R as DimenR

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "vi-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NativePictureInPictureBannerTest {
    private fun createView(): NativePictureInPicture = NativePictureInPicture(
        ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar),
    )

    @Test
    fun `banner binds icon CTA with accessible ad action and handles missing optional assets`() {
        val view = createView()
        selectLayout(view, NativePictureInPicture.LayoutFormat.Banner)
        val ad = mock(NativeAd::class.java)
        `when`(ad.headline).thenReturn("TikTok")
        `when`(ad.callToAction).thenReturn("Install")
        view.setNativeAd(ad)

        val cta = view.findViewById<ImageView>(R.id.cta)
        val nativeView = view.findViewById<NativeAdView>(R.id.native_ad_view)
        assertSame(cta, nativeView.callToActionView)
        assertSame(view.findViewById(R.id.primary), nativeView.headlineView)
        assertEquals("Install", cta.contentDescription)
        assertEquals("TikTok", view.findViewById<TextView>(R.id.primary).text.toString())
        assertEquals(View.GONE, view.findViewById<ImageView>(R.id.icon).visibility)
        assertNull(nativeView.bodyView)
        assertNull(nativeView.advertiserView)
        assertNotNull(cta.drawable)

        view.setNativeAd(mock(NativeAd::class.java))
        assertEquals("", cta.contentDescription)
        assertEquals("", view.findViewById<TextView>(R.id.primary).text.toString())
    }

    @Test
    fun `switching layouts preserves ad text and supports styling both CTA types`() {
        val view = createView()
        val ad = mock(NativeAd::class.java)
        `when`(ad.headline).thenReturn("Example app")
        `when`(ad.callToAction).thenReturn("Open")
        view.setNativeAd(ad)
        view.applyStyles(NativeTemplateStyle.Builder()
            .withCallToActionTextTypeface(Typeface.DEFAULT_BOLD)
            .withCallToActionTypefaceColor("#FFFFFF")
            .withCallToActionTextSize(14f)
            .build())

        for (format in listOf(NativePictureInPicture.LayoutFormat.Banner,
            NativePictureInPicture.LayoutFormat.MediaCard,
            NativePictureInPicture.LayoutFormat.Compact)) {
            selectLayout(view, format)
            assertEquals(1, view.childCount)
            assertEquals("Example app", view.findViewById<TextView>(R.id.primary).text.toString())
            val cta = view.findViewById<View>(R.id.cta)
            assertSame(cta, view.findViewById<NativeAdView>(R.id.native_ad_view).callToActionView)
            if (format == NativePictureInPicture.LayoutFormat.Banner) {
                assertTrue(cta is ImageView)
                assertNotNull((cta as ImageView).colorFilter)
            } else {
                assertEquals("Open", (cta as TextView).text.toString())
                assertEquals(Color.WHITE, cta.currentTextColor)
            }
        }
    }

    @Test
    fun `banner fits headline icon CTA and separate close control within its bounds`() {
        val view = createView()
        selectLayout(view, NativePictureInPicture.LayoutFormat.Banner)
        val icon = view.findViewById<ImageView>(R.id.icon)
        icon.visibility = View.VISIBLE
        view.findViewById<TextView>(R.id.primary).text = "A long headline that must stay on one line"
        val width = view.resources.getDimensionPixelSize(DimenR.dimen._320dp)
        val height = view.resources.getDimensionPixelSize(DimenR.dimen._56dp)
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)

        val title = view.findViewById<TextView>(R.id.primary)
        val cta = view.findViewById<ImageView>(R.id.cta)
        assertTrue(icon.right <= title.left)
        assertTrue(title.right <= cta.left)
        assertTrue(cta.right <= width)
        assertTrue(cta.bottom <= height)
        assertTrue(title.width > 0)
        assertEquals(1, title.maxLines)
        assertEquals(width, view.findViewById<View>(R.id.background).width)
        val badge = view.findViewById<TextView>(R.id.ad_notification_view)
        val close = view.findViewById<View>(R.id.close_button_container)
        assertEquals("Quảng cáo", badge.text.toString())
        val closeBounds = Rect(0, 0, close.width, close.height)
        val badgeBounds = Rect(0, 0, badge.width, badge.height)
        view.offsetDescendantRectToMyCoords(close, closeBounds)
        view.offsetDescendantRectToMyCoords(badge, badgeBounds)
        assertTrue("Close $closeBounds must sit above badge $badgeBounds",
            closeBounds.bottom <= badgeBounds.top)
        assertFalse(close.parent is NativeAdView)
        var closed = false
        view.onClose = { closed = true }
        close.performClick()
        assertTrue(closed)
    }

    private fun selectLayout(view: NativePictureInPicture, format: NativePictureInPicture.LayoutFormat) {
        NativePictureInPicture::class.java.getDeclaredMethod(
            "applyLayoutFormat", NativePictureInPicture.LayoutFormat::class.java,
        ).apply { isAccessible = true }.invoke(view, format)
    }
}
