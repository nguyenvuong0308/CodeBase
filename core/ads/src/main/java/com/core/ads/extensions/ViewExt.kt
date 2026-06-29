package com.core.ads.extensions

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ScaleDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewOutlineProvider
import androidx.core.graphics.toColorInt

fun View.updateRadius(radiusDp: Float) {
    val bg = background ?: return
    val radiusPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, radiusDp, resources.displayMetrics)

    when (bg) {
        is GradientDrawable -> {
            bg.mutate()
            bg.cornerRadius = radiusPx
        }

        is StateListDrawable -> {
            bg.mutate()

            // 1) Thử “public-ish” API bằng reflection getStateCount/getStateDrawable
            val okByMethods = try {
                val getCount = StateListDrawable::class.java.getMethod("getStateCount")
                val getDrawable = StateListDrawable::class.java.getMethod("getStateDrawable", Int::class.javaPrimitiveType)
                val count = getCount.invoke(bg) as Int
                for (i in 0 until count) {
                    (getDrawable.invoke(bg, i) as? Drawable)?.let { d ->
                        setRadiusDeep(d, radiusPx)
                    }
                }
                true
            } catch (_: Throwable) {
                false
            }

            // 2) Fallback: chọc trực tiếp vào StateListState.mDrawables (không cần cast DCState)
            if (!okByMethods) {
                try {
                    val stateObj = bg.constantState ?: return
                    val field = stateObj.javaClass.getDeclaredField("mDrawables")
                    field.isAccessible = true
                    val arr = field.get(stateObj) as? Array<Drawable> ?: return
                    arr.forEach { setRadiusDeep(it, radiusPx) }
                } catch (_: Throwable) {
                    // Không làm gì để tránh crash; có thể log nếu muốn
                }
            }

            // Cập nhật outline để clip nội dung theo bo góc của background hiện tại
            if (Build.VERSION.SDK_INT >= 21) {
                outlineProvider = ViewOutlineProvider.BACKGROUND
                clipToOutline = true
            }
            // Yêu cầu vẽ lại
            invalidate()
        }
    }
}

/**
 * Đặt radius “đệ quy” cho nhiều kiểu drawable có thể lồng nhau.
 */
private fun setRadiusDeep(d: Drawable, radiusPx: Float) {
    when (d) {
        is GradientDrawable -> {
            d.mutate()
            d.cornerRadius = radiusPx
        }

        is InsetDrawable -> {
            d.drawable?.let { setRadiusDeep(it, radiusPx) }
        }

        is LayerDrawable -> {
            for (i in 0 until d.numberOfLayers) {
                setRadiusDeep(d.getDrawable(i), radiusPx)
            }
        }

        is ScaleDrawable -> {
            d.drawable?.let { setRadiusDeep(it, radiusPx) }
        }

        else -> {
            // Các loại khác (ColorDrawable, BitmapDrawable, Ripple…): bỏ qua
        }
    }
}


