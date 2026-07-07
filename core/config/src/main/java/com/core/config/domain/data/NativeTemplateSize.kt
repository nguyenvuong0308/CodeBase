package com.core.config.domain.data

sealed class NativeTemplateSize {

    abstract val key: String

    companion object {

        fun getSizeBy(key: String): NativeTemplateSize {
            return when(key) {
                Small.key -> Small
                SmallCtaTop.key -> SmallCtaTop
                SmallCtaRight.key -> SmallCtaRight
                SmallLong.key -> SmallLong
                SmallForPopup.key -> SmallForPopup

                Medium.key -> Medium
                MediumCtaTop.key -> MediumCtaTop
                MediumCtaRightTop.key -> MediumCtaRightTop
                MediumCollapsibleCtaBottom.key -> MediumCollapsibleCtaBottom
                MediumCtaRight.key -> MediumCtaRight
                MediumMediaRight.key -> MediumMediaRight
                MediumMediaLeft.key -> MediumMediaLeft

                FullCtaBottom.key -> FullCtaBottom
                FullCtaBottomOnboarding.key -> FullCtaBottomOnboarding
                FullCtaTop.key -> FullCtaTop
                FullCtaRight.key -> FullCtaRight
                MediumCtaBottom.key -> MediumCtaBottom
                SmallCtaBottom.key -> SmallCtaBottom
                MiniCtaRight.key -> MiniCtaRight
                FullInterstitialV1.key -> FullInterstitialV1
                FullInterstitialV2.key -> FullInterstitialV2
                else -> CustomKey(key)
            }
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

    object MediumCollapsibleCtaBottom : NativeTemplateSize() {
        override val key = "medium_collapsible_cta_bottom"
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

    class CustomKey(val customKey: String) : NativeTemplateSize() {
        override val key = "custom"
    }
}