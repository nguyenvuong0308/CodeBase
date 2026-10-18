package com.core.ads.customviews.ads

import kotlin.random.Random

/** One random choice per bound ad; style/lifecycle updates reuse that choice. */
internal class NativeClosePositionResolver(
    private val randomLeft: () -> Boolean = { Random.nextBoolean() },
) {
    private var shuffledLeft: Boolean? = null

    fun beginAd() { shuffledLeft = null }

    fun isLeft(position: String?): Boolean = when (position?.trim()?.lowercase()) {
        "left", "start" -> true
        "shuffle" -> shuffledLeft ?: randomLeft().also { shuffledLeft = it }
        else -> false
    }
}
