package com.codebasetemplate.features.feature_language.ui.v2.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.codebasetemplate.R

class LanguageBranchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var isLast: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.language_v2_branch)
        strokeWidth = resources.displayMetrics.density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val path = Path()

    var cardCenterY: Float = 36f * resources.displayMetrics.density
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val startX = 16f * resources.displayMetrics.density
        val centerY = cardCenterY.coerceIn(0f, height.toFloat())
        val endX = if (layoutDirection == LAYOUT_DIRECTION_RTL) 0f else width.toFloat()
        val cornerEndX = if (layoutDirection == LAYOUT_DIRECTION_RTL) {
            startX - 16f * resources.displayMetrics.density
        } else {
            startX + 16f * resources.displayMetrics.density
        }
        path.reset()
        path.moveTo(startX, 0f)
        if (isLast) {
            path.lineTo(startX, centerY - 16f * resources.displayMetrics.density)
            path.quadTo(startX, centerY, cornerEndX, centerY)
            path.lineTo(endX, centerY)
        } else {
            path.lineTo(startX, height.toFloat())
            path.moveTo(startX, centerY)
            path.lineTo(endX, centerY)
        }
        canvas.drawPath(path, paint)
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        super.onRtlPropertiesChanged(layoutDirection)
        invalidate()
    }
}