fun View.updateBackgroundColor(colorString: String?) {
    try {
        colorString?.let {
            val split = colorString.split(",")
            if(split.size == 2) {
                val startColor = split[0].toColorInt()
                val endColor = split[1].toColorInt()
                val bg = (background as? GradientDrawable)?.mutate() as? GradientDrawable ?: GradientDrawable()
                bg.orientation = GradientDrawable.Orientation.LEFT_RIGHT
                bg.colors = intArrayOf(startColor, endColor)
                background = bg
                invalidate()
            } else {
                if (colorString.isEmpty()) return
                val colorInt = try {
                    colorString.toColorInt()
                } catch (_: Throwable) {
                    return
                }

                val bg = background ?: return

                when (bg) {
                    is GradientDrawable -> {
                        bg.mutate()
                        bg.setColor(colorInt)
                    }

                    is StateListDrawable -> {
                        bg.mutate()

                        // 1) Dùng reflection truy cập danh sách state drawables
                        val okByMethods = try {
                            val getCount = StateListDrawable::class.java.getMethod("getStateCount")
                            val getDrawable = StateListDrawable::class.java.getMethod("getStateDrawable", Int::class.javaPrimitiveType)
                            val count = getCount.invoke(bg) as Int
                            for (i in 0 until count) {
                                (getDrawable.invoke(bg, i) as? Drawable)?.let { d ->
                                    setColorDeep(d, colorInt)
                                }
                            }
                            true
                        } catch (_: Throwable) {
                            false
                        }

                        // 2) Fallback nếu cách trên không hoạt động
                        if (!okByMethods) {
                            try {
                                val stateObj = bg.constantState ?: return
                                val field = stateObj.javaClass.getDeclaredField("mDrawables")
                                field.isAccessible = true
                                val arr = field.get(stateObj) as? Array<Drawable> ?: return
                                arr.forEach { setColorDeep(it, colorInt) }
                            } catch (_: Throwable) {
                                // Bỏ qua nếu lỗi, tránh crash
                            }
                        }

                        invalidate()
                    }

                    else -> {
                        setColorDeep(bg, colorInt)
                        invalidate()
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}


fun View.updateBackgroundColorBottomToTop(colorString: String?) {
    try {
        colorString?.let {
            val split = colorString.split(",")
            if(split.size == 2) {
                val startColor = split[0].trim().toColorInt()
                val endColor = split[1].trim().toColorInt()
                val bg = (background as? GradientDrawable)?.mutate() as? GradientDrawable ?: GradientDrawable()
                bg.orientation = GradientDrawable.Orientation.BOTTOM_TOP
                bg.colors = intArrayOf(startColor, endColor)
                background = bg
                invalidate()
            } else {
                if (colorString.isEmpty()) return
                val colorInt = try {
                    colorString.trim().toColorInt()
                } catch (_: Throwable) {
                    return
                }

                val bg = background ?: return

                when (bg) {
                    is GradientDrawable -> {
                        bg.mutate()
                        bg.setColor(colorInt)
                    }

                    is StateListDrawable -> {
                        bg.mutate()

                        // 1) Dùng reflection truy cập danh sách state drawables
                        val okByMethods = try {
                            val getCount = StateListDrawable::class.java.getMethod("getStateCount")
                            val getDrawable = StateListDrawable::class.java.getMethod("getStateDrawable", Int::class.javaPrimitiveType)
                            val count = getCount.invoke(bg) as Int
                            for (i in 0 until count) {
                                (getDrawable.invoke(bg, i) as? Drawable)?.let { d ->
                                    setColorDeep(d, colorInt)
                                }
                            }
                            true
                        } catch (_: Throwable) {
                            false
                        }

                        // 2) Fallback nếu cách trên không hoạt động
                        if (!okByMethods) {
                            try {
                                val stateObj = bg.constantState ?: return
                                val field = stateObj.javaClass.getDeclaredField("mDrawables")
                                field.isAccessible = true
                                val arr = field.get(stateObj) as? Array<Drawable> ?: return
                                arr.forEach { setColorDeep(it, colorInt) }
                            } catch (_: Throwable) {
                                // Bỏ qua nếu lỗi, tránh crash
                            }
                        }

                        invalidate()
                    }

                    else -> {
                        setColorDeep(bg, colorInt)
                        invalidate()
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Đặt màu đệ quy cho nhiều loại Drawable lồng nhau.
 */
private fun setColorDeep(d: Drawable, colorInt: Int) {
    when (d) {
        is GradientDrawable -> {
            d.mutate()
            d.setColor(colorInt)
        }

        is InsetDrawable -> {
            d.drawable?.let { setColorDeep(it, colorInt) }
        }

        is LayerDrawable -> {
            for (i in 0 until d.numberOfLayers) {
                setColorDeep(d.getDrawable(i), colorInt)
            }
        }

        is ScaleDrawable -> {
            d.drawable?.let { setColorDeep(it, colorInt) }
        }

        is StateListDrawable -> {
            // Gọi lại chính nó để áp dụng màu cho từng state
            try {
                val getCount = StateListDrawable::class.java.getMethod("getStateCount")
                val getDrawable = StateListDrawable::class.java.getMethod("getStateDrawable", Int::class.javaPrimitiveType)
                val count = getCount.invoke(d) as Int
                for (i in 0 until count) {
                    (getDrawable.invoke(d, i) as? Drawable)?.let { setColorDeep(it, colorInt) }
                }
            } catch (_: Throwable) {
            }
        }

        else -> {
            // Các loại khác: bỏ qua
        }
    }
}
