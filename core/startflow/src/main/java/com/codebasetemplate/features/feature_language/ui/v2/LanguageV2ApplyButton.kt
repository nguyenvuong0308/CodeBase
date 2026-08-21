package com.codebasetemplate.features.feature_language.ui.v2

import androidx.appcompat.widget.AppCompatButton
import com.core.startflow.R

internal enum class LanguageV2ApplyMode {
    ICON,
    TEXT;

    companion object {
        fun from(useText: Boolean): LanguageV2ApplyMode = if (useText) TEXT else ICON
    }
}

internal fun AppCompatButton.configureLanguageV2ApplyButton() {
    when (LanguageV2ApplyMode.from(resources.getBoolean(R.bool.startflow_language_v2_apply_use_text))) {
        LanguageV2ApplyMode.ICON -> {
            text = null
            contentDescription = context.getString(R.string.core_common_save)
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_language_v2_check,
                0,
            )
        }

        LanguageV2ApplyMode.TEXT -> {
            setText(R.string.startflow_language_v2_apply_text_value)
            contentDescription = null
            setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        }
    }
}
