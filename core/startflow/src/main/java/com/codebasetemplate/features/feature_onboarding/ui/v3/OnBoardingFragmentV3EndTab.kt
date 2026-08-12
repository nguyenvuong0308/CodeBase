package com.codebasetemplate.features.feature_onboarding.ui.v3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.codebasetemplate.features.feature_onboarding.ui.model.OnBoardingItem
import com.core.startflow.R
import com.core.startflow.StartFlowScreenType
import com.core.startflow.databinding.StartflowFragmentOnboardingV3EndTabBinding
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingEvent
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingViewModel
import com.codebasetemplate.util.EventTracking
import com.core.baseui.fragment.BaseFragment
import com.core.baseui.fragment.ScreenType
import com.core.baseui.fragment.argument
import com.core.startflow.onboarding.OnBoardingContentProvider
import com.core.startflow.onboarding.OnBoardingUiCustomizer
import com.core.startflow.onboarding.activeOnBoardingContentProvider
import com.core.startflow.onboarding.v3.OnBoardingV3PageActions
import com.core.startflow.onboarding.v3.OnBoardingV3PageRenderer
import com.core.startflow.onboarding.v3.OnBoardingV3PageState
import com.core.startflow.onboarding.v3.OnBoardingV3PageType
import com.core.startflow.onboarding.v3.OnBoardingV3RenderScope
import com.core.startflow.onboarding.v3.OnBoardingV3RenderedPage
import com.core.startflow.onboarding.v3.OnBoardingV3UiCustomizer
import com.core.startflow.onboarding.v3.OnBoardingV3UiSpec
import com.core.startflow.onboarding.v3.activeOnBoardingV3Renderer
import com.core.startflow.onboarding.v3.applyOnBoardingV3Customizers
import com.core.startflow.onboarding.v3.toOnBoardingV3ActionPosition
import com.core.utilities.visibleIf
import com.core.utilities.setOnSingleClick
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnBoardingFragmentV3EndTab : BaseFragment<StartflowFragmentOnboardingV3EndTabBinding>() {

    private val sharedViewModel: OnBoardingViewModel by activityViewModels()

    @Inject
    lateinit var uiCustomizers: Set<@JvmSuppressWildcards OnBoardingUiCustomizer>

    @Inject
    lateinit var contentProviders: Set<@JvmSuppressWildcards OnBoardingContentProvider>

    @Inject
    lateinit var v3UiCustomizers: Set<@JvmSuppressWildcards OnBoardingV3UiCustomizer>

    @Inject
    lateinit var v3PageRenderers: Set<@JvmSuppressWildcards OnBoardingV3PageRenderer>

    private var renderedPage: OnBoardingV3RenderedPage? = null

    companion object {
        fun newInstance(position: Int) = OnBoardingFragmentV3EndTab().apply {
            this.introductionPosition = position
        }
    }

    private var introductionPosition by argument<Int>()

    override fun bindingProvider(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): StartflowFragmentOnboardingV3EndTabBinding {
        return StartflowFragmentOnboardingV3EndTabBinding.inflate(inflater, container, false)
    }

    override val screenType: ScreenType
        get() = StartFlowScreenType.OnBoarding

    @Suppress("DEPRECATION")
    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)
        val contentProvider = contentProviders.activeOnBoardingContentProvider()
        val onBoardingConfig = remoteConfigRepository.getOnBoardingConfig()
        val state = OnBoardingV3PageState(
            pageType = OnBoardingV3PageType.END_TAB,
            introductionPosition = introductionPosition,
            realPosition = introductionPosition,
            pageCount = sharedViewModel.itemsOnboarding.count {
                it is OnBoardingItem.Item
            },
            isPageEnd = true,
            isShowAd = false,
            actionPosition = onBoardingConfig.toOnBoardingV3ActionPosition(),
            imageRes = contentProvider.getImageResIntro(introductionPosition),
            title = getString(contentProvider.getStringIntro(introductionPosition)),
            subtitle = contentProvider.getSubtitleIntro(introductionPosition)?.let(::getString),
            config = onBoardingConfig,
        )

        val renderer = v3PageRenderers.activeOnBoardingV3Renderer(state)
        if (renderer != null) {
            installRenderedPage(
                renderer.render(
                    OnBoardingV3RenderScope(
                        inflater = layoutInflater,
                        parent = viewBinding.root,
                        lifecycleOwner = viewLifecycleOwner,
                        state = state,
                        actions = pageActions(state),
                    )
                )
            )
            return
        }

        viewBinding.ivIntroduction.setImageResource(state.imageRes)
        viewBinding.tvTitle.text = state.title

        viewBinding.btGetStart.setOnSingleClick {
            pageActions(state).onPrimaryAction()
        }

        applyUiSpec(
            v3UiCustomizers.applyOnBoardingV3Customizers(
                context = requireContext(),
                state = state,
                initial = OnBoardingV3UiSpec(
                    actionText = getString(R.string.core_onboarding_action_get_start)
                ),
            )
        )

        // Backward-compatible bridge for direct View Binding customizers.
        uiCustomizers.sortedBy { it.javaClass.name }.forEach {
            it.customizeOnBoardingV3EndTab(
                fragment = this,
                binding = viewBinding,
                position = introductionPosition
            )
        }
    }

    private fun applyUiSpec(spec: OnBoardingV3UiSpec) = with(viewBinding) {
        spec.titleTextAppearanceRes?.let(tvTitle::setTextAppearance)
        spec.actionTextAppearanceRes?.let(btGetStart::setTextAppearance)
        tvTitle.visibleIf(spec.isTitleVisible)
        spec.titleTextColor?.let(tvTitle::setTextColor)
        btGetStart.text = spec.actionText
        spec.actionTextColor?.let(btGetStart::setTextColor)
        spec.actionBackgroundRes?.let(btGetStart::setBackgroundResource)
        spec.imageBackgroundRes?.let(ivIntroduction::setBackgroundResource)
    }

    private fun pageActions(state: OnBoardingV3PageState): OnBoardingV3PageActions {
        return object : OnBoardingV3PageActions {
            override fun onPrimaryAction() {
                if (state.isPageEnd) onFinish() else onNext()
            }

            override fun onNext() {
                sharedViewModel.navigateTo(OnBoardingEvent.NextAction(EventTracking.VALUE_CLICK))
            }

            override fun onFinish() {
                sharedViewModel.navigateTo(OnBoardingEvent.FinishAction(EventTracking.VALUE_CLICK))
            }
        }
    }

    private fun installRenderedPage(page: OnBoardingV3RenderedPage) {
        check(page.view.parent == null) {
            "OnBoardingV3PageRenderer must return a View without a parent"
        }
        renderedPage = page
        viewBinding.root.removeAllViews()
        viewBinding.root.addView(
            page.view,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        )
    }

    override fun onDestroyView() {
        renderedPage?.onDispose?.invoke()
        renderedPage = null
        super.onDestroyView()
    }

}
