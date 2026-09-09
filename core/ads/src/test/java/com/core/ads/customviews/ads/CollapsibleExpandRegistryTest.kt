package com.core.ads.customviews.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollapsibleExpandRegistryTest {

    @Test
    fun `expanded item is remembered for the same container`() {
        val registry = CollapsibleExpandRegistry<Any>()
        val containerIdentity = Any()
        val expandedItem = Any()
        val otherItem = Any()

        registry.markExpanded(expandedItem, containerIdentity)

        assertTrue(registry.hasExpanded(expandedItem, containerIdentity))
        assertFalse(registry.hasExpanded(otherItem, containerIdentity))
    }

    @Test
    fun `same ad can expand again in a different container`() {
        val registry = CollapsibleExpandRegistry<Any>()
        val firstContainerIdentity = Any()
        val nextContainerIdentity = Any()
        val reusedAd = Any()

        registry.markExpanded(reusedAd, firstContainerIdentity)

        assertTrue(registry.hasExpanded(reusedAd, firstContainerIdentity))
        assertFalse(registry.hasExpanded(reusedAd, nextContainerIdentity))
    }

    @Test
    fun `cooldown is active only before its expiry boundary`() {
        val registry = CollapsibleExpandRegistry<Any>()
        registry.markClosed(key = "native_home", nowMillis = 1_000L)

        assertTrue(
            registry.isCooldownActive(
                key = "native_home",
                cooldownMillis = 2_000L,
                nowMillis = 2_999L,
            ),
        )
        assertFalse(
            registry.isCooldownActive(
                key = "native_home",
                cooldownMillis = 2_000L,
                nowMillis = 3_000L,
            ),
        )
    }

    @Test
    fun `cooldown is isolated by ad place key`() {
        val registry = CollapsibleExpandRegistry<Any>()
        registry.markClosed(key = "native_home", nowMillis = 1_000L)

        assertFalse(
            registry.isCooldownActive(
                key = "native_settings",
                cooldownMillis = 2_000L,
                nowMillis = 1_500L,
            ),
        )
    }
}
