package com.core.rate

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.core.rate.databinding.FbDialogRateBottomBinding
import com.core.utilities.invisible
import com.core.utilities.setOnSingleClick
import com.core.utilities.util.hideNavigationBarForBottomDialog
import com.core.utilities.visible
import com.core.utilities.visibleIf
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RateBottomDialog : BottomSheetDialogFragment() {
    private lateinit var mViewBinding: FbDialogRateBottomBinding

    var onRate: ((star: Int) -> Unit)? = null
    var oldImage: Int = R.drawable.fb_ic_smile_bottom_0
    var onIgnore: (() -> Unit)? = null
    private var isRated = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mViewBinding = FbDialogRateBottomBinding.inflate(layoutInflater, container, false)
        return mViewBinding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                it.setBackgroundColor(Color.TRANSPARENT)
                BottomSheetBehavior.from(it).apply {
                    setState(BottomSheetBehavior.STATE_EXPANDED)
                    isDraggable = false
                }
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewBinding.apply {
            RateInApp.instance.primaryColorInt?.let { primaryColor ->
                tvReview.applyRatePrimaryBackgroundTint(primaryColor)
                tvDescription.setTextColor(primaryColor)
            }

            val listStar = listOf(
                ivStar1, ivStar2, ivStar3, ivStar4, ivStar5
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

            if (RateInApp.instance.rateConfig.isHideNavigationBar) {
                requireDialog().hideNavigationBarForBottomDialog()
            }
        }
    }

    private fun updateUi(star: Int) {
        mViewBinding.apply {
            tvReview.isEnabled = star > 0

            val smileIcon = getSmileIcon(star)
            if (ivSmile.isVisible) {
                if (smileIcon != oldImage) {
                    ivSmile.setImageResource(smileIcon)
                }
            } else {
                ivSmile.visible()
                ivSmile.setImageResource(smileIcon)
            }
            if (star >= RateInApp.instance.rateConfig.minStarRateOK) {
                imageArrow.invisible()
                imageOval.invisible()
                tvDescription.invisible()
            } else {
                imageArrow.visible()
                imageOval.visible()
                tvDescription.visible()
            }
            tvMessage.visibleIf(star > 0)
            tvTitle.setText(getTextTitle(star))
            tvMessage.setText(getTextMessage(star))
            oldImage = smileIcon
            tvReview.setText(getTextButton(star))
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!isRated) {
            onIgnore?.invoke()
        }
    }

    private fun getSmileIcon(star: Int): Int {
        return when (star) {
            1 -> R.drawable.fb_ic_smile_bottom_1
            2 -> R.drawable.fb_ic_smile_bottom_2
            3 -> R.drawable.fb_ic_smile_bottom_3
            4 -> R.drawable.fb_ic_smile_bottom_4
            5 -> R.drawable.fb_ic_smile_bottom_5
            else -> R.drawable.fb_ic_smile_bottom_0
        }
    }

    private fun getTextTitle(star: Int): Int {
        return when (star) {
            1, 2, 3 -> R.string.fb_rate_bottom_bad
            4, 5 -> R.string.fb_rate_bottom_good
            else -> R.string.fb_rate_bottom_default
        }
    }

    private fun getTextMessage(star: Int): Int {
        return when (star) {
            1, 2, 3, 4 -> R.string.fb_rate_bottom_mess_bad
            5 -> R.string.fb_rate_bottom_mess_good
            else -> R.string.fb_rate_bottom_mess_default
        }
    }

    private fun getTextButton(star: Int): Int {
        return when {
            star >= RateInApp.instance.rateConfig.minStarRateOK -> R.string.fb_rate_on_google_play
            else -> R.string.fb_text_feedback_to
        }
    }
}