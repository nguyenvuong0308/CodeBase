package com.codebasetemplate.features.feature_onboarding.ui.v3

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.codebasetemplate.features.feature_onboarding.ui.model.OnBoardingItem
import com.core.startflow.R
import com.core.startflow.StartFlowScreenType
import com.core.startflow.databinding.StartflowFragmentOnboardingV3Binding
import com.codebasetemplate.features.feature_onboarding.ui.v1.OnBoardingEvent
import com.codebasetemplate.util.EventTracking
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.baseui.fragment.BaseFragment
import com.core.baseui.fragment.ScreenType
import com.core.baseui.fragment.argument
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.OnBoardingConfig
import com.core.startflow.OnBoardingConfigFactory as StartFlowOnBoardingConfigFactory
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
import com.core.utilities.getStatusBarHeight
import com.core.utilities.gone
import com.core.utilities.padding
import com.core.utilities.setOnSingleClick
import com.core.utilities.visibleIf
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OnBoardingFragmentV3 : BaseFragment<StartflowFragmentOnboardingV3Binding>() {

    private val sharedViewModel: OnBoardingViewModelV3 by activityViewModels()

    @Inject
    lateinit var uiCustomizers: Set<@JvmSuppressWildcards OnBoardingUiCustomizer>

    @Inject
    lateinit var contentProviders: Set<@JvmSuppressWildcards OnBoardingContentProvider>

    @Inject
    lateinit var v3UiCustomizers: Set<@JvmSuppressWildcards OnBoardingV3UiCustomizer>

    @Inject
    lateinit var v3PageRenderers: Set<@JvmSuppressWildcards OnBoardingV3PageRenderer>

    private var renderedPage: OnBoardingV3RenderedPage? = null
    private var latestAdResource: AdLoadBannerNativeUiResource? = null

    companion object {
        private const val SHOW_CONTROL_ANIMATION_DURATION_MS = 300L
        fun newInstance(position: Int, isPageEnd: Boolean, isShowAd: Boolean, realPosition: Int, fullAds: Boolean) = OnBoardingFragmentV3().apply {
            this.realPosition = realPosition
            this.introductionPosition = position
            this.isShowAd = isShowAd
            this.isPageEnd = isPageEnd
            this.fullAds = fullAds
        }
    }

    private var realPosition by argument<Int>()
    private var introductionPosition by argument<Int>()
    private var isPageEnd by argument<Boolean>()
    private var isShowAd by argument<Boolean>()
    private var fullAds by argument<Boolean>()

    override fun bindingProvider(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): StartflowFragmentOnboardingV3Binding {
        return StartflowFragmentOnboardingV3Binding.inflate(inflater, container, false)
    }

    override val screenType: ScreenType
        get() = StartFlowScreenType.OnBoarding

    @Suppress("DEPRECATION")
    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)

        val contentProvider = contentProviders.activeOnBoardingContentProvider()
        val onBoardingConfig = remoteConfigRepository.getOnBoardingConfig()
        val state = OnBoardingV3PageState(
            pageType = OnBoardingV3PageType.STANDARD,
            introductionPosition = introductionPosition,
            realPosition = realPosition,
            pageCount = sharedViewModel.itemsOnboarding
                .filterIsInstance<OnBoardingItem.Item>()
                .size,
            isPageEnd = isPageEnd,
            isShowAd = isShowAd,
            actionPosition = onBoardingConfig.toOnBoardingV3ActionPosition(),
            imageRes = contentProvider.getImageResIntro(realPosition),
            title = getString(contentProvider.getStringIntro(realPosition)),
            subtitle = contentProvider.getSubtitleIntro(realPosition)?.let(::getString),
            config = onBoardingConfig,
            isFullAds = fullAds
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
            latestAdResource?.let(::dispatchAdResource)
            return
        }
        viewBinding.layoutMediumNative.visibleIf(!fullAds)
        viewBinding.layoutFullNative.visibleIf(fullAds)

        bindLayoutMediumNativeUi(state)
        bindLayoutFullNativeUi(state)

        // Backward-compatible bridge for apps that still use direct View Binding customization.
        uiCustomizers.sortedBy { it.javaClass.name }.forEach {
            it.customizeOnBoardingV3(
                fragment = this,
                binding = viewBinding,
                introductionPosition = introductionPosition,
                realPosition = realPosition,
                isPageEnd = isPageEnd,
                isShowAd = isShowAd,
                onBoardingConfig = onBoardingConfig
            )
        }
    }

    private var isControlShownInCurrentView = false

    private fun bindLayoutFullNativeUi(state: OnBoardingV3PageState) {
        viewBinding.ivClose.setOnSingleClick {
            sharedViewModel.navigateTo(OnBoardingEvent.NextEvent)
        }
        val onBoardingConfig = remoteConfigRepository.getOnBoardingConfig()
        val ivClose = viewBinding.ivClose
        val swipe = viewBinding.swipe
        isControlShownInCurrentView = false
        ivClose.prepareDelayedVisible(onBoardingConfig.isShowClose)
        swipe.prepareDelayedVisible(onBoardingConfig.isShowSwipe)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (isControlShownInCurrentView) return@repeatOnLifecycle
                delay(onBoardingConfig.delayShowCloseSwipeSeconds.coerceAtLeast(0L) * 1000L)
                ivClose.showWithFadeIfNeeded()
                swipe.showWithFadeIfNeeded()
                isControlShownInCurrentView = true
            }
        }
        viewBinding.layoutFullNative.padding(top = getStatusBarHeight()) // Fullscreen cách statusbar (để hiển thị chữ "i" quảng cáo không bị che)
    }


    private fun View.prepareDelayedVisible(isShow: Boolean) {
        isGone = !isShow
        alpha = 0f
    }

    private fun View.showWithFadeIfNeeded() {
        if (!isVisible) return
        animate()
            .alpha(1f)
            .setDuration(SHOW_CONTROL_ANIMATION_DURATION_MS)
            .start()
    }

    private fun bindLayoutMediumNativeUi(state: OnBoardingV3PageState) {
        viewBinding.ivIntroduction.setImageResource(state.imageRes)
        viewBinding.tvTitle.text = state.title
        viewBinding.tvTitleTop.text = state.title
        viewBinding.tvSubTitleTop.text = state.subtitle
        viewBinding.tvSubTitleTop.visibleIf(state.subtitle != null)

        val positionNext = state.config.positionNext
        viewBinding.topNext.visibleIf(positionNext == OnBoardingConfig.POSITION_NEXT_TOP)
        viewBinding.topNextV2.visibleIf(positionNext == OnBoardingConfig.POSITION_NEXT_TOP_V2)
        viewBinding.bottomNext.visibleIf(positionNext == OnBoardingConfig.POSITION_NEXT_BOTTOM)
        if (positionNext == OnBoardingConfig.POSITION_NEXT_TOP) {
            viewBinding.frameAds.setBackgroundColor(Color.WHITE)
        } else {
            viewBinding.frameAds.setBackgroundResource(R.drawable.bg_white_round_top)
        }

        val indicators2 = listOf(
            viewBinding.indicatorTopV11,
            viewBinding.indicatorTopV12,
            viewBinding.indicatorTopV13,
            viewBinding.indicatorTopV14,
        )
        indicators2.updateSelectedIndicator(state.introductionPosition)

        val indicators1 = listOf(
            viewBinding.indicatorTopV21,
            viewBinding.indicatorTopV22,
            viewBinding.indicatorTopV23,
            viewBinding.indicatorTopV24,

        )
        indicators1.updateSelectedIndicator(state.introductionPosition)

        val indicators0 = listOf(
            viewBinding.indicatorBottom1,
            viewBinding.indicatorBottom2,
            viewBinding.indicatorBottom3,
            viewBinding.indicatorBottom4
        )
        indicators0.updateSelectedIndicator(state.introductionPosition)

        val actions = pageActions(state)

        viewBinding.tvNextTop.setOnSingleClick {
            actions.onPrimaryAction()
        }

        viewBinding.tvNextTopV1.setOnSingleClick {
            actions.onPrimaryAction()
        }

        viewBinding.tvNextBottom.setOnSingleClick {
            actions.onPrimaryAction()
        }
//        viewBinding.swipe.visibleIf(introductionPosition == 0)
        viewBinding.frameAds.visibleIf(state.isShowAd)

        val initialSpec = OnBoardingV3UiSpec(
            actionText = getString(
                if (state.isPageEnd) {
                    R.string.core_onboarding_action_get_start
                } else {
                    R.string.common_next
                }
            )
        )
        applyUiSpec(
            v3UiCustomizers.applyOnBoardingV3Customizers(
                context = requireContext(),
                state = state,
                initial = initialSpec,
            )
        )
    }

    private fun applyUiSpec(spec: OnBoardingV3UiSpec) = with(viewBinding) {
        spec.titleTextAppearanceRes?.let {
            tvTitle.setTextAppearance(it)
            tvTitleTop.setTextAppearance(it)
        }
        spec.subtitleTextAppearanceRes?.let(tvSubTitleTop::setTextAppearance)

        val actionViews = listOf(tvNextTop, tvNextTopV1, tvNextBottom)
        spec.actionTextAppearanceRes?.let { appearance ->
            actionViews.forEach { it.setTextAppearance(appearance) }
        }

        tvTitle.visibleIf(spec.isTitleVisible)
        tvTitleTop.visibleIf(spec.isTitleVisible)
        tvSubTitleTop.visibleIf(spec.isSubtitleVisible && tvSubTitleTop.text.isNotEmpty())
        spec.titleTextColor?.let {
            tvTitle.setTextColor(it)
            tvTitleTop.setTextColor(it)
        }
        spec.subtitleTextColor?.let(tvSubTitleTop::setTextColor)

        actionViews.forEach { action ->
            action.text = spec.actionText
            spec.actionTextColor?.let(action::setTextColor)
            spec.actionBackgroundRes?.let(action::setBackgroundResource)
        }
        spec.imageBackgroundRes?.let(ivIntroduction::setBackgroundResource)
        spec.adBackgroundRes?.let(frameAds::setBackgroundResource)

        spec.indicatorDrawableRes?.let { background ->
            listOf(
                indicatorTopV11, indicatorTopV12, indicatorTopV13, indicatorTopV14,
                indicatorTopV21, indicatorTopV22, indicatorTopV23, indicatorTopV24,
                indicatorBottom1, indicatorBottom2, indicatorBottom3, indicatorBottom4,
            ).forEach { it.setBackgroundResource(background) }
        }
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

    override fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> {
        if (!isShowAd) return listOf()
        return StartFlowOnBoardingConfigFactory.getOnboardingV3AdPlaceNames(introductionPosition)
    }

    override fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
        super.onBannerNativeResult(adResource)
        latestAdResource = adResource
        dispatchAdResource(adResource)
    }

    private fun dispatchAdResource(adResource: AdLoadBannerNativeUiResource) {
        val placeName = StartFlowOnBoardingConfigFactory
            .getOnboardingV3AdPlaceName(introductionPosition)
        renderedPage?.onBannerNativeResult?.invoke(adResource, placeName)
            ?: if(fullAds) viewBinding.fullNativeBanner.processAdResource(adResource, placeName) else viewBinding.layoutBannerNative.processAdResource(adResource, placeName)
    }

    override fun onDestroyView() {
        renderedPage?.onDispose?.invoke()
        renderedPage = null
        latestAdResource = null
        viewBinding.ivClose.animate().cancel()
        viewBinding.swipe.animate().cancel()
        super.onDestroyView()
    }

    private fun List<View>.updateSelectedIndicator(selectedPosition: Int) {
        val normalWidth = firstOrNull()?.layoutParams?.width ?: return
        val selectedWidth = normalWidth * 2

        forEachIndexed { index, view ->
            val isSelected = index == selectedPosition
            view.isSelected = isSelected
            view.layoutParams = view.layoutParams.apply {
                width = if (isSelected) selectedWidth else normalWidth
            }

            if(index > sharedViewModel.itemsOnboarding.filterIsInstance<OnBoardingItem.Item>().size - 1) {
                view.gone()
            }
        }
    }

}
