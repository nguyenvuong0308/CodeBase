package com.core.ads.customviews.ads

import android.app.Activity
import android.app.Application
import android.view.ContextThemeWrapper
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.core.ads.R
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import com.core.dimens.R as DimenR

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class NativePictureInPictureShuffleTest {
    @Test
    fun `show overloads use the selected layout consistently for binding and window dimensions`() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup().visible()
        val view = NativePictureInPicture(ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar))
        val config = NativePictureInPicture.Config(
            layoutFormat = NativePictureInPicture.LayoutFormat.Shuffle,
            closeCountDownSeconds = 0,
        )
        val ad = mock(NativeAd::class.java)
        `when`(ad.headline).thenReturn("Shuffle ad")
        `when`(ad.callToAction).thenReturn("Open")
        try {
            repeat(6) { index ->
                val shown = if (index % 2 == 0) {
                    view.show(controller.get(), ad, config = config)
                } else {
                    view.show(controller.get(), config)
                }
                assertTrue(shown)
                val resolved = field(view, "currentConfig") as NativePictureInPicture.Config
                assertNotEquals(NativePictureInPicture.LayoutFormat.Shuffle, resolved.layoutFormat)
                assertEquals(resolved.layoutFormat, field(view, "displayedLayoutFormat"))
                assertEquals(config.copy(layoutFormat = resolved.layoutFormat), resolved)
                val params = view.layoutParams as WindowManager.LayoutParams
                val resources = view.resources
                val expectedWidth = when (resolved.layoutFormat) {
                    NativePictureInPicture.LayoutFormat.Compact -> resources.getDimensionPixelSize(DimenR.dimen._180dp)
                    NativePictureInPicture.LayoutFormat.MediaCard -> resources.getDimensionPixelSize(DimenR.dimen._208dp)
                    NativePictureInPicture.LayoutFormat.Banner -> resources.getDimensionPixelSize(DimenR.dimen._320dp)
                    else -> error("Shuffle was not resolved")
                }
                val expectedHeight = when (resolved.layoutFormat) {
                    NativePictureInPicture.LayoutFormat.Compact -> expectedWidth
                    NativePictureInPicture.LayoutFormat.Banner -> resources.getDimensionPixelSize(DimenR.dimen._56dp)
                    NativePictureInPicture.LayoutFormat.MediaCard -> maxOf(expectedWidth,
                        resources.getDimensionPixelSize(R.dimen.native_picture_in_picture_media_min_size) +
                            resources.getDimensionPixelSize(DimenR.dimen._42dp) +
                            resources.getDimensionPixelSize(DimenR.dimen._40dp) +
                            resources.getDimensionPixelSize(DimenR.dimen._3dp) +
                            2 * resources.getDimensionPixelSize(DimenR.dimen._1_5dp))
                    else -> error("Shuffle was not resolved")
                }
                assertEquals(expectedWidth, params.width)
                assertEquals(expectedHeight, params.height)
                assertEquals("Shuffle ad", view.findViewById<TextView>(R.id.primary).text.toString())
                val cta = view.findViewById<View>(R.id.cta)
                assertSame(cta, view.findViewById<NativeAdView>(R.id.native_ad_view).callToActionView)
                assertEquals(resolved.layoutFormat == NativePictureInPicture.LayoutFormat.Banner,
                    cta is ImageView)
                assertEquals(resolved.layoutFormat == NativePictureInPicture.LayoutFormat.MediaCard,
                    view.findViewById<View>(R.id.media_view) != null)
            }
        } finally {
            view.dismiss()
            controller.pause().stop().destroy()
        }
    }

    private fun field(view: NativePictureInPicture, name: String): Any? =
        NativePictureInPicture::class.java.getDeclaredField(name).apply {
            isAccessible = true
        }.get(view)
}
