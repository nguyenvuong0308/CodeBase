package com.codebasetemplate.features.feature_language.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.core.startflow.databinding.StartflowItemLanguageLtrBinding
import com.core.startflow.databinding.StartflowItemLanguageRtlBinding
import com.core.baseui.adapter.DataBoundListAdapter
import com.core.baseui.executor.AppExecutors
import com.core.baseui.supportedlanguage.SupportedLanguage
import com.core.utilities.visibleIf
import com.codebasetemplate.features.feature_language.ui.v2.adapter.bindLanguageFlag
import com.google.android.material.textview.MaterialTextView
import com.core.dimens.R as DimensR

class SupportedLanguageAdapter(
    appExecutors: AppExecutors,
) : DataBoundListAdapter<SupportedLanguage, ViewBinding>(
    appExecutors = appExecutors,
    diffCallback = object : DiffUtil.ItemCallback<SupportedLanguage>() {
        override fun areItemsTheSame(
            oldItem: SupportedLanguage,
            newItem: SupportedLanguage,
        ): Boolean {
            return oldItem.languageCode == newItem.languageCode
        }

        override fun areContentsTheSame(
            oldItem: SupportedLanguage,
            newItem: SupportedLanguage,
        ): Boolean {
            return true
        }
    }
) {

    var clickGuideLanguageCode = ""
    var isShowHand = true
    var isShowFlag = true
    var onClickListener: ((SupportedLanguage) -> Unit)? = null

    override fun getItemId(position: Int): Long {
        return currentList[position].languageCode.hashCode().toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return currentList[position].rightToLeft.viewType
    }

    override fun createBinding(parent: ViewGroup, viewType: Int): ViewBinding {
        return when (viewType) {
            SupportedLanguage.RightToLeft.No.viewType -> StartflowItemLanguageLtrBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

            else -> StartflowItemLanguageRtlBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        }
    }

    override fun bind(binding: ViewBinding, item: SupportedLanguage) {
        val isShowClickHand = item.languageCode == clickGuideLanguageCode && isShowHand

        when (binding) {
            is StartflowItemLanguageLtrBinding -> {
                showData(
                    item = item,
                    tvLanguageName = binding.tvLanguage,
                )
                bindFlag(
                    flagView = binding.tvFlag,
                    languageView = binding.tvLanguage,
                    item = item,
                    isFlagOnRight = false,
                )
                binding.tvLanguage.isSelected = item.isSelected
                binding.ivChecked.isSelected = item.isSelected
                binding.lottieView.visibleIf(isShowClickHand)
                if (isShowClickHand) {
                    binding.lottieView.playAnimation()
                } else {
                    binding.lottieView.cancelAnimation()
                }
            }

            is StartflowItemLanguageRtlBinding -> {
                showData(
                    item = item,
                    tvLanguageName = binding.tvLanguage,
                )
                bindFlag(
                    flagView = binding.tvFlag,
                    languageView = binding.tvLanguage,
                    item = item,
                    isFlagOnRight = true,
                )
                binding.tvLanguage.isSelected = item.isSelected
                binding.ivChecked.isSelected = item.isSelected

                binding.lottieView.visibleIf(isShowClickHand)
                if (isShowClickHand) {
                    binding.lottieView.playAnimation()
                } else {
                    binding.lottieView.cancelAnimation()
                }
            }

        }

        binding.root.setOnClickListener {
            if (isShowHand) {
                hideHandClick()
            }
            if (item.isSelected) {
                onClickListener?.invoke(item)
                return@setOnClickListener
            }
            val oldSelected = currentList.indexOfFirst { it.isSelected }
            if (oldSelected >= 0) {
                notifyItemChanged(oldSelected)
            }

            currentList.forEach { it.isSelected = false }
            item.isSelected = true

            val index = currentList.indexOfFirst { it.languageCode == item.languageCode }
            if (index >= 0) {
                notifyItemChanged(index)
            }
            onClickListener?.invoke(item)
        }

    }

    private fun hideHandClick() {
        val index = currentList.indexOfFirst { it.languageCode == clickGuideLanguageCode }
        isShowHand = false
        if(index >= 0) {
            notifyItemChanged(index)
        }
    }

    private fun bindFlag(
        flagView: TextView,
        languageView: TextView,
        item: SupportedLanguage,
        isFlagOnRight: Boolean,
    ) {
        val countryCode = LanguageFlagCountry.of(item.languageCode)
        val showFlag = isShowFlag && countryCode != null
        flagView.visibleIf(showFlag)
        if (showFlag) {
            bindLanguageFlag(flagView, countryCode)
        }

        val resources = languageView.resources
        val defaultSidePadding = resources.getDimensionPixelSize(DimensR.dimen._20dp)
        val flagSidePadding = if (showFlag) {
            val params = flagView.layoutParams as ViewGroup.MarginLayoutParams
            val flagMargin = if (isFlagOnRight) params.rightMargin else params.leftMargin
            flagMargin + params.width + resources.getDimensionPixelSize(DimensR.dimen._12dp)
        } else {
            defaultSidePadding
        }
        languageView.setPadding(
            if (isFlagOnRight) defaultSidePadding else flagSidePadding,
            languageView.paddingTop,
            if (isFlagOnRight) flagSidePadding else defaultSidePadding,
            languageView.paddingBottom,
        )
    }

    private fun showData(
        item: SupportedLanguage,
        tvLanguageName: MaterialTextView,
    ) {
        tvLanguageName.text = item.displayName
        tvLanguageName.isSelected = item.isSelected
    }

}
