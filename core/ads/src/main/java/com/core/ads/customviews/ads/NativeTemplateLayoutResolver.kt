package com.core.ads.customviews.ads

import androidx.annotation.LayoutRes
import com.core.ads.R
import com.core.config.domain.data.NativeTemplateSize

@LayoutRes
internal fun resolveNativeShimmerLayout(
    nativeTemplateSize: NativeTemplateSize,
    customLayout: (NativeTemplateSize) -> Int,
): Int = when (nativeTemplateSize) {
    NativeTemplateSize.Medium -> R.layout.gnt_medium_template_view_shimmer
    NativeTemplateSize.MediumCtaRight -> R.layout.gnt_medium_cta_right_shimmer
    NativeTemplateSize.MediumCtaBottom -> R.layout.gnt_medium_cta_bottom_template_view_shimmer
    NativeTemplateSize.MediumShortCtaBottom -> R.layout.gnt_medium_short_cta_bottom_template_view_shimmer
    NativeTemplateSize.MediumCtaRightTop -> R.layout.gnt_medium_cta_right_top_shimmer
    NativeTemplateSize.MediumCtaTop -> R.layout.gnt_medium_cta_top_template_view_shimmer
    NativeTemplateSize.MediumMediaLeft -> R.layout.gnt_medium_media_left_shimmer
    NativeTemplateSize.MediumMediaLeftCtaRight -> R.layout.gnt_medium_media_left_cta_right_shimmer
    NativeTemplateSize.MediumMediaRight -> R.layout.gnt_medium_media_right_shimmer
    NativeTemplateSize.Small -> R.layout.gnt_small_template_view_shimmer
    NativeTemplateSize.SmallCtaTop -> R.layout.gnt_small_cta_top_template_view_shimmer
    NativeTemplateSize.SmallCtaBottom -> R.layout.gnt_small_cta_bottom_template_view_shimmer
    NativeTemplateSize.SmallCtaRight -> R.layout.gnt_small_cta_right_shimmer
    NativeTemplateSize.SmallBannerCtaRight -> R.layout.gnt_small_banner_cta_right_shimmer
    NativeTemplateSize.SmallForPopup -> R.layout.gnt_small_for_popup_template_view_shimmer
    NativeTemplateSize.SmallLong -> R.layout.gnt_small_long_template_view_shimmer
    NativeTemplateSize.MiniCtaRight -> R.layout.gnt_mini_cta_right_shimmer
    NativeTemplateSize.FullCtaBottomOnboarding,
    NativeTemplateSize.FullCtaBottom,
    NativeTemplateSize.FullCtaTop,
    NativeTemplateSize.FullCtaRight,
    NativeTemplateSize.FullInterstitialV1,
    NativeTemplateSize.FullInterstitialV2,
    NativeTemplateSize.FullInterstitialV3,
    -> R.layout.gnt_full_cta_bottom_template_view_shimmer

    is NativeTemplateSize.CustomKey -> customLayout(nativeTemplateSize)
}
