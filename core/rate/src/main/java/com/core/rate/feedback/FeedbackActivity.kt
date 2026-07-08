package com.core.rate.feedback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.widget.addTextChangedListener
import com.core.baseui.BaseActivity
import com.core.baseui.toolbar.CoreToolbarView
import com.core.rate.R
import com.core.rate.RateInApp
import com.core.rate.applyRateFeedbackOptionColors
import com.core.rate.applyRatePrimaryBackgroundTint
import com.core.rate.databinding.FbActivityFeedbackBinding
import com.core.utilities.setOnSingleClick
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FeedbackActivity : BaseActivity<FbActivityFeedbackBinding>() {

    override val isHideNavigationBar: Boolean = RateInApp.instance.rateConfig.isHideNavigationBar

    override val isHideStatusBar: Boolean = RateInApp.instance.rateConfig.isHideStatusBar

    override val isRegisterOnKeyboardListener: Boolean = true

    override fun bindingProvider(inflater: LayoutInflater): FbActivityFeedbackBinding {
        return FbActivityFeedbackBinding.inflate(inflater)
    }

    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)

        viewBinding.apply {
            RateInApp.instance.primaryColorInt?.let { primaryColor ->
                ivSendFeedback.applyRatePrimaryBackgroundTint(primaryColor)
            }

            toolbar.onToolbarListener = object : CoreToolbarView.OnToolbarListener {
                override fun onBack() {
                    finish()
                }
            }

            if (resources.getBoolean(R.bool.fb_button_submit_feedback_inside_input)) {
                val lp = ivSendFeedback.layoutParams as LinearLayoutCompat.LayoutParams
                lp.setMargins(lp.leftMargin, resources.getDimensionPixelSize(R.dimen.fb_top_margin_button_submit_feedback), lp.rightMargin, lp.bottomMargin)
                edtFeedback.setPadding(edtFeedback.paddingLeft, edtFeedback.paddingTop, edtFeedback.paddingRight, resources.getDimensionPixelSize(R.dimen.fb_bottom_padding_input))
            }

            val isFlexUI = resources.getBoolean(R.bool.fb_matter_feedback_flex_ui)
            matterFlexible.visibility = if (isFlexUI) View.VISIBLE else View.GONE
            matterLinear.visibility = if (!isFlexUI) View.VISIBLE else View.GONE

            val isType = resources.getBoolean(R.bool.fb_show_text_type_in_feedback)
            tvType.visibility = if (isType) View.VISIBLE else View.GONE


            val listOption = if (!isFlexUI) listOf(tvFeatureQuality, tvCrash, tvBug, tvOthers) else listOf(tvFeatureQualityFl, tvCrashFl, tvBugFl, tvOtherFl)
            listOption.forEach {
                RateInApp.instance.primaryColorInt?.let { primaryColor ->
                    it.applyRateFeedbackOptionColors(primaryColor)
                }
                it.setOnSingleClick { _ ->
                    it.isSelected = !it.isSelected
                }
            }

            edtFeedback.addTextChangedListener(afterTextChanged = {
                it?.let {
                    ivSendFeedback.isEnabled = it.length >= 6
                }
            })

            ivSendFeedback.setOnClickListener {
                ShareUtils.feedbackFocusEmail(
                    this@FeedbackActivity, Feedback(
                        content = edtFeedback.text.toString(),
                        isFeatureQuality = tvFeatureQuality.isSelected || tvFeatureQualityFl.isSelected,
                        isCrash = tvCrash.isSelected || tvCrashFl.isSelected,
                        isBug = tvBug.isSelected || tvBugFl.isSelected,
                        isOthers = tvOthers.isSelected || tvOtherFl.isSelected
                    )
                )
                setResult(RESULT_OK)
                finish()
            }
        }
    }
}

