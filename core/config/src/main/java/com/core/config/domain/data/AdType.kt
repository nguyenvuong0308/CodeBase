package com.core.config.domain.data

sealed class AdType {

    abstract val key: String

    companion object {
        fun getAdTypeBy(key: String) = when (key) {
            Banner.key -> Banner
            Native.key -> Native
            NativeInterstitial.key -> NativeInterstitial
            Interstitial.key -> Interstitial
            RewardedInterstitial.key -> RewardedInterstitial
            RewardedVideo.key -> RewardedVideo
            AppOpen.key -> AppOpen
            else -> None
        }
    }

    object Banner : AdType() {
        override val key = "banner"
    }

    object Native : AdType() {
        override val key = "native"
    }

    object NativeInterstitial : AdType() {
        override val key = "native_interstitial"
    }

    object Interstitial : AdType() {
        override val key = "interstitial"
    }

    object RewardedInterstitial : AdType() {
        override val key = "rewarded_interstitial"
    }

    object RewardedVideo : AdType() {
        override val key = "rewarded"
    }

    object AppOpen : AdType() {
        override val key = "app_open"
    }

    object None : AdType() {
        override val key = ""
    }

}
