package com.core.rate

import android.content.res.ColorStateList
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.core.rate.databinding.FbDialogThankFeedbackBinding
import com.core.utilities.util.hideNavigationBarForBottomDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ThankForFeedbackBottomDialog : BottomSheetDialogFragment() {
    private lateinit var mViewBinding: FbDialogThankFeedbackBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mViewBinding = FbDialogThankFeedbackBinding.inflate(inflater, container, false)
        return mViewBinding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundColor(Color.TRANSPARENT)
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewBinding.apply {
            RateInApp.instance.primaryColorInt?.let { primaryColor ->
                imageLogoThank.imageTintList = ColorStateList.valueOf(primaryColor)
                tvOk.applyRatePrimaryBackgroundTint(primaryColor)
            }
            val showHeader = resources.getBoolean(R.bool.fb_show_header_thank_feedback)
            imageHeader.visibility = if (showHeader) View.VISIBLE else View.GONE

            tvOk.setOnClickListener {
                dismiss()
            }

            if (RateInApp.instance.rateConfig.isHideNavigationBar) {
                requireDialog().hideNavigationBarForBottomDialog()
            }
        }
    }
}