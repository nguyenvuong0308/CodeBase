package com.core.ads.customviews.ads

import kotlin.random.Random

/** Resolve once per ad bind, independently of the Close position shuffle. */
internal class NativeThemeResolver(private val chooseDark: () -> Boolean = { Random.nextBoolean() }) {
    private var shuffled: String? = null

    fun beginAd() { shuffled = null }

    fun resolve(mode: String?): String? = when (mode?.trim()?.lowercase()) {
        "dark" -> "dark"
        "light" -> "light"
        "shuffle" -> shuffled ?: (if (chooseDark()) "dark" else "light").also { shuffled = it }
        else -> null
    }
}
