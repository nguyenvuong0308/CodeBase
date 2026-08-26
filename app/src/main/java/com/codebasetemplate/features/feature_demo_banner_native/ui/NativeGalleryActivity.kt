package com.codebasetemplate.features.feature_demo_banner_native.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import com.codebasetemplate.R
import com.codebasetemplate.core.base_ui.CoreActivity
import com.codebasetemplate.databinding.ActivityNativeGalleryBinding
import com.codebasetemplate.databinding.ItemNativeGalleryOptionBinding
import com.codebasetemplate.databinding.ItemNativeGallerySectionBinding
import com.core.baseui.toolbar.CoreToolbarView
import com.core.utilities.setOnSingleClick
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NativeGalleryActivity : CoreActivity<ActivityNativeGalleryBinding>() {

    override fun bindingProvider(inflater: LayoutInflater): ActivityNativeGalleryBinding {
        return ActivityNativeGalleryBinding.inflate(inflater)
    }

    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)
        viewBinding.toolbar.onToolbarListener = object : CoreToolbarView.OnToolbarListener {
            override fun onBack() {
                finish()
            }
        }
        viewBinding.formatCount.text = resources.getQuantityString(
            R.plurals.native_gallery_format_count,
            NativeGalleryPreviewConfig.options.size,
            NativeGalleryPreviewConfig.options.size,
        )
        renderOptions()
    }

    private fun renderOptions() {
        var currentGroup: String? = null
        NativeGalleryPreviewConfig.options.forEach { option ->
            if (option.group != currentGroup) {
                currentGroup = option.group
                val sectionBinding = ItemNativeGallerySectionBinding.inflate(
                    layoutInflater,
                    viewBinding.optionsContainer,
                    false,
                )
                sectionBinding.sectionTitle.text = option.group
                viewBinding.optionsContainer.addView(sectionBinding.root)
            }

            val optionBinding = ItemNativeGalleryOptionBinding.inflate(
                layoutInflater,
                viewBinding.optionsContainer,
                false,
            )
            optionBinding.optionTitle.text = option.title
            optionBinding.optionDetails.text = option.detailsText()
            optionBinding.root.contentDescription = "native-gallery-option:${option.id}"
            optionBinding.root.setOnSingleClick { openPreview(option.arguments) }
            viewBinding.optionsContainer.addView(optionBinding.root)
        }
    }

    private fun NativeGalleryOption.detailsText(): String {
        val previewArguments = arguments
        return if (previewArguments.collapsible) {
            getString(
                R.string.native_gallery_collapsible_details,
                previewArguments.templateKey,
                previewArguments.expandTemplateKey,
            )
        } else {
            getString(R.string.native_gallery_template_details, previewArguments.templateKey)
        }
    }

    private fun openPreview(arguments: NativeGalleryPreviewArguments) {
        startActivity(
            Intent(this, NativeGalleryPreviewActivity::class.java).apply {
                putExtra(NativeGalleryPreviewConfig.EXTRA_TEMPLATE_KEY, arguments.templateKey)
                putExtra(NativeGalleryPreviewConfig.EXTRA_COLLAPSIBLE, arguments.collapsible)
                arguments.expandTemplateKey?.let { expandTemplateKey ->
                    putExtra(
                        NativeGalleryPreviewConfig.EXTRA_EXPAND_TEMPLATE,
                        expandTemplateKey,
                    )
                }
            }
        )
    }
}
