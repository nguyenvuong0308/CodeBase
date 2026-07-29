package com.core.config.domain.data

sealed class NativeAfterInterstitialLoadStrategy {

    abstract val key: String

    companion object {
        fun getBy(key: String): NativeAfterInterstitialLoadStrategy {
            // Config sai hoặc chưa được khai báo sẽ ưu tiên preload sớm cùng interstitial.
            return when (key.trim()) {
                WithInterstitial.key -> WithInterstitial
                OnInterstitialImpression.key -> OnInterstitialImpression
                else -> WithInterstitial
            }
        }
    }

    object WithInterstitial : NativeAfterInterstitialLoadStrategy() {
        override val key = "with_interstitial"
    }

    object OnInterstitialImpression : NativeAfterInterstitialLoadStrategy() {
        override val key = "on_interstitial_impression"
    }
}
