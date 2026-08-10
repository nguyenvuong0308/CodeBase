package com.codebasetemplate.features.feature_demo_native_pip.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import com.codebasetemplate.core.base_ui.CoreActivity
import com.codebasetemplate.databinding.ActivityNativePipTestBinding
import com.codebasetemplate.required.ads.AppAdPlaceName
import com.core.ads.customviews.ads.NativePictureInPicture
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.baseui.toolbar.CoreToolbarView
import com.core.config.domain.data.IAdPlaceName
import com.core.utilities.setOnSingleClick
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NativePipTestActivity : CoreActivity<ActivityNativePipTestBinding>() {

    private val nativePictureInPicture: NativePictureInPicture by lazy {
        NativePictureInPicture(this)
    }

    override fun bindingProvider(inflater: LayoutInflater): ActivityNativePipTestBinding {
        return ActivityNativePipTestBinding.inflate(inflater)
    }

    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)
        viewBinding.toolbar.onToolbarListener = object : CoreToolbarView.OnToolbarListener {
            override fun onBack() {
                finish()
            }
        }
        viewBinding.restartButton.setOnSingleClick {
            recreate()
        }
        viewBinding.finishButton.setOnSingleClick {
            finish()
        }
    }

    override fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> {
        return listOf(AppAdPlaceName.ANCHORED_NATIVE_PIP_HOME)
    }

    override fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
        nativePictureInPicture.processAdResource(
            lifecycleOwner = this,
            activity = this,
            adResource = adResource,
            placeName = AppAdPlaceName.ANCHORED_NATIVE_PIP_HOME,
            config = NativePictureInPicture.Config(
                initialGravity = Gravity.TOP or Gravity.END
            )
        )
    }
}
