package com.codebasetemplate.features.feature_setting.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import com.codebasetemplate.R
import com.codebasetemplate.databinding.CoreFragmentSettingBinding
import com.codebasetemplate.features.feature_language.ui.LanguageActivity
import com.codebasetemplate.features.main.ui.host.MainHostEvent
import com.codebasetemplate.features.main.ui.host.MainHostViewModel
import com.codebasetemplate.required.shortcut.AppScreenType
import com.core.baseui.fragment.BaseChildOfHostFragment
import com.core.baseui.fragment.ScreenType
import com.core.baseui.toolbar.CoreToolbarView
import com.core.rate.RateInApp
import com.core.utilities.setOnSingleClick
import com.core.utilities.shareApp
import com.core.utilities.toast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingChildOfHostFragment :
    BaseChildOfHostFragment<CoreFragmentSettingBinding, MainHostEvent, MainHostViewModel>() {
    override fun bindingProvider(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): CoreFragmentSettingBinding {
        return CoreFragmentSettingBinding.inflate(inflater, container, false)
    }

    override val hostViewModel: MainHostViewModel by viewModels(ownerProducer = { requireParentFragment() })
    override val screenType: ScreenType
        get() = AppScreenType.Setting

    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)
        viewBinding.run {
            toolbar.onToolbarListener = object : CoreToolbarView.OnToolbarListener {
                override fun onBack() {
                    hostViewModel.navigateTo(MainHostEvent.ActionBack)
                }
            }

            tvLanguage.setOnSingleClick {
                startActivity(
                    LanguageActivity.Companion.intentStart(
                        requireActivity(),
                        fromSetting = true
                    )
                )
            }

            tvPolicy.setOnSingleClick {
                runCatching {
                    val url = "https://sites.google.com/view/highsecure-policy"
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = url.toUri()
                    startActivity(intent)
                }.onFailure {
                    toast("Unable to open WebView: No activity found to handle the requested action")
                }
            }

            tvShareApp.setOnSingleClick {
                context?.shareApp(R.string.app_name)
            }

            tvRateUs.setOnSingleClick {
                activity?.let {
//                    RateInApp.instance.primaryColorInt = "#ff0000".toColorInt()
                    RateInApp.instance.showDialogRateAndFeedback(
                        context = it,
                        onRated = { rate ->
                            analyticsManager.logEvent("rate_app_${rate}")
                        },
                        forceShow = true
                    )
                }
            }
        }
    }
}