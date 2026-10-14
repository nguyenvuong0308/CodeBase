package com.core.ads.customviews.ads

import android.app.Application
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.PopupWindow
import com.core.config.domain.data.NativeAdPlace
import com.core.config.domain.data.NativeExpandTemplate
import com.core.config.domain.data.NativeTemplateSize
import com.core.config.domain.data.RemoteAdPlaceName
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "xhdpi")
class CollapsiblePopupMarginTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test
    fun `asymmetric margins inset template and close button in LTR and RTL`() {
        for (direction in listOf(View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL)) {
            for (closePosition in listOf("left", "right")) {
                val controller = controller(NativeTemplateStyle.Builder()
                    .withNativeExpandMargins(12.5f, 24f)
                    .withControlClosePosition(closePosition).build())
                val content = createContent(controller)
                content.layoutDirection = direction
                layout(content)

                // xhdpi converts 12.5dp / 24dp to 25px / 48px.
                val template = content.getChildAt(0)
                val close = content.getChildAt(1)
                assertEquals(25, template.left)
                assertEquals(752, template.right)
                assertTrue(close.left >= template.left)
                assertTrue(close.right <= template.right)
                assertEquals(0, content.paddingTop)
                assertEquals(0, content.paddingBottom)
            }
        }
    }

    @Test
    fun `default and invalid margins preserve full width`() {
        val styles = listOf(
            NativeTemplateStyle.Builder().build(),
            NativeTemplateStyle.Builder().withNativeExpandMargins(-10f, -20f).build(),
            NativeTemplateStyle.Builder().withNativeExpandMargins(Float.NaN, Float.POSITIVE_INFINITY).build(),
        )
        styles.forEach { style ->
            val content = createContent(controller(style))
            layout(content)
            assertEquals(0, content.getChildAt(0).left)
            assertEquals(800, content.getChildAt(0).right)
        }
    }

    @Test
    fun `rebinding updates margins of an existing popup`() {
        val ad = mock(NativeAd::class.java)
        val anchor = FrameLayout(context)
        val controller = CollapsibleNativeController(anchor, Any(), {})
        controller.setExpanded(false)
        controller.bind(EmptyTemplate(context), ad, NativeTemplateStyle.Builder().build())
        val content = createContent(controller)
        val popup = mock(PopupWindow::class.java)
        `when`(popup.contentView).thenReturn(content)
        `when`(popup.isShowing).thenReturn(true)
        CollapsibleNativeController::class.java.getDeclaredField("popupWindow").apply {
            isAccessible = true
            set(controller, popup)
        }

        controller.bind(EmptyTemplate(context), ad,
            NativeTemplateStyle.Builder().withNativeExpandMargins(10f, 30f).build())
        layout(content)

        assertEquals(20, content.getChildAt(0).left)
        assertEquals(740, content.getChildAt(0).right)
        verify(popup).update(eq(anchor), eq(0), anyInt(), eq(-1), eq(-1))
    }

    @Test
    fun `container forwards placement margins for both expanded templates without insetting inline`() {
        for (expandTemplate in listOf(NativeExpandTemplate.V1, NativeExpandTemplate.V2)) {
            val container = BannerNativeContainerLayout(context)
            val place = mock(NativeAdPlace::class.java)
            `when`(place.placeName).thenReturn(RemoteAdPlaceName("margin_test"))
            `when`(place.nativeTemplateSize).thenReturn(NativeTemplateSize.Small)
            `when`(place.nativeExpandTemplate).thenReturn(expandTemplate)
            `when`(place.isNativeCollapsible).thenReturn(true)
            `when`(place.alwaysHideInlineNative).thenReturn(true)
            `when`(place.nativeExpandMarginLeftDp).thenReturn(8f)
            `when`(place.nativeExpandMarginRightDp).thenReturn(18f)
            container.setNativeExpanded(false)
            container.onAdLoaded(mock(NativeAd::class.java), place)
            val controller = BannerNativeContainerLayout::class.java
                .getDeclaredField("collapsibleNativeControllerOrNull").run {
                    isAccessible = true
                    get(container) as CollapsibleNativeController
                }
            val content = createContent(controller)
            layout(content)

            assertEquals(16, content.getChildAt(0).left)
            assertEquals(764, content.getChildAt(0).right)
            assertEquals(0, container.paddingLeft)
            assertEquals(0, container.paddingRight)
            assertEquals(View.GONE, container.getChildAt(0).visibility)
        }
    }

    private fun controller(styles: NativeTemplateStyle): CollapsibleNativeController {
        return CollapsibleNativeController(FrameLayout(context), Any(), {}).apply {
            setExpanded(false)
            bind(EmptyTemplate(context), mock(NativeAd::class.java), styles)
        }
    }

    // Exercise the controller's real popup layout without requiring a live ad SDK render.
    private fun createContent(controller: CollapsibleNativeController): FrameLayout =
        CollapsibleNativeController::class.java
            .getDeclaredMethod("createPopupContent", BaseNativeTemplateView::class.java).run {
                isAccessible = true
                invoke(controller, EmptyTemplate(context)) as FrameLayout
            }

    private fun layout(view: View) {
        view.measure(View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }

    private class EmptyTemplate(context: Context) : BaseNativeTemplateView(context) {
        override fun setNativeAd(nativeAd: NativeAd) = Unit
        override fun applyStyles(styles: NativeTemplateStyle) = Unit
        override fun destroyNativeAd() = Unit
    }
}
