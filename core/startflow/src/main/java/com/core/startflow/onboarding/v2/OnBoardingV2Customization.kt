package com.core.startflow.onboarding.v2

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.lifecycle.LifecycleOwner
import com.core.config.domain.data.OnBoardingConfig

data class OnBoardingV2PageState(
    val position: Int,
    val pageCount: Int,
    val isLastPage: Boolean,
    val isShowAd: Boolean,
    @field:DrawableRes val imageRes: Int,
    val title: CharSequence,
    val subtitle: CharSequence?,
    val config: OnBoardingConfig,
)

/** Null resources/colors retain the value declared by the core V2 layout. */
data class OnBoardingV2UiSpec(
    val actionText: CharSequence,
    val isTitleVisible: Boolean = true,
    val isSubtitleVisible: Boolean = true,
    val isActionFillGradientEnabled: Boolean? = null,
    val actionFillGradientColors: List<Int>? = null,
    @field:ColorInt val titleTextColor: Int? = null,
    @field:ColorInt val subtitleTextColor: Int? = null,
    @field:ColorInt val actionTextColor: Int? = null,
    @field:DrawableRes val rootBackgroundRes: Int? = null,
    @field:DrawableRes val imageBackgroundRes: Int? = null,
    @field:DrawableRes val contentBackgroundRes: Int? = null,
    @field:DrawableRes val actionBackgroundRes: Int? = null,
    @field:StyleRes val titleTextAppearanceRes: Int? = null,
    @field:StyleRes val subtitleTextAppearanceRes: Int? = null,
    @field:StyleRes val actionTextAppearanceRes: Int? = null,
)

interface OnBoardingV2UiCustomizer {
    val priority: Int
        get() = 0

    fun customize(
        context: Context,
        state: OnBoardingV2PageState,
        current: OnBoardingV2UiSpec,
    ): OnBoardingV2UiSpec = current
}

interface OnBoardingV2PageActions {
    fun onPrimaryAction()
    fun onNext()
    fun onFinish()
}

data class OnBoardingV2RenderScope(
    val inflater: LayoutInflater,
    val parent: ViewGroup,
    val lifecycleOwner: LifecycleOwner,
    val state: OnBoardingV2PageState,
    val actions: OnBoardingV2PageActions,
)

class OnBoardingV2RenderedPage(
    val view: View,
    val onDispose: () -> Unit = {},
)

interface OnBoardingV2PageRenderer {
    val priority: Int
        get() = 0

    fun supports(state: OnBoardingV2PageState): Boolean = true

    fun render(scope: OnBoardingV2RenderScope): OnBoardingV2RenderedPage
}

fun Set<OnBoardingV2UiCustomizer>.applyOnBoardingV2Customizers(
    context: Context,
    state: OnBoardingV2PageState,
    initial: OnBoardingV2UiSpec,
): OnBoardingV2UiSpec {
    return sortedWith(
        compareBy<OnBoardingV2UiCustomizer> { it.priority }
            .thenBy { it.javaClass.name }
    ).fold(initial) { current, customizer ->
        customizer.customize(context, state, current)
    }
}

fun Set<OnBoardingV2PageRenderer>.activeOnBoardingV2Renderer(
    state: OnBoardingV2PageState,
): OnBoardingV2PageRenderer? {
    return sortedWith(
        compareByDescending<OnBoardingV2PageRenderer> { it.priority }
            .thenBy { it.javaClass.name }
    ).firstOrNull { it.supports(state) }
}
