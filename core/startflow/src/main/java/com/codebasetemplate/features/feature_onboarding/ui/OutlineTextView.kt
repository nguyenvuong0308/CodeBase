package com.codebasetemplate.features.feature_onboarding.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Region
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withSave
import com.core.startflow.R
import kotlin.math.cos
import kotlin.math.sin

class OutlineTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    private val mOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        isDither = true
    }
    private val mOutlinePath = Path()
    private val mTempOutlinePath = Path()

    private var mStrokeWidth = 0f
    private var mStrokeColor = Color.RED

    /** Gradient cho FILL (phần chữ) — giữ nguyên logic cũ */
    private var fillGradient: LinearGradient? = null
    private var fillGradientColors: IntArray? = intArrayOf(
        "#fb03fb".toColorInt(),
        "#0bdaff".toColorInt()
    )
    private var isFillGradient: Boolean = false

    /** Gradient cho STROKE (viền) — mới thêm */
    private var strokeGradient: Shader? = null
    private var strokeGradientColors: List<Int>? = null
    private var strokeGradientOrientation: GradientOrientation = GradientOrientation.HORIZONTAL
    private var isStrokeGradientEnabled: Boolean = false

    enum class GradientOrientation {
        HORIZONTAL, VERTICAL, DIAGONAL_TL_BR, DIAGONAL_TR_BL, ANGLE // ANGLE dùng với setStrokeGradientAngle
    }
    private var strokeAngleDeg: Float = 0f // dùng khi orientation = ANGLE

    init {
        context.withStyledAttributes(attrs, R.styleable.OutlineTextView) {
            mStrokeWidth = getDimension(R.styleable.OutlineTextView_OutlineTextView_stroke_size, 0f)
            mStrokeColor = getColor(R.styleable.OutlineTextView_OutlineTextView_stroke_color, Color.RED)
            isFillGradient = getBoolean(R.styleable.OutlineTextView_OutlineTextView_gradient, false)
        }

        mOutlinePaint.strokeWidth = mStrokeWidth
        mOutlinePaint.color = mStrokeColor

        // Nếu muốn default stroke gradient (tùy bạn): comment/giữ lại
        // enableStrokeGradient(intArrayOf("#FF7AF5".toColorInt(), "#7AE1FF".toColorInt()))
    }

    /* ===================== lifecycle layout ===================== */

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // FILL gradient theo view width (nếu dùng)
        if (isFillGradient && w > 0 && fillGradientColors != null) {
            fillGradient = LinearGradient(
                0f, 0f, w.toFloat(), 0f,
                fillGradientColors!!, null, Shader.TileMode.CLAMP
            )
            paint.shader = fillGradient
        } else {
            paint.shader = null
        }
        // Stroke gradient sẽ cập nhật theo bounds của path trong updateOutlineShader()
    }



    fun invalidAuto() {
        updateOutlinePath()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        updateOutlinePath()
    }

    private fun updateOutlinePath() {
        mOutlinePath.reset()
        mTempOutlinePath.reset()
        if (layout == null || text.isNullOrEmpty()) {
            invalidate()
            return
        }

        for (i in 0 until lineCount) {
            val start = layout.getLineStart(i)
            val end = layout.getLineEnd(i)
            var textLine = text.subSequence(start, end).toString()

            // Ellipsize cho dòng cuối
            if (i == maxLines - 1 && layout.ellipsizedWidth > 0 && layout.getEllipsisCount(i) > 0) {
                val ellipsisStart = layout.getEllipsisStart(i)
                textLine = textLine.substring(0, ellipsisStart) + "…"
            }

            val xOffset = layout.getLineLeft(i) + paddingLeft
            val baseline = layout.getLineBaseline(i) + paddingTop
            paint.getTextPath(textLine, 0, textLine.length, xOffset, baseline.toFloat(), mTempOutlinePath)
            mOutlinePath.addPath(mTempOutlinePath)
        }

        updateOutlineShader() // cập nhật shader viền theo bounds path
        invalidate()
    }

    /* ===================== drawing ===================== */

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (mStrokeWidth > 0) {
            canvas.withSave {
                // Chặn phần stroke “đè” vào trong chữ (giữ style viền ngoài)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    @Suppress("DEPRECATION")
                    clipPath(mOutlinePath, Region.Op.DIFFERENCE)
                } else {
                    clipOutPath(mOutlinePath)
                }
                drawPath(mOutlinePath, mOutlinePaint)
            }
        }
    }

    /* ===================== public API ===================== */

    fun setStrokeColor(color: Int) {
        mStrokeColor = color
        if (!isStrokeGradientEnabled) {
            mOutlinePaint.shader = null
            mOutlinePaint.color = mStrokeColor
        }
        invalidate()
    }

    fun setStrokeWidth(strokeWidth: Float) {
        mStrokeWidth = strokeWidth
        mOutlinePaint.strokeWidth = strokeWidth
        invalidate()
    }

    /** Bật gradient cho viền với màu & hướng cơ bản */
    fun setStrokeGradient(colors: List<Int>, orientation: GradientOrientation = GradientOrientation.HORIZONTAL) {
        strokeGradientColors = colors
        strokeGradientOrientation = orientation
        isStrokeGradientEnabled = true
        updateOutlineShader()
        invalidate()
    }

    /** Bật gradient viền với góc tùy ý (0° = trái→phải, 90° = trên→dưới, v.v.) */
    fun setStrokeGradientAngle(colors: List<Int>, angleDegrees: Float) {
        strokeGradientColors = colors
        strokeGradientOrientation = GradientOrientation.ANGLE
        strokeAngleDeg = angleDegrees
        isStrokeGradientEnabled = true
        updateOutlineShader()
        invalidate()
    }

    /** Tắt gradient viền → quay về màu đơn */
    fun clearStrokeGradient() {
        isStrokeGradientEnabled = false
        strokeGradientColors = null
        strokeGradient = null
        mOutlinePaint.shader = null
        mOutlinePaint.color = mStrokeColor
        invalidate()
    }

    /** Bật/tắt gradient fill (phần chữ) nếu bạn muốn switch runtime */
    fun setFillGradientEnabled(enabled: Boolean, colors: IntArray? = null) {
        isFillGradient = enabled
        fillGradientColors = colors ?: fillGradientColors
        if (isFillGradient && width > 0 && fillGradientColors != null) {
            fillGradient = LinearGradient(
                0f, 0f, width.toFloat(), 0f,
                fillGradientColors!!, null, Shader.TileMode.CLAMP
            )
            paint.shader = fillGradient
        } else {
            paint.shader = null
        }
        invalidate()
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        requestLayout()
    }

    /* ===================== helpers ===================== */

    private fun updateOutlineShader() {
        if (!isStrokeGradientEnabled || strokeGradientColors.isNullOrEmpty()) {
            mOutlinePaint.shader = null
            mOutlinePaint.color = mStrokeColor
            return
        }

        // Lấy bounds của path chữ để đặt gradient khít chữ
        val r = RectF()
        mOutlinePath.computeBounds(r, true)
        if (r.width() <= 0f || r.height() <= 0f) {
            mOutlinePaint.shader = null
            return
        }

        val (x0, y0, x1, y1) = when (strokeGradientOrientation) {
            GradientOrientation.HORIZONTAL     -> floatArrayOf(r.left, r.centerY(), r.right, r.centerY())
            GradientOrientation.VERTICAL       -> floatArrayOf(r.centerX(), r.top, r.centerX(), r.bottom)
            GradientOrientation.DIAGONAL_TL_BR -> floatArrayOf(r.left, r.top, r.right, r.bottom)
            GradientOrientation.DIAGONAL_TR_BL -> floatArrayOf(r.right, r.top, r.left, r.bottom)
            GradientOrientation.ANGLE -> {
                // Tính theo góc: tâm rect + vector theo góc
                val cx = r.centerX()
                val cy = r.centerY()
                val rad = Math.toRadians(strokeAngleDeg.toDouble())
                val dx = (r.width() / 2f) * cos(rad).toFloat()
                val dy = (r.height() / 2f) * sin(rad).toFloat()
                floatArrayOf(cx - dx, cy - dy, cx + dx, cy + dy)
            }
        }

        strokeGradient = LinearGradient(
            x0, y0, x1, y1,
            strokeGradientColors!!.toIntArray(),
            null,
            Shader.TileMode.CLAMP
        )
        mOutlinePaint.shader = strokeGradient
        // Khi có shader, color base không dùng nữa
    }
}
