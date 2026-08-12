package com.codebasetemplate.features.feature_onboarding.ui.v1

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.isVisible
import com.core.startflow.databinding.StartflowDotIndicatorBinding
import com.core.dimens.R
import com.core.utilities.changeSize
import com.core.utilities.gone


class DotIndicator @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayoutCompat(context, attrs) {
    private val _viewBinding = StartflowDotIndicatorBinding.inflate(LayoutInflater.from(context), this, true)
    private val _listDot = ArrayList<View>()
    init {
        _viewBinding.run {
            _listDot.add(dot1)
            _listDot.add(dot2)
            _listDot.add(dot3)
            _listDot.add(dot4)
            _listDot.add(dot5)
            _listDot.add(dot6)
        }
    }

    fun setCountPage(count: Int) {
        _listDot.forEachIndexed { index, view ->
            if(index < count) {
                view.visibility = VISIBLE
            } else {
                view.visibility = GONE
            }
        }
    }

    fun setPage(page: Int) {
        _listDot.forEachIndexed { index, view ->
            val isSelected = index <= page
            view.isSelected = isSelected
            view.changeSize(context.resources.getDimensionPixelSize(R.dimen._10dp), context.resources.getDimensionPixelSize(R.dimen._10dp))
        }

        _listDot.filter { it.isVisible }.lastOrNull { it.isSelected }?.changeSize(
            context.resources.getDimensionPixelSize(R.dimen._15dp),
            context.resources.getDimensionPixelSize(R.dimen._10dp)
        )
    }

    fun setFullAdPosition(position: Int) {
        _listDot.getOrNull(position)?.gone()
    }

}