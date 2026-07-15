package com.codebasetemplate.features.feature_uninstall.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.codebasetemplate.databinding.CoreFragmentUninstallFeedbackBinding
import com.codebasetemplate.features.feature_uninstall.ui.navigate.UninstallNavigateEvent
import com.codebasetemplate.required.ads.AppAdPlaceName
import com.codebasetemplate.required.shortcut.AppScreenType
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.baseui.InsetsViewModel
import com.core.baseui.fragment.BaseChildOfHostFragment
import com.core.baseui.fragment.ScreenType
import com.core.baseui.fragment.collectFlowOn
import com.core.baseui.toolbar.CoreToolbarView
import com.core.config.domain.data.IAdPlaceName
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UninstallFeedbackChildOfHostFragment:
    BaseChildOfHostFragment<CoreFragmentUninstallFeedbackBinding, UninstallNavigateEvent, UninstallShareViewModel>() {
    private val insetsViewModel: InsetsViewModel by activityViewModels()
    override fun bindingProvider(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): CoreFragmentUninstallFeedbackBinding {
        return CoreFragmentUninstallFeedbackBinding.inflate(inflater, container, false)
    }

    override val hostViewModel: UninstallShareViewModel by viewModels( ownerProducer = { requireParentFragment() })
    override val screenType: ScreenType
        get() = AppScreenType.ReasonUninstall

    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)
        viewBinding.run {
            uninstallButton.setOnClickListener {
                // Tạo một Intent để mở màn hình chi tiết ứng dụng trong Cài đặt
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)

                // Chỉ định gói ứng dụng cần mở (chính là ứng dụng của bạn)
                val uri = Uri.fromParts("package", requireContext().packageName, null)
                intent.data = uri

                // Bắt đầu Activity của hệ thống
                startActivity(intent)
            }

            toolbar.onToolbarListener = object : CoreToolbarView.OnToolbarListener {
                override fun onBack() {
                    hostViewModel.navigateActionBack()
                }
            }
        }
        collectFlowOn(insetsViewModel.systemInsets) { systemInsets ->
            systemInsets?.insets?.let {
                viewBinding.root.setPadding(
                    it.left,
                    it.top,
                    it.right,
                    it.bottom
                )
            }
        }
    }

    override fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> {
        return listOf(
            AppAdPlaceName.ANCHORED_UNINSTALL_BOTTOM_STEP_2
        )
    }

    override fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
        viewBinding.layoutBannerNative.processAdResource(adResource, AppAdPlaceName.ANCHORED_UNINSTALL_BOTTOM_STEP_2)
    }
}
