package com.codebasetemplate.required.startflow

import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.core.startflow.databinding.StartflowFragmentOnboardingFullNativeBinding
import com.core.startflow.onboarding.v3.OnBoardingV3FullAdsPageRenderer
import com.core.startflow.onboarding.v3.OnBoardingV3PageState
import com.core.startflow.onboarding.v3.OnBoardingV3RenderScope
import com.core.startflow.onboarding.v3.OnBoardingV3RenderedPage
import com.core.utilities.padding
import com.core.utilities.setOnSingleClick
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** App-owned renderer used to exercise the full-ad V3 customization path. */
@Singleton
class AppOnBoardingV3FullAdsPageRenderer @Inject constructor() :
    OnBoardingV3FullAdsPageRenderer {

    override val priority: Int = 100

    override fun supports(state: OnBoardingV3PageState): Boolean = state.isFullAds

    override fun render(scope: OnBoardingV3RenderScope): OnBoardingV3RenderedPage {
        val binding = StartflowFragmentOnboardingFullNativeBinding.inflate(
            scope.inflater,
            scope.parent,
            false,
        )
        val config = scope.state.config

        binding.ivClose.setOnSingleClick { scope.actions.onNext() }
        binding.ivClose.prepareDelayedVisible(config.isShowClose)
        binding.swipe.prepareDelayedVisible(config.isShowSwipe)
        binding.layoutRoot.padding(top = binding.root.statusBarHeight())

        var areControlsShown = false
        val controlsJob = scope.lifecycleOwner.lifecycleScope.launch {
            scope.lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (areControlsShown) return@repeatOnLifecycle
                delay(config.delayShowCloseSwipeSeconds.coerceAtLeast(0L) * 1000L)
                binding.ivClose.showWithFadeIfNeeded()
                binding.swipe.showWithFadeIfNeeded()
                areControlsShown = true
            }
        }

        return OnBoardingV3RenderedPage(
            view = binding.root,
            onBannerNativeResult = { resource, placeName ->
                binding.layoutBannerNative.processAdResource(resource, placeName)
            },
            onDispose = {
                controlsJob.cancel()
                binding.ivClose.animate().cancel()
                binding.swipe.animate().cancel()
            },
        )
    }

    private fun View.prepareDelayedVisible(isShown: Boolean) {
        isGone = !isShown
        alpha = 0f
    }

    private fun View.showWithFadeIfNeeded() {
        if (!isVisible) return
        animate()
            .alpha(1f)
            .setDuration(SHOW_CONTROL_ANIMATION_DURATION_MS)
            .start()
    }

    private fun View.statusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

    private companion object {
        const val SHOW_CONTROL_ANIMATION_DURATION_MS = 300L
    }
}
