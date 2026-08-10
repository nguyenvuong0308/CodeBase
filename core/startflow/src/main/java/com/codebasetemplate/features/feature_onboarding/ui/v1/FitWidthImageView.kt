package com.codebasetemplate.features.feature_onboarding.ui.v1

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class FitWidthImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val drawable = drawable
        if (drawable != null) {
            // Lấy chiều rộng view từ parent
            val width = MeasureSpec.getSize(widthMeasureSpec)
            // Tính chiều cao dựa trên tỷ lệ ảnh
            val aspectRatio = drawable.intrinsicHeight.toFloat() / drawable.intrinsicWidth
            val height = (width * aspectRatio).toInt()
            setMeasuredDimension(width, height)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}
