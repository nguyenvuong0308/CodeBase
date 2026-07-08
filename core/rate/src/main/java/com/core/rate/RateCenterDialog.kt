package com.core.rate

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.core.rate.databinding.FbDialogRateBinding
import com.core.utilities.invisible
import com.core.utilities.setOnSingleClick
import com.core.utilities.util.hideNavigationBar
import com.core.utilities.visible


class RateCenterDialog(context: Context) : AlertDialog(context) {
    private var mViewBinding = FbDialogRateBinding.inflate(LayoutInflater.from(context))

    var onRate: ((star: Int) -> Unit)? = null
    var oldImage: Int = R.drawable.fb_ic_smile_1
    var onIgnore: (() -> Unit)? = null
    private var isRated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mViewBinding.root)
        mViewBinding.apply {
            RateInApp.instance.primaryColorInt?.let { primaryColor ->
                tvReview.applyRatePrimaryBackgroundTint(primaryColor)
                tvDescription.setTextColor(primaryColor)
            }

            val listStar = listOf(
                ivStar1,
                ivStar2,
                ivStar3,
                ivStar4,
                ivStar5
            )
            // khởi tạo mặc định lúc đầu khi chưa chọn sao
            updateUi(0)

            listStar.forEachIndexed { index, imageView ->
                imageView.tag = index + 1
                imageView.setOnSingleClick { view ->
                    listStar.forEach {
                        it.isSelected = it.tag as Int <= view.tag as Int
                    }
                    updateUi(star = imageView.tag as Int)
                }
            }

            tvReview.setOnClickListener {
                val star = listStar.last { it.isSelected }.tag as Int
                onRate?.invoke(star)
                isRated = true
                dismiss()
            }

            lottieStar.addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {

                }

                override fun onAnimationEnd(animation: Animator) {
                    lottieStar.invisible()
                    ivEndStar1.visible()
                    listStar.forEach {
                        it.visible()
                    }
                    listStar.last().startAnimation(AnimationUtils.loadAnimation(context, R.anim.shake))
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationRepeat(animation: Animator) {
                }
            })

            setOnDismissListener {
                if (!isRated) {
                    onIgnore?.invoke()
                }
            }
        }

        val window = window
        val wlp = window?.attributes
        wlp?.let {
            wlp.width = (context.resources.displayMetrics.widthPixels * 0.9f).toInt()
            wlp.height = WindowManager.LayoutParams.WRAP_CONTENT
            window.setAttributes(wlp)
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }

        if (RateInApp.instance.rateConfig.isHideNavigationBar) {
            hideNavigationBar()
        }
    }

    private fun updateUi(star: Int) {
        mViewBinding.apply {
            tvReview.isEnabled = star > 0
            val smileIcon = getSmileIcon(star)
            if (star == 0) {
                ivSmile.visibility = View.INVISIBLE
            } else {
                if (ivSmile.isVisible) {
                    if (smileIcon != oldImage) {
                        animateIconChange(ivSmile, smileIcon)
                    }
                } else {
                    ivSmile.visibility = View.VISIBLE
                    ivSmile.setImageResource(smileIcon)
                }
            }
            tvTitle.setText(getTextTitle(star))
            oldImage = smileIcon
            tvReview.setText(getTextButton(star))
        }
    }

    private fun getSmileIcon(star: Int): Int {
        return when (star) {
            1 -> R.drawable.fb_ic_smile_1
            2 -> R.drawable.fb_ic_smile_2
            3 -> R.drawable.fb_ic_smile_3
            4 -> R.drawable.fb_ic_smile_4
            5 -> R.drawable.fb_ic_smile_5
            else -> R.drawable.fb_ic_smile_1
        }
    }

    private fun getTextTitle(star: Int): Int {
        return when (star) {
            1, 2, 3, 4 -> R.string.fb_rate_us_bad
            5 -> R.string.fb_rate_us_good
            else -> R.string.fb_rate_us_default
        }
    }

    private fun getTextButton(star: Int): Int {
        return when {
            star >= RateInApp.instance.rateConfig.minStarRateOK -> R.string.fb_rate_on_google_play
            else -> R.string.fb_feedback_rate
        }
    }

    private fun animateIconChange(imageView: ImageView, newIcon: Int) {
        val fadeOut = ObjectAnimator.ofFloat(imageView, "alpha", 1f, 0f)
        val fadeIn = ObjectAnimator.ofFloat(imageView, "alpha", 0f, 1f)

        fadeOut.duration = 200
        fadeIn.duration = 200

        fadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                imageView.setImageResource(newIcon)
                fadeIn.start()
            }
        })

        fadeOut.start()
    }
}