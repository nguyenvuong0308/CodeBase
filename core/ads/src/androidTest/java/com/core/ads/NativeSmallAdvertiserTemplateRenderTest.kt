package com.core.ads

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders the small advertiser templates with sample assets so their layout can be checked on a
 * device without waiting for a live AdMob fill. Images are written to the app external files dir.
 */
@RunWith(AndroidJUnit4::class)
class NativeSmallAdvertiserTemplateRenderTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun renderSmallAdvertiserCtaBottom() {
        render(R.layout.gnt_small_advertiser_cta_bottom_template_view, "small_advertiser_cta_bottom")
    }

    @Test
    fun renderSmallAdvertiserCtaTop() {
        render(R.layout.gnt_small_advertiser_cta_top_template_view, "small_advertiser_cta_top")
    }

    private fun render(layoutRes: Int, name: String) {
        val container = FrameLayout(context).apply { setBackgroundColor(Color.WHITE) }
        LayoutInflater.from(context).inflate(layoutRes, container, true)

        container.findViewById<ImageView>(R.id.icon).setImageDrawable(ColorDrawable(Color.LTGRAY))
        container.findViewById<TextView>(R.id.primary).text = "Sollicitudin diam eleifend"
        container.findViewById<TextView>(R.id.advertiser).text = "RES Tuyển dụng"
        container.findViewById<TextView>(R.id.body).text =
            "Risus nunc aliquet tempus arcu et convallis ut nullam nullam nullam nullam"
        container.findViewById<TextView>(R.id.cta).text = "Sign up"

        val width = (360 * context.resources.displayMetrics.density).toInt()
        container.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        container.layout(0, 0, container.measuredWidth, container.measuredHeight)
        assertTrue("$name must have a visible height", container.measuredHeight > 0)

        val icon = container.findViewById<View>(R.id.icon)
        val cta = container.findViewById<View>(R.id.layout_cta)
        val body = container.findViewById<View>(R.id.body)
        if (name.endsWith("_top")) {
            assertTrue("CTA must be above the icon", cta.bottom <= icon.top)
        } else {
            assertTrue("CTA must be below the body", cta.top >= body.bottom)
        }

        val bitmap = Bitmap.createBitmap(
            container.measuredWidth,
            container.measuredHeight,
            Bitmap.Config.ARGB_8888,
        )
        container.draw(Canvas(bitmap))
        val output = File(context.getExternalFilesDir(null), "native-render/$name.png")
        output.parentFile?.mkdirs()
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(output.length() > 0)
    }
}
