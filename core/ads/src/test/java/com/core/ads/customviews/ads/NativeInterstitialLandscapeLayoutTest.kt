package com.core.ads.customviews.ads

import android.app.Application
import android.graphics.Rect
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.core.ads.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "w800dp-h360dp-land-mdpi")
class NativeInterstitialLandscapeLayoutTest {
    @Test
    fun `V1 landscape keeps controls visible and reserves a large media area`() = verifyLayout(1)

    @Test
    fun `V2 landscape keeps controls visible and reserves a large media area`() = verifyLayout(2)

    @Test
    fun `title and body use exact configured colors even when contrast is low`() = forEachTemplate { root ->
        for ((title, body) in listOf("#FFFFFF" to "#F5F5F5", "#1F2937" to "#6B7280", "#80ABCDEF" to "#80987654")) {
            root.applyStyles(textStyle("#CD121212", title, body))
            assertConfiguredTextColors(root, title, body)
        }
    }

    @Test
    fun `reapplying styles updates both colors to the latest config`() = forEachTemplate { root ->
        root.applyStyles(textStyle("#121212", "#FFFFFF", "#F5F5F5"))
        assertConfiguredTextColors(root, "#FFFFFF", "#F5F5F5")
        root.applyStyles(textStyle("#FFFFFF", "#ABCDEF", "#FEDCBA"))
        assertConfiguredTextColors(root, "#ABCDEF", "#FEDCBA")
    }

    @Test
    fun `distinct configured title and body colors are not swapped`() = forEachTemplate { root ->
        root.applyStyles(textStyle("#121212", "#FFFF00", "#00FFFF"))
        assertConfiguredTextColors(root, "#FFFF00", "#00FFFF")
    }

    @Test
    fun `individual text backgrounds do not override configured text colors`() = forEachTemplate { root ->
        root.applyStyles(NativeTemplateStyle.Builder()
            .withBackgroundResource(R.drawable.core_bg_native_ads)
            .withBackgroundColor("#121212")
            .withBackgroundFullColor("#FFFFFF")
            .withPrimaryTextTypefaceColor("#ABCDEF")
            .withTertiaryTextTypefaceColor("#FEDCBA")
            .withPrimaryTextBackgroundColor(ColorDrawable(Color.WHITE))
            .withTertiaryTextBackgroundColor(ColorDrawable(Color.WHITE))
            .build())
        assertConfiguredTextColors(root, "#ABCDEF", "#FEDCBA")
    }

    private fun assertConfiguredTextColors(root: BaseNativeTemplateView, title: String, body: String) {
        assertEquals("${root.javaClass.simpleName} title must match config",
            Color.parseColor(title), root.findViewById<TextView>(R.id.primary).currentTextColor)
        assertEquals("${root.javaClass.simpleName} body must match config",
            Color.parseColor(body), root.findViewById<TextView>(R.id.body).currentTextColor)
    }

    private fun textStyle(background: String, title: String, body: String) = NativeTemplateStyle.Builder()
        .withBackgroundResource(R.drawable.core_bg_native_ads)
        .withBackgroundColor(background)
        .withBackgroundFullColor("#FFFFFF")
        .withPrimaryTextTypefaceColor(title)
        .withTertiaryTextTypefaceColor(body)
        .build()

    private fun forEachTemplate(check: (BaseNativeTemplateView) -> Unit) {
        for (qualifiers in listOf("w800dp-h360dp-land-mdpi", "w360dp-h800dp-port-mdpi")) {
            RuntimeEnvironment.setQualifiers(qualifiers)
            val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(),
                com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar)
            check(NativeInterstitialV1View(context))
            check(NativeInterstitialV2View(context))
        }
    }

    private fun verifyLayout(version: Int) {
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar)
        val root = if (version == 1) NativeInterstitialV1View(context) else NativeInterstitialV2View(context)
        root.applyStyles(NativeTemplateStyle.Builder().build())
        root.findViewById<TextView>(R.id.primary).text = "Test Ad: A longer application title"
        root.findViewById<TextView>(R.id.body).text =
            "Discover the application and all of its features. Install it for free and get started today."
        root.findViewById<TextView>(R.id.cta).text = "CÀI ĐẶT"
        root.findViewById<View>(R.id.lnCountDown).visibility = View.INVISIBLE
        root.findViewById<View>(R.id.tvClose).visibility = View.VISIBLE

        for ((width, height) in listOf(800 to 360, 640 to 320)) {
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            root.layout(0, 0, width, height)
            listOf(R.id.cta, R.id.primary, R.id.body, R.id.tvClose, R.id.media_view).forEach { id ->
                val child = root.findViewById<View>(id)
                val bounds = boundsIn(root, child)
                assertTrue("V$version $width x $height: $id has no area", bounds.width() > 0 && bounds.height() > 0)
                var parent = child.parent as ViewGroup
                while (parent !== root) {
                    assertTrue("V$version $width x $height: $id clipped by ${parent.id}",
                        boundsIn(root, parent).contains(bounds))
                    parent = parent.parent as ViewGroup
                }
                assertTrue("V$version $width x $height: $id outside viewport", Rect(0, 0, width, height).contains(bounds))
            }
            val media = boundsIn(root, root.findViewById(R.id.media_view))
            assertTrue("Media must use at least 45% of landscape width", media.width() >= width * 0.45f)
            assertTrue("Media must use at least 65% of landscape height", media.height() >= height * 0.65f)
            assertFalse("Media and content must not overlap",
                Rect.intersects(media, boundsIn(root, root.findViewById(R.id.background))))
            assertFalse("Close and CTA must not overlap",
                Rect.intersects(boundsIn(root, root.findViewById(R.id.tvClose)), boundsIn(root, root.findViewById(R.id.cta))))
        }
    }

    private fun boundsIn(root: ViewGroup, child: View) = Rect().also {
        child.getDrawingRect(it)
        root.offsetDescendantRectToMyCoords(child, it)
    }
}
