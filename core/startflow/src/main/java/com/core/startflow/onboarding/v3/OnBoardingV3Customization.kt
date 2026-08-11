package com.core.startflow.onboarding.v3

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.lifecycle.LifecycleOwner
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.OnBoardingConfig

/** Immutable data that both the default UI and an app-owned renderer can consume. */
data class OnBoardingV3PageState(
    val pageType: OnBoardingV3PageType,
    val introductionPosition: Int,
    val realPosition: Int,
    val pageCount: Int,
    val isPageEnd: Boolean,
    val isShowAd: Boolean,
    val actionPosition: OnBoardingV3ActionPosition,
    @field:DrawableRes val imageRes: Int,
    val title: CharSequence,
    val subtitle: CharSequence?,
    val config: OnBoardingConfig,
)

enum class OnBoardingV3PageType {
    STANDARD,
    END_TAB,
}

enum class OnBoardingV3ActionPosition {
    TOP,
    TOP_CARD,
    BOTTOM,
}

/**
 * Safe customization surface for the core-owned V3 layouts.
 * Null resources/colors retain the value declared by the core layout.
 */
data class OnBoardingV3UiSpec(
    val actionText: CharSequence,
    val isTitleVisible: Boolean = true,
    val isSubtitleVisible: Boolean = true,
    @field:ColorInt val titleTextColor: Int? = null,
    @field:ColorInt val subtitleTextColor: Int? = null,
    @field:ColorInt val actionTextColor: Int? = null,
    @field:DrawableRes val actionBackgroundRes: Int? = null,
    @field:DrawableRes val imageBackgroundRes: Int? = null,
    @field:DrawableRes val adBackgroundRes: Int? = null,
    @field:DrawableRes val indicatorDrawableRes: Int? = null,
    @field:StyleRes val titleTextAppearanceRes: Int? = null,
    @field:StyleRes val subtitleTextAppearanceRes: Int? = null,
    @field:StyleRes val actionTextAppearanceRes: Int? = null,
)

/**
 * Use this for normal branding changes. Customizers run from low to high priority,
 * so a higher-priority customizer can intentionally override an earlier result.
 */
interface OnBoardingV3UiCustomizer {
    val priority: Int
        get() = 0

    fun customize(
        context: Context,
        state: OnBoardingV3PageState,
        current: OnBoardingV3UiSpec,
    ): OnBoardingV3UiSpec = current
}

/** Navigation remains owned by StartFlow even when an app replaces the whole page UI. */
interface OnBoardingV3PageActions {
    fun onPrimaryAction()

    fun onNext()

    fun onFinish()
}

data class OnBoardingV3RenderScope(
    val inflater: LayoutInflater,
    val parent: ViewGroup,
    val lifecycleOwner: LifecycleOwner,
    val state: OnBoardingV3PageState,
    val actions: OnBoardingV3PageActions,
)

/**
 * A complete app-owned page. The returned view must not already have a parent.
 * Use [onBannerNativeResult] to forward ads into an app-owned ad container.
 */
class OnBoardingV3RenderedPage(
    val view: View,
    val onBannerNativeResult: (
        resource: AdLoadBannerNativeUiResource,
        placeName: IAdPlaceName,
    ) -> Unit = { _, _ -> },
    val onDispose: () -> Unit = {},
)

/**
 * Use this only when the core layout cannot represent an app's design.
 * The highest-priority renderer whose [supports] returns true owns the page UI.
 */
interface OnBoardingV3PageRenderer {
    val priority: Int
        get() = 0

    fun supports(state: OnBoardingV3PageState): Boolean = true

    fun render(scope: OnBoardingV3RenderScope): OnBoardingV3RenderedPage
}

fun Set<OnBoardingV3UiCustomizer>.applyOnBoardingV3Customizers(
    context: Context,
    state: OnBoardingV3PageState,
    initial: OnBoardingV3UiSpec,
): OnBoardingV3UiSpec {
    return sortedWith(
        compareBy<OnBoardingV3UiCustomizer> { it.priority }
            .thenBy { it.javaClass.name }
    ).fold(initial) { current, customizer ->
        customizer.customize(context, state, current)
    }
}

fun Set<OnBoardingV3PageRenderer>.activeOnBoardingV3Renderer(
    state: OnBoardingV3PageState,
): OnBoardingV3PageRenderer? {
    return sortedWith(
        compareByDescending<OnBoardingV3PageRenderer> { it.priority }
            .thenBy { it.javaClass.name }
    ).firstOrNull { it.supports(state) }
}

fun OnBoardingConfig.toOnBoardingV3ActionPosition(): OnBoardingV3ActionPosition {
    return when (positionNext) {
        OnBoardingConfig.POSITION_NEXT_TOP_V2 -> OnBoardingV3ActionPosition.TOP_CARD
        OnBoardingConfig.POSITION_NEXT_BOTTOM -> OnBoardingV3ActionPosition.BOTTOM
        else -> OnBoardingV3ActionPosition.TOP
    }
}
