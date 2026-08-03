package com.codebasetemplate.features.feature_language.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.core.startflow.databinding.CoreItemLanguageLtrBinding
import com.core.startflow.databinding.CoreItemLanguageRtlBinding
import com.core.baseui.adapter.DataBoundListAdapter
import com.core.baseui.executor.AppExecutors
import com.core.baseui.supportedlanguage.SupportedLanguage
import com.core.utilities.visibleIf
import com.google.android.material.textview.MaterialTextView

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

    var systemLanguageCode = ""
    var isShowHand = true
    var onClickListener: ((SupportedLanguage) -> Unit)? = null

    override fun getItemId(position: Int): Long {
        return currentList[position].languageCode.hashCode().toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return currentList[position].rightToLeft.viewType
    }

    override fun createBinding(parent: ViewGroup, viewType: Int): ViewBinding {
        return when (viewType) {
            SupportedLanguage.RightToLeft.No.viewType -> CoreItemLanguageLtrBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

            else -> CoreItemLanguageRtlBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        }
    }

    override fun bind(binding: ViewBinding, item: SupportedLanguage) {
        if (item.languageCode == systemLanguageCode) {
            isShowHand = isShowHand && !item.isSelected
        }
        val isShowClickHand = item.languageCode == systemLanguageCode && isShowHand

        when (binding) {
            is CoreItemLanguageLtrBinding -> {
                showData(
                    item = item,
                    tvLanguageName = binding.tvLanguage,
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

            is CoreItemLanguageRtlBinding -> {
                showData(
                    item = item,
                    tvLanguageName = binding.tvLanguage,
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
                return@setOnClickListener
            }
            onClickListener?.invoke(item)
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
        }

    }

    private fun hideHandClick() {
        val index = currentList.indexOfFirst { it.languageCode == systemLanguageCode }
        isShowHand = false
        if(index >= 0) {
            notifyItemChanged(index)
        }
    }

    private fun showData(
        item: SupportedLanguage,
        tvLanguageName: MaterialTextView,
    ) {
        tvLanguageName.text = item.displayName
        tvLanguageName.isSelected = item.isSelected
    }

}