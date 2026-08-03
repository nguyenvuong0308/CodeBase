package com.codebasetemplate.features.feature_onboarding.ui.v2

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.ColorInt

class GradientStrokeDrawable(
    @ColorInt private val startColor: Int,
    @ColorInt private val endColor: Int,
    private val strokeWidth: Float,
    private val cornerRadius: Float
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@GradientStrokeDrawable.strokeWidth
    }

    private val rectF = RectF()

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        rectF.set(
            bounds.left + strokeWidth / 2,
            bounds.top + strokeWidth / 2,
            bounds.right - strokeWidth / 2,
            bounds.bottom - strokeWidth / 2
        )
        paint.shader = LinearGradient(
            rectF.left,
            rectF.top,
            rectF.right,
            rectF.bottom,
            startColor,
            endColor,
            Shader.TileMode.CLAMP
        )
    }

    override fun draw(canvas: Canvas) {
        // Vẽ stroke gradient
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/** Extension tiện dụng để áp dụng vào View */
fun View.setGradientStrokeBackground(
    @ColorInt startColor: Int,
    @ColorInt endColor: Int,
    strokeWidthDp: Float = 3f,
    cornerRadiusDp: Float = 12f
) {
    val density = context.resources.displayMetrics.density
    background = GradientStrokeDrawable(
        startColor = startColor,
        endColor = endColor,
        strokeWidth = strokeWidthDp * density,
        cornerRadius = cornerRadiusDp * density
    )
}
