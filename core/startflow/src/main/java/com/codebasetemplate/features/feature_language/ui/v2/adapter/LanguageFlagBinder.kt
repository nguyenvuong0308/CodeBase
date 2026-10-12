package com.codebasetemplate.features.feature_language.ui.v2.adapter

import android.widget.TextView
import com.core.startflow.R
import java.util.Locale

/**
 * Shows the `flag_xx` drawable of [countryCode], falling back to the flag emoji
 * when the drawable is missing.
 */
internal fun bindLanguageFlag(view: TextView, countryCode: String?) {
    if (countryCode.isNullOrBlank()) {
        view.text = ""
        view.setBackgroundResource(R.drawable.bg_language_flag_circle)
        return
    }

    val flagResource = view.context.resources.getIdentifier(
        languageFlagResourceName(countryCode),
        "drawable",
        view.context.packageName
    )
    if (flagResource != 0) {
        view.text = ""
        view.setBackgroundResource(flagResource)
    } else {
        view.text = LanguageV2Adapter.countryFlagEmoji(countryCode)
        view.setBackgroundResource(R.drawable.bg_language_flag_circle)
    }
}

internal fun languageFlagResourceName(countryCode: String): String {
    return "flag_${countryCode.lowercase(Locale.US)}"
}
