package com.core.config.domain.data

sealed class NativeTemplateSize {

    abstract val key: String

    companion object {

        val builtInTemplates: List<NativeTemplateSize> = listOf(
            Small,
            SmallCtaTop,
            SmallCtaBottom,
            SmallCtaRight,
            SmallBannerCtaRight,
            MiniCtaRight,
            SmallLong,
            SmallForPopup,
            Medium,
            MediumCtaTop,
            MediumCtaBottom,
            MediumShortCtaBottom,
            MediumCtaRightTop,
            MediumCtaRight,
            MediumMediaRight,
            MediumMediaLeft,
            MediumMediaCtaRight,
            MediumMediaLeftCtaRight,
            LargeMediaCtaRight,
            FullCtaBottom,
            FullCtaBottomOnboarding,
            FullCtaTop,
            FullCtaRight,
            FullInterstitialV1,
            FullInterstitialV2,
            FullInterstitialV3,
        )

        fun getSizeBy(key: String): NativeTemplateSize {
            return builtInTemplates.firstOrNull { it.key == key } ?: CustomKey(key)
        }
    }

    object Small : NativeTemplateSize() {
        override val key = "small"
    }

    object SmallCtaTop : NativeTemplateSize() {
        override val key = "small_cta_top"
    }

    object SmallCtaBottom : NativeTemplateSize() {
        override val key = "small_cta_bottom"
    }

    object SmallCtaRight: NativeTemplateSize() {
        override val key = "small_cta_right"
    }

    object SmallBannerCtaRight : NativeTemplateSize() {
        override val key = "small_banner_cta_right"
    }

    object MiniCtaRight: NativeTemplateSize() {
        override val key = "mini_cta_right"
    }

    object SmallLong : NativeTemplateSize() {
        override val key = "small_long"
    }

    object SmallForPopup : NativeTemplateSize() {
        override val key = "small_for_popup"
    }

    object Medium : NativeTemplateSize() {
        override val key = "medium"
    }

    object MediumCtaTop : NativeTemplateSize() {
        override val key = "medium_cta_top"
    }

    object MediumCtaBottom : NativeTemplateSize() {
        override val key = "medium_cta_bottom"
    }

    object MediumShortCtaBottom : NativeTemplateSize() {
        override val key = "medium_short_cta_bottom"
    }

    object MediumCtaRightTop: NativeTemplateSize() {
        override val key = "medium_cta_right_top"
    }

    object MediumCtaRight: NativeTemplateSize() {
        override val key = "medium_cta_right"
    }

    object MediumMediaRight: NativeTemplateSize() {
        override val key = "medium_media_right"
    }

    object MediumMediaLeft: NativeTemplateSize() {
        override val key = "medium_media_left"
    }

    object MediumMediaCtaRight : NativeTemplateSize() {
        override val key = "medium_media_cta_right"
    }

    object MediumMediaLeftCtaRight : NativeTemplateSize() {
        override val key = "medium_media_left_cta_right"
    }

    object LargeMediaCtaRight : NativeTemplateSize() {
        override val key = "large_media_cta_right"
    }

    object FullCtaBottom: NativeTemplateSize() {
        override val key = "full_cta_bottom"
    }

    object FullCtaBottomOnboarding: NativeTemplateSize() {
        override val key = "full_cta_bottom_onboarding"
    }

    object FullCtaTop: NativeTemplateSize() {
        override val key = "full_cta_top"
    }

    object FullCtaRight: NativeTemplateSize() {
        override val key = "full_cta_right"
    }

    object FullInterstitialV1: NativeTemplateSize() {
        override val key = "full_interstitial_v1"
    }

    object FullInterstitialV2: NativeTemplateSize() {
        override val key = "full_interstitial_v2"
    }

    object FullInterstitialV3: NativeTemplateSize() {
        override val key = "full_interstitial_v3"
    }

    class CustomKey(val customKey: String) : NativeTemplateSize() {
        override val key = "custom"
    }
}
