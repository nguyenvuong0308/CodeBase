package com.codebasetemplate.features.feature_demo_native_collapsible.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CollapsibleNativeTestControlsTest {

    @Test
    fun `expand requests explicit expanded state`() {
        val requestedStates = mutableListOf<Boolean>()
        val controls = CollapsibleNativeTestControls(requestedStates::add)

        controls.expand()

        assertEquals(listOf(true), requestedStates)
    }

    @Test
    fun `collapse requests explicit collapsed state`() {
        val requestedStates = mutableListOf<Boolean>()
        val controls = CollapsibleNativeTestControls(requestedStates::add)

        controls.collapse()

        assertEquals(listOf(false), requestedStates)
    }
}
