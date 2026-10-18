package com.core.ads.customviews.ads

import android.content.Context
import kotlin.random.Random

/** One draw when creating the ad view; timers and restyling keep that same view. */
internal fun createShuffledNativeInterstitial(
    context: Context,
    nextIndex: () -> Int = { Random.nextInt(3) },
): NativeInterstitialConceptView = when (val index = nextIndex()) {
    0 -> NativeInterstitialV4View(context)
    1 -> NativeInterstitialV5View(context)
    2 -> NativeInterstitialV6View(context)
    else -> error("Template index must be in 0..2, was $index")
}
