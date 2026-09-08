package com.core.ads.admob

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdEventNameTest {

    @Test
    fun `dismiss action creates placement dismissed event`() {
        assertEquals(
            "dialog_interstitial_Dismissed",
            buildAdEventName("dialog_interstitial", getDismissAction(isNative = false)),
        )
    }

    @Test
    fun `native dismiss action adds native prefix`() {
        assertEquals(
            "dialog_interstitial_Native_Dismissed",
            buildAdEventName("dialog_interstitial", getDismissAction(isNative = true)),
        )
    }

    @Test
    fun `dismiss event keeps firebase name valid and within limit`() {
        val eventName = buildAdEventName(
            baseName = "prefix/reward-interstitial-name-that-is-longer-than-limit",
            action = "Dismissed",
        )

        assertEquals(40, eventName.length)
        assertTrue(eventName.matches(Regex("[A-Za-z0-9_]+")))
        assertTrue(eventName.endsWith("_Dismissed"))
    }

    @Test
    fun `existing show and click event names stay unchanged`() {
        assertEquals("reward_Showed", buildAdEventName("reward", "Showed"))
        assertEquals("reward_Clicked", buildAdEventName("reward", "Clicked"))
    }
}
