package com.codebasetemplate.features.feature_onboarding.ui.v1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.core.startflow.R
import com.core.startflow.databinding.StartflowFragmentOnboardingBinding
import com.core.startflow.StartFlowScreenType
import com.core.startflow.onboarding.OnBoardingContentProvider
import com.core.startflow.onboarding.OnBoardingUiCustomizer
import com.core.startflow.onboarding.activeOnBoardingContentProvider
import com.core.startflow.onboarding.v1.OnBoardingV1PageActions
import com.core.startflow.onboarding.v1.OnBoardingV1PageRenderer
import com.core.startflow.onboarding.v1.OnBoardingV1PageState
import com.core.startflow.onboarding.v1.OnBoardingV1RenderScope
import com.core.startflow.onboarding.v1.OnBoardingV1RenderedPage
import com.core.startflow.onboarding.v1.OnBoardingV1UiCustomizer
import com.core.startflow.onboarding.v1.OnBoardingV1UiSpec
import com.core.startflow.onboarding.v1.activeOnBoardingV1Renderer
import com.core.startflow.onboarding.v1.applyOnBoardingV1Customizers
import com.core.baseui.fragment.BaseFragment
import com.core.baseui.fragment.ScreenType
import com.core.baseui.fragment.argument
import com.core.baseui.fragment.argumentNullable
import com.core.utilities.visibleIf
import com.core.utilities.setOnSingleClick
import com.codebasetemplate.util.EventTracking
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnBoardingFragment: BaseFragment<StartflowFragmentOnboardingBinding>() {

    private val sharedViewModel: OnBoardingViewModel by activityViewModels()

    @Inject
    lateinit var uiCustomizers: Set<@JvmSuppressWildcards OnBoardingUiCustomizer>

    @Inject
    lateinit var contentProviders: Set<@JvmSuppressWildcards OnBoardingContentProvider>

    @Inject
    lateinit var v1UiCustomizers: Set<@JvmSuppressWildcards OnBoardingV1UiCustomizer>

    @Inject
    lateinit var v1PageRenderers: Set<@JvmSuppressWildcards OnBoardingV1PageRenderer>

    private var renderedPage: OnBoardingV1RenderedPage? = null

    companion object {
        fun newInstance(position: Int) = OnBoardingFragment().apply {
            this.introductionPosition = position
        }

        fun newInstance(
            position: Int,
            isPageEnd: Boolean,
            isShowAd: Boolean,
            pageCount: Int,
        ) = OnBoardingFragment().apply {
            this.introductionPosition = position
            this.isPageEndOverride = isPageEnd
            this.isShowAdOverride = isShowAd
            this.pageCountOverride = pageCount
        }
    }

    private var introductionPosition by argument<Int>()
    private var isPageEndOverride by argumentNullable<Boolean>()
    private var isShowAdOverride by argumentNullable<Boolean>()
    private var pageCountOverride by argumentNullable<Int>()

    override fun bindingProvider(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): StartflowFragmentOnboardingBinding {
        return StartflowFragmentOnboardingBinding.inflate(inflater, container, false)
    }

    override val screenType: ScreenType
        get() = StartFlowScreenType.OnBoarding

    @Suppress("DEPRECATION")
    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)

        val contentProvider = contentProviders.activeOnBoardingContentProvider()
        val resolvedPageCount = pageCountOverride?.takeIf { it > 0 }
            ?: contentProvider.introPageCount.coerceAtLeast(1)
        val state = OnBoardingV1PageState(
            position = introductionPosition,
            pageCount = resolvedPageCount,
            isLastPage = isPageEndOverride
                ?: (introductionPosition == resolvedPageCount - 1),
            isShowAd = isShowAdOverride ?: false,
            imageRes = contentProvider.getImageResIntro(introductionPosition),
            title = getString(contentProvider.getStringIntro(introductionPosition)),
            subtitle = contentProvider.getSubtitleIntro(introductionPosition)?.let(::getString),
            config = remoteConfigRepository.getOnBoardingConfig(),
        )

        val renderer = v1PageRenderers.activeOnBoardingV1Renderer(state)
        if (renderer != null) {
            installRenderedPage(
                renderer.render(
                    OnBoardingV1RenderScope(
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

        bindDefaultUi(state)

        // Backward-compatible bridge for direct View Binding customizers.
        uiCustomizers.sortedBy { it.javaClass.name }.forEach {
            it.customizeOnBoardingV1(
                fragment = this,
                binding = viewBinding,
                position = introductionPosition,
                isLastPage = state.isLastPage,
            )
        }
    }

    private fun bindDefaultUi(state: OnBoardingV1PageState) = with(viewBinding) {
        ivIntroduction.setImageResource(state.imageRes)
        tvTitle.text = state.title
        dotsIndicator.setCountPage(state.pageCount)
        dotsIndicator.setPage(state.position)

        val actions = pageActions(state)
        tvNext.setOnSingleClick { actions.onPrimaryAction() }

        applyUiSpec(
            v1UiCustomizers.applyOnBoardingV1Customizers(
                context = requireContext(),
                state = state,
                initial = OnBoardingV1UiSpec(
                    actionText = getString(
                        if (state.isLastPage) {
                            R.string.core_onboarding_action_get_start
                        } else {
                            R.string.core_onboarding_action_next
                        }
                    )
                ),
            )
        )
    }

    private fun applyUiSpec(spec: OnBoardingV1UiSpec) = with(viewBinding) {
        spec.titleTextAppearanceRes?.let(tvTitle::setTextAppearance)
        spec.actionTextAppearanceRes?.let(tvNext::setTextAppearance)
        tvTitle.visibleIf(spec.isTitleVisible)
        dotsIndicator.visibleIf(spec.isIndicatorVisible)
        spec.titleTextColor?.let(tvTitle::setTextColor)
        tvNext.text = spec.actionText
        spec.actionTextColor?.let(tvNext::setTextColor)
        spec.rootBackgroundRes?.let(root::setBackgroundResource)
        spec.imageBackgroundRes?.let(ivIntroduction::setBackgroundResource)
        spec.indicatorContainerBackgroundRes?.let(layoutIndicator::setBackgroundResource)
        spec.actionBackgroundRes?.let(tvNext::setBackgroundResource)
    }

    private fun pageActions(state: OnBoardingV1PageState): OnBoardingV1PageActions {
        return object : OnBoardingV1PageActions {
            override fun onPrimaryAction() {
                if (state.isLastPage) onFinish() else onNext()
            }

            override fun onNext() {
                sharedViewModel.navigateTo(OnBoardingEvent.NextAction(EventTracking.VALUE_CLICK))
            }

            override fun onFinish() {
                sharedViewModel.navigateTo(OnBoardingEvent.FinishAction(EventTracking.VALUE_CLICK))
            }
        }
    }

    private fun installRenderedPage(page: OnBoardingV1RenderedPage) {
        check(page.view.parent == null) {
            "OnBoardingV1PageRenderer must return a View without a parent"
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
