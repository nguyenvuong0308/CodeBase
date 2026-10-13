package com.core.startflow.onboarding.v3

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnBoardingV3PageUserInputTest {
    @Test
    fun `page enables gestures by default when shown`() {
        val applied = mutableListOf<Boolean>()
        val page = OnBoardingV3PageUserInput { applied.add(it) }

        assertTrue(applied.isEmpty())
        page.activate()

        assertEquals(listOf(true), applied)
    }

    @Test
    fun `visible page can disable and reenable gestures`() {
        val applied = mutableListOf<Boolean>()
        val page = OnBoardingV3PageUserInput { applied.add(it) }

        page.activate()
        page.setEnabled(false)
        page.setEnabled(true)

        assertEquals(listOf(true, false, true), applied)
    }

    @Test
    fun `preloaded page applies only its latest setting when shown`() {
        val applied = mutableListOf<Boolean>()
        val page = OnBoardingV3PageUserInput { applied.add(it) }

        page.setEnabled(true)
        page.setEnabled(false)
        assertTrue(applied.isEmpty())

        page.activate()
        assertEquals(listOf(false), applied)
    }

    @Test
    fun `switching pages restores each pages setting without offscreen interference`() {
        var pagerEnabled = true
        val first = OnBoardingV3PageUserInput { pagerEnabled = it }
        val second = OnBoardingV3PageUserInput { pagerEnabled = it }

        first.setEnabled(false)
        first.activate()
        second.setEnabled(true)
        assertFalse(pagerEnabled)

        first.deactivate()
        second.activate()
        assertTrue(pagerEnabled)
        first.setEnabled(false)
        assertTrue(pagerEnabled)

        second.deactivate()
        first.activate()
        assertFalse(pagerEnabled)
    }

    @Test
    fun `paused page remembers changes until resumed`() {
        val applied = mutableListOf<Boolean>()
        val page = OnBoardingV3PageUserInput { applied.add(it) }

        page.activate()
        page.deactivate()
        page.setEnabled(false)
        assertEquals(listOf(true), applied)

        page.activate()
        assertEquals(listOf(true, false), applied)
    }

    @Test
    fun `disposed views cannot change gestures on a new view`() {
        var pagerEnabled = true
        val oldPage = OnBoardingV3PageUserInput { pagerEnabled = it }
        oldPage.activate()
        oldPage.setEnabled(false)
        oldPage.dispose()

        val newPage = OnBoardingV3PageUserInput { pagerEnabled = it }
        newPage.activate()
        oldPage.setEnabled(false)
        oldPage.activate()

        assertTrue(pagerEnabled)
    }
}
