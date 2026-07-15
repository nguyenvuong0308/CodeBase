package com.core.baseui.toolbar

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.withStyledAttributes
import com.core.baseui.R

class RippleImageView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private var imageView: AppCompatImageView? = null
    private var rippleDrawable: RippleDrawable? = null

    var paddingRipple: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, context.resources.displayMetrics)
        set(value) {
            field = value
            val p = value.toInt()
            setPadding(p, p, p, p)
            rebuildRipple()
        }

    /** Resource icon */
    var iconRippleRes: Int = 0
        set(value) {
            field = value
            imageView?.setImageResource(value)
        }

    /** Màu ripple tùy chọn; null = dùng màu theme (colorControlHighlight) */
    var rippleColor: Int? = null
        set(value) {
            field = value
            rebuildRipple()
        }

    /** true = ripple tròn (đường kính = min(w,h)); false = oval phủ full bounds */
    var circleRipple: Boolean = true
        set(value) {
            field = value
            rebuildRipple()
        }

    var applyScaleX: Boolean = true
        set(value) {
            field = value
            updateIconScaleX()
        }

    init {
        isClickable = true
        isFocusable = true
        foregroundGravity = Gravity.FILL // để foreground phủ full bounds

        if (imageView == null) {
            imageView = AppCompatImageView(context).also { iv ->
                val lp = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
                ).apply { gravity = Gravity.CENTER }
                addView(iv, lp)
            }
            updateIconScaleX()
        }

        context.withStyledAttributes(attrs, R.styleable.RippleImageView) {
            paddingRipple = getDimension(R.styleable.RippleImageView_riv_padding_ripple, paddingRipple)
            iconRippleRes = getResourceId(R.styleable.RippleImageView_riv_icon_ripple, iconRippleRes)
            applyScaleX = getBoolean(R.styleable.RippleImageView_riv_apply_scale_x, applyScaleX)
            if (hasValue(R.styleable.RippleImageView_riv_ripple_color)) {
                rippleColor = getColor(R.styleable.RippleImageView_riv_ripple_color, 0)
            }
            if (hasValue(R.styleable.RippleImageView_riv_circle_ripple)) {
                circleRipple = getBoolean(R.styleable.RippleImageView_riv_circle_ripple, true)
            }
        }

        if (iconRippleRes != 0) imageView?.setImageResource(iconRippleRes)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildRipple()
    }

    private fun rebuildRipple() {
        if (width <= 0 || height <= 0) return

        val w = width
        val h = height

        // mask oval cơ bản
        val baseOval = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.WHITE) // chỉ dùng alpha
        }

        // căn giữa ripple theo toàn view (không trừ padding)
        val maskDrawable = if (circleRipple) {
            val d = minOf(w, h)
            val left = (w - d) / 2
            val top = (h - d) / 2
            val right = w - (left + d)
            val bottom = h - (top + d)
            InsetDrawable(baseOval, left, top, right, bottom)
        } else {
            baseOval
        }

        // màu ripple
        val tv = TypedValue()
        val color = rippleColor?.let {
            ColorStateList.valueOf(it)
        } ?: run {
            context.theme.resolveAttribute(android.R.attr.colorControlHighlight, tv, true)
            ColorStateList.valueOf(tv.data)
        }

        // tạo ripple + hotspot căn giữa
        val rd = RippleDrawable(color, null, maskDrawable)
        if (circleRipple) {
            val d = minOf(w, h)
            val left = (w - d) / 2
            val top = (h - d) / 2
            rd.setHotspotBounds(left, top, left + d, top + d)
        } else {
            rd.setHotspotBounds(0, 0, w, h)
        }

        rippleDrawable = rd
        foreground = rd
    }

    private fun updateIconScaleX() {
        imageView?.scaleX = if (applyScaleX) {
            resources.getInteger(R.integer.locale_mirror_flip).toFloat()
        } else {
            1f
        }
    }

    override fun setEnabled(enabled: Boolean) {
        imageView?.isEnabled = enabled
        super.setEnabled(enabled)
    }
}