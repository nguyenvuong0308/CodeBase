package com.core.ads.customviews.ads

import android.app.Application
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.core.config.domain.data.AdType
import com.core.config.domain.data.BannerSize
import com.core.config.domain.data.NativeAdPlace
import com.core.config.domain.data.NativeExpandTemplate
import com.core.config.domain.data.NativeTemplateSize
import com.core.config.domain.data.RemoteAdPlaceName
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CollapsibleInlineVisibilityTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test
    fun `explicit collapse hides inline without binding ad and leaves anchor visible`() {
        val fixture = Fixture()
        fixture.controller.setExpanded(false)
        fixture.bind(hideInline = true)

        assertEquals(View.GONE, fixture.inline.visibility)
        assertEquals(0, fixture.inline.bindCount)
        assertEquals(View.VISIBLE, fixture.anchor.visibility)
    }

    @Test
    fun `default style keeps collapsed inline visible and bound`() {
        val fixture = Fixture()
        fixture.controller.setExpanded(false)
        fixture.controller.bind(fixture.inline, fixture.ad, NativeTemplateStyle.Builder().build())

        assertEquals(View.VISIBLE, fixture.inline.visibility)
        assertEquals(1, fixture.inline.bindCount)
    }

    @Test
    fun `pending expansion and unavailable anchor fallback both hide inline`() {
        val fixture = Fixture()
        fixture.bind(hideInline = true)
        assertEquals(View.GONE, fixture.inline.visibility)
        assertEquals(1, fixture.anchor.pending.size)

        // Detached anchor cannot show a popup, so the queued request takes the fallback path.
        fixture.anchor.runPending()
        assertEquals(View.GONE, fixture.inline.visibility)
        assertEquals(0, fixture.inline.bindCount)
    }

    @Test
    fun `collapse cancels pending expansion even when inline is already gone`() {
        val fixture = Fixture()
        fixture.bind(hideInline = true)
        fixture.controller.setExpanded(false)

        fixture.anchor.runPending()
        assertEquals(0, fixture.anchor.attachmentChecks)
        assertEquals(View.GONE, fixture.inline.visibility)
        assertEquals(0, fixture.inline.bindCount)
    }

    @Test
    fun `lifecycle collapse and refresh keep inline hidden`() {
        val actions: List<(CollapsibleNativeController) -> Unit> = listOf(
            { it.setExpanded(false) },
            { it.onAnchorHidden() },
            { it.onAnchorDetached() },
            { it.onNativeRefreshStarted() },
        )
        actions.forEach { collapse ->
            val fixture = Fixture()
            fixture.bind(hideInline = true)
            collapse(fixture.controller)
            fixture.anchor.runPending()
            assertEquals(View.GONE, fixture.inline.visibility)
            assertEquals(0, fixture.inline.bindCount)
        }
    }

    @Test
    fun `container forwards flag and restores loading shimmer after hidden inline`() {
        val container = BannerNativeContainerLayout(context)
        val place = mock(NativeAdPlace::class.java)
        `when`(place.placeName).thenReturn(RemoteAdPlaceName("inline_test"))
        `when`(place.nativeTemplateSize).thenReturn(NativeTemplateSize.Small)
        `when`(place.nativeExpandTemplate).thenReturn(NativeExpandTemplate.V1)
        `when`(place.isNativeCollapsible).thenReturn(true)
        `when`(place.alwaysHideInlineNative).thenReturn(true)
        container.setNativeExpanded(false)
        container.onAdLoaded(mock(NativeAd::class.java), place)

        assertTrue(container.getChildAt(0) is BaseNativeTemplateView)
        assertEquals(View.GONE, container.getChildAt(0).visibility)
        assertEquals(View.VISIBLE, container.visibility)

        container.setAdSize(AdType.Native, BannerSize.Anchored, NativeTemplateSize.Small)

        val shimmer = container.getChildAt(0) as PlaceHolderView
        assertEquals(View.VISIBLE, shimmer.visibility)
        assertTrue((shimmer.getChildAt(0) as ShimmerFrameLayout).isShimmerStarted)
        assertEquals(View.VISIBLE, container.visibility)
    }

    private inner class Fixture {
        val anchor = QueuedAnchor(context)
        val inline = RecordingInline(context)
        val ad = mock(NativeAd::class.java)
        val controller = CollapsibleNativeController(anchor, Any(), {})

        fun bind(hideInline: Boolean) {
            controller.bind(inline, ad, NativeTemplateStyle.Builder()
                .withAlwaysHideInlineNative(hideInline).build())
        }
    }

    private class QueuedAnchor(context: Context) : FrameLayout(context) {
        val pending = mutableListOf<Runnable>()
        var attachmentChecks = 0
        override fun isAttachedToWindow(): Boolean {
            attachmentChecks++
            return false
        }
        override fun post(action: Runnable): Boolean {
            pending.add(action)
            return true
        }
        fun runPending() {
            val actions = pending.toList()
            pending.clear()
            actions.forEach(Runnable::run)
        }
    }

    private class RecordingInline(context: Context) : BaseNativeTemplateView(context) {
        var bindCount = 0
        override fun setNativeAd(nativeAd: NativeAd) { bindCount++ }
        override fun applyStyles(styles: NativeTemplateStyle) = Unit
        override fun destroyNativeAd() = Unit
    }
}
