package com.core.ads.customviews.ads

import org.junit.Assert.*
import org.junit.Test

class NativeClosePositionResolverTest {
    @Test fun `shuffle supports both random outcomes and chooses only once per ad`() {
        for (left in listOf(false, true)) {
            var calls = 0
            val resolver = NativeClosePositionResolver { calls++; left }
            resolver.beginAd()
            repeat(10) { assertEquals(left, resolver.isLeft("shuffle")) }
            assertEquals(1, calls)
        }
    }

    @Test fun `new ad selects again and may repeat previous side`() {
        val choices = listOf(true, false, false).iterator()
        val resolver = NativeClosePositionResolver { choices.next() }
        for (expected in listOf(true, false, false)) {
            resolver.beginAd()
            repeat(3) { assertEquals(expected, resolver.isLeft(" SHUFFLE ")) }
        }
        assertFalse(choices.hasNext())
    }

    @Test fun `fixed positions aliases and invalid fallback never use randomness`() {
        val resolver = NativeClosePositionResolver { error("Fixed position must not use randomness") }
        listOf("left", "start", " LEFT ").forEach { assertTrue(resolver.isLeft(it)) }
        listOf(null, "", "invalid", "right", "end", " RIGHT ").forEach { assertFalse(resolver.isLeft(it)) }
    }

    @Test fun `temporary fixed override does not reroll shuffle for the same ad`() {
        var calls = 0
        val resolver = NativeClosePositionResolver { calls++; true }
        assertTrue(resolver.isLeft("shuffle"))
        assertFalse(resolver.isLeft("right"))
        assertTrue(resolver.isLeft("shuffle"))
        assertEquals(1, calls)
    }
}
