package com.codebasetemplate.features.feature_language.ui

import android.content.Context
import android.content.Intent
import com.codebasetemplate.features.feature_language.ui.v2.LanguageActivityV21
import com.codebasetemplate.features.feature_language.ui.v2.LanguageActivityV2FromSetting
import com.codebasetemplate.required.firebase.LanguageActivityConfig

object LanguageActivityNavigator {

    fun intentStart(
        context: Context,
        config: LanguageActivityConfig,
        fromSetting: Boolean = false,
        fromSplash: Boolean = false,
        fromIntroduction: Boolean = false,
    ): Intent {
        return if (config.isV2) {
            if (fromSetting) {
                LanguageActivityV2FromSetting.intentStart(
                    context = context,
                    fromSetting = true,
                    fromSplash = fromSplash,
                    fromIntroduction = fromIntroduction
                )
            } else {
                LanguageActivityV21.intentStart(
                    context = context,
                    fromSetting = false,
                    fromSplash = fromSplash,
                    fromIntroduction = fromIntroduction
                )
            }
        } else {
            LanguageActivity.intentStart(
                context = context,
                fromSetting = fromSetting,
                fromSplash = fromSplash,
                fromIntroduction = fromIntroduction
            )
        }
    }
}
