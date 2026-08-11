package com.codebasetemplate.features.feature_onboarding.ui.v2

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.fragment.app.activityViewModels
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingEvent
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingViewModel
import com.codebasetemplate.util.EventTracking
import com.core.baseui.fragment.BaseFragment
import com.core.baseui.fragment.ScreenType
import com.core.baseui.fragment.argument
import com.core.baseui.fragment.argumentNullable
import com.core.startflow.R
import com.core.startflow.StartFlowScreenType
import com.core.startflow.databinding.CoreFragmentOnboardingV2Binding
import com.core.startflow.onboarding.OnBoardingContentProvider
import com.core.startflow.onboarding.OnBoardingUiCustomizer
import com.core.startflow.onboarding.activeOnBoardingContentProvider
import com.core.startflow.onboarding.v2.OnBoardingV2PageActions
import com.core.startflow.onboarding.v2.OnBoardingV2PageRenderer
import com.core.startflow.onboarding.v2.OnBoardingV2PageState
import com.core.startflow.onboarding.v2.OnBoardingV2RenderScope
import com.core.startflow.onboarding.v2.OnBoardingV2RenderedPage
import com.core.startflow.onboarding.v2.OnBoardingV2UiCustomizer
import com.core.startflow.onboarding.v2.OnBoardingV2UiSpec
import com.core.startflow.onboarding.v2.activeOnBoardingV2Renderer
import com.core.startflow.onboarding.v2.applyOnBoardingV2Customizers
import com.core.utilities.setOnSingleClick
import com.core.utilities.visibleIf
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnBoardingFragment2 : BaseFragment<CoreFragmentOnboardingV2Binding>() {

    private val sharedViewModel: OnBoardingViewModel by activityViewModels()

    @Inject
    lateinit var uiCustomizers: Set<@JvmSuppressWildcards OnBoardingUiCustomizer>

    @Inject
    lateinit var contentProviders: Set<@JvmSuppressWildcards OnBoardingContentProvider>

    @Inject
    lateinit var v2UiCustomizers: Set<@JvmSuppressWildcards OnBoardingV2UiCustomizer>

    @Inject
    lateinit var v2PageRenderers: Set<@JvmSuppressWildcards OnBoardingV2PageRenderer>

    private var renderedPage: OnBoardingV2RenderedPage? = null

    companion object {
        fun newInstance(position: Int) = OnBoardingFragment2().apply {
            introductionPosition = position
        }

        fun newInstance(
            position: Int,
            isPageEnd: Boolean,
            isShowAd: Boolean,
            pageCount: Int,
        ) = OnBoardingFragment2().apply {
            introductionPosition = position
            isPageEndOverride = isPageEnd
            isShowAdOverride = isShowAd
            pageCountOverride = pageCount
        }
    }

    private var introductionPosition by argument<Int>()
    private var isPageEndOverride by argumentNullable<Boolean>()
    private var isShowAdOverride by argumentNullable<Boolean>()
    private var pageCountOverride by argumentNullable<Int>()

    override fun bindingProvider(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): CoreFragmentOnboardingV2Binding {
        return CoreFragmentOnboardingV2Binding.inflate(inflater, container, false)
    }

    override val screenType: ScreenType
        get() = StartFlowScreenType.OnBoarding

    @Suppress("DEPRECATION")
    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)

        val contentProvider = contentProviders.activeOnBoardingContentProvider()
        val resolvedPageCount = pageCountOverride?.takeIf { it > 0 }
            ?: contentProvider.introPageCount.coerceAtLeast(1)
        val state = OnBoardingV2PageState(
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

        val renderer = v2PageRenderers.activeOnBoardingV2Renderer(state)
        if (renderer != null) {
            installRenderedPage(
                renderer.render(
                    OnBoardingV2RenderScope(
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
            it.customizeOnBoardingV2(
                fragment = this,
                binding = viewBinding,
                position = introductionPosition,
                isLastPage = state.isLastPage,
            )
        }
    }

    private fun bindDefaultUi(state: OnBoardingV2PageState) = with(viewBinding) {
        ivIntroduction.setImageResource(state.imageRes)
        tvTitle.text = state.title
        tvTitle2.text = state.subtitle
        tvTitle2.visibleIf(state.subtitle != null)

        val actionText = if (state.isLastPage) {
            tvNext.setGradientStrokeBackground(
                Color.RED,
                Color.BLUE,
                strokeWidthDp = 1.5f,
                cornerRadiusDp = 12f,
            )
            tvNext.setFillGradientEnabled(
                true,
                intArrayOf(
                    "#fb03fb".toColorInt(),
                    "#0bdaff".toColorInt(),
                )
            )
            layoutContent.setBackgroundColor(Color.TRANSPARENT)
            getString(R.string.core_onboarding_action_get_start)
        } else {
            tvNext.setFillGradientEnabled(false)
            tvNext.setTextColor(Color.WHITE)
            tvNext.setBackgroundResource(R.drawable.core_button_onboarding)
            layoutContent.setBackgroundResource(R.drawable.core_bg_content_onboarding_v2)
            getString(R.string.core_onboarding_action_next)
        }

        val actions = pageActions(state)
        tvNext.setOnSingleClick { actions.onPrimaryAction() }

        applyUiSpec(
            v2UiCustomizers.applyOnBoardingV2Customizers(
                context = requireContext(),
                state = state,
                initial = OnBoardingV2UiSpec(actionText = actionText),
            )
        )
    }

    private fun applyUiSpec(spec: OnBoardingV2UiSpec) = with(viewBinding) {
        spec.titleTextAppearanceRes?.let(tvTitle::setTextAppearance)
        spec.subtitleTextAppearanceRes?.let(tvTitle2::setTextAppearance)
        spec.actionTextAppearanceRes?.let(tvNext::setTextAppearance)
        tvTitle.visibleIf(spec.isTitleVisible)
        tvTitle2.visibleIf(spec.isSubtitleVisible && tvTitle2.text.isNotEmpty())
        spec.titleTextColor?.let(tvTitle::setTextColor)
        spec.subtitleTextColor?.let(tvTitle2::setTextColor)
        tvNext.text = spec.actionText
        spec.actionTextColor?.let(tvNext::setTextColor)
        spec.rootBackgroundRes?.let(root::setBackgroundResource)
        spec.imageBackgroundRes?.let(ivIntroduction::setBackgroundResource)
        spec.contentBackgroundRes?.let(layoutContent::setBackgroundResource)
        spec.actionBackgroundRes?.let(tvNext::setBackgroundResource)
        spec.isActionFillGradientEnabled?.let { enabled ->
            tvNext.setFillGradientEnabled(
                enabled,
                spec.actionFillGradientColors?.toIntArray(),
            )
        }
    }

    private fun pageActions(state: OnBoardingV2PageState): OnBoardingV2PageActions {
        return object : OnBoardingV2PageActions {
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

    private fun installRenderedPage(page: OnBoardingV2RenderedPage) {
        check(page.view.parent == null) {
            "OnBoardingV2PageRenderer must return a View without a parent"
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
