package com.codebasetemplate.features.feature_language.ui.v2

import android.content.Context
import android.content.Intent
import com.core.config.domain.data.IAdPlaceName
import com.codebasetemplate.features.feature_language.ui.v2.adapter.LanguageGroup
import com.codebasetemplate.features.feature_language.ui.v2.adapter.LanguageOption
import com.codebasetemplate.required.ads.AppAdPlaceName
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LanguageActivityV22 : BaseLanguageActivityV2FlowActivity() {

    override val languageNativeAdPlaceName: IAdPlaceName
        get() = AppAdPlaceName.ANCHORED_CHANGE_LANGUAGE_V2_NATIVE_2

    override val trackingViewEventName: String
        get() = "lfo2_view"

    override val trackingCompleteEventName: String
        get() = "lfo2_complete"

    override fun initialExpandedGroupId(languageGroups: List<LanguageGroup>): String? {
        return selectedGroupIdArg ?: languageGroups.firstOrNull()?.id
    }

    override fun onLanguageSelected(option: LanguageOption, group: LanguageGroup) {
        logTrackingComplete()
        val nextIntent = LanguageActivityV23.intentStart(
            context = this,
            selectedGroupId = group.id,
            selectedLanguageTag = option.languageTag,
            fromSetting = isFromSetting,
            fromSplash = isOpenFromSlash,
            fromIntroduction = backFromIntroduction
        )
        startStep(nextIntent)
    }

    override fun onApplyClicked() = Unit

    companion object {
        fun intentStart(
            context: Context,
            selectedGroupId: String,
            selectedLanguageTag: String? = null,
            fromSetting: Boolean = false,
            fromSplash: Boolean = false,
            fromIntroduction: Boolean = false,
        ): Intent {
            return LanguageV2FlowArgs.buildIntent(
                context = context,
                target = LanguageActivityV22::class.java,
                fromSetting = fromSetting,
                fromSplash = fromSplash,
                fromIntroduction = fromIntroduction
            ).apply {
                putExtra(LanguageV2FlowArgs.KEY_SELECTED_GROUP_ID, selectedGroupId)
                putExtra(LanguageV2FlowArgs.KEY_SELECTED_LANGUAGE_TAG, selectedLanguageTag)
            }
        }
    }
}
