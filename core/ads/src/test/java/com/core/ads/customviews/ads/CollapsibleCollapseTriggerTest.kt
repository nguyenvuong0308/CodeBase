package com.core.ads.customviews.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollapsibleCollapseTriggerTest {

    @Test
    fun `detaching anchor does not rebind native ad`() {
        assertFalse(CollapsibleCollapseTrigger.ANCHOR_DETACHED.shouldRebindNativeAd)
    }

    @Test
    fun `hiding anchor still rebinds native ad`() {
        assertTrue(CollapsibleCollapseTrigger.ANCHOR_HIDDEN.shouldRebindNativeAd)
    }

    @Test
    fun `refreshing native ad still rebinds current inline ad`() {
        assertTrue(CollapsibleCollapseTrigger.NATIVE_REFRESH_STARTED.shouldRebindNativeAd)
    }
}
