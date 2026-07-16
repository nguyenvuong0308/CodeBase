package com.codebasetemplate.features.feature_language.ui.v2

import android.content.Context
import android.content.Intent
import com.core.config.domain.data.IAdPlaceName
import com.codebasetemplate.features.feature_language.ui.v2.adapter.LanguageGroup
import com.codebasetemplate.features.feature_language.ui.v2.adapter.LanguageOption
import com.codebasetemplate.required.ads.AppAdPlaceName
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LanguageActivityV23 : BaseLanguageActivityV2FlowActivity() {

    override val languageNativeAdPlaceName: IAdPlaceName
        get() = AppAdPlaceName.ANCHORED_CHANGE_LANGUAGE_V2_NATIVE_3

    override val trackingViewEventName: String
        get() = "lfo3_view"

    override val trackingCompleteEventName: String
        get() = "lfo3_complete"

    override val shouldHandleLanguageApplyNavigation: Boolean
        get() = true

    override fun initialExpandedGroupId(languageGroups: List<LanguageGroup>): String? {
        return selectedGroupIdArg
            ?: languageGroups.firstOrNull { group ->
                group.options.any { it.languageTag == selectedLanguageTagArg }
            }?.id
    }

    override fun initialSelectedLanguageTag(): String? {
        return selectedLanguageTagArg
    }

    override fun initialApplyVisibility(): Boolean {
        return initialSelectedLanguageTag() != null
    }

    override fun initialApplyEnabled(): Boolean {
        return initialSelectedLanguageTag() != null
    }

    override fun onLanguageSelected(option: LanguageOption, group: LanguageGroup) {
        setApplyButtonState(isVisible = true, isEnabled = true)
    }

    override fun onApplyClicked() {
        logTrackingComplete()
        startApplyFlow()
    }

    companion object {
        fun intentStart(
            context: Context,
            selectedGroupId: String,
            selectedLanguageTag: String,
            fromSetting: Boolean = false,
            fromSplash: Boolean = false,
            fromIntroduction: Boolean = false,
        ): Intent {
            return LanguageV2FlowArgs.buildIntent(
                context = context,
                target = LanguageActivityV23::class.java,
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
