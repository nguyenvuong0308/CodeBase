package com.core.startflow.onboarding.v1

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.lifecycle.LifecycleOwner
import com.core.config.domain.data.OnBoardingConfig

data class OnBoardingV1PageState(
    val position: Int,
    val pageCount: Int,
    val isLastPage: Boolean,
    val isShowAd: Boolean,
    @field:DrawableRes val imageRes: Int,
    val title: CharSequence,
    val subtitle: CharSequence?,
    val config: OnBoardingConfig,
)

/** Null resources/colors retain the value declared by the core V1 layout. */
data class OnBoardingV1UiSpec(
    val actionText: CharSequence,
    val isTitleVisible: Boolean = true,
    val isIndicatorVisible: Boolean = true,
    @field:ColorInt val titleTextColor: Int? = null,
    @field:ColorInt val actionTextColor: Int? = null,
    @field:DrawableRes val rootBackgroundRes: Int? = null,
    @field:DrawableRes val imageBackgroundRes: Int? = null,
    @field:DrawableRes val indicatorContainerBackgroundRes: Int? = null,
    @field:DrawableRes val actionBackgroundRes: Int? = null,
    @field:StyleRes val titleTextAppearanceRes: Int? = null,
    @field:StyleRes val actionTextAppearanceRes: Int? = null,
)

interface OnBoardingV1UiCustomizer {
    val priority: Int
        get() = 0

    fun customize(
        context: Context,
        state: OnBoardingV1PageState,
        current: OnBoardingV1UiSpec,
    ): OnBoardingV1UiSpec = current
}

interface OnBoardingV1PageActions {
    fun onPrimaryAction()
    fun onNext()
    fun onFinish()
}

data class OnBoardingV1RenderScope(
    val inflater: LayoutInflater,
    val parent: ViewGroup,
    val lifecycleOwner: LifecycleOwner,
    val state: OnBoardingV1PageState,
    val actions: OnBoardingV1PageActions,
)

class OnBoardingV1RenderedPage(
    val view: View,
    val onDispose: () -> Unit = {},
)

interface OnBoardingV1PageRenderer {
    val priority: Int
        get() = 0

    fun supports(state: OnBoardingV1PageState): Boolean = true

    fun render(scope: OnBoardingV1RenderScope): OnBoardingV1RenderedPage
}

fun Set<OnBoardingV1UiCustomizer>.applyOnBoardingV1Customizers(
    context: Context,
    state: OnBoardingV1PageState,
    initial: OnBoardingV1UiSpec,
): OnBoardingV1UiSpec {
    return sortedWith(
        compareBy<OnBoardingV1UiCustomizer> { it.priority }
            .thenBy { it.javaClass.name }
    ).fold(initial) { current, customizer ->
        customizer.customize(context, state, current)
    }
}

fun Set<OnBoardingV1PageRenderer>.activeOnBoardingV1Renderer(
    state: OnBoardingV1PageState,
): OnBoardingV1PageRenderer? {
    return sortedWith(
        compareByDescending<OnBoardingV1PageRenderer> { it.priority }
            .thenBy { it.javaClass.name }
    ).firstOrNull { it.supports(state) }
}
