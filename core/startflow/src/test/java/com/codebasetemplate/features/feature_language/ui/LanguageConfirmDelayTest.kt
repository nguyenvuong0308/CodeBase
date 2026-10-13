package com.codebasetemplate.features.feature_language.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageConfirmDelayTest {
    private var now = 0L
    private var visible = false
    private val visibilityChanges = mutableListOf<Boolean>()
    private val callbacks = linkedMapOf<Runnable, Long>()
    private val delay = LanguageConfirmDelay(
        postDelayed = { callback, milliseconds -> callbacks[callback] = now + milliseconds },
        removeCallbacks = { callbacks.remove(it) },
        setVisible = {
            visible = it
            visibilityChanges.add(it)
        },
    )

    @Test
    fun `confirm remains hidden until three seconds after selection`() {
        delay.onLanguageSelected(3_000)
        advanceBy(2_999)
        assertFalse(visible)
        advanceBy(1)
        assertTrue(visible)
        assertTrue(callbacks.isEmpty())
    }

    @Test
    fun `before first reveal each selection restarts the delay and stale callback cannot reveal confirm`() {
        delay.onLanguageSelected(3_000)
        val staleCallback = callbacks.keys.single()
        advanceBy(2_000)
        delay.onLanguageSelected(3_000)
        assertEquals(1, callbacks.size)
        staleCallback.run()
        assertFalse(visible)
        advanceBy(1_000)
        assertFalse(visible)
        advanceBy(1_999)
        assertFalse(visible)
        advanceBy(1)
        assertTrue(visible)
    }

    @Test
    fun `selections after first reveal never hide confirm or schedule another delay`() {
        delay.onLanguageSelected(3_000)
        advanceBy(3_000)
        assertTrue(visible)
        visibilityChanges.clear()
        repeat(3) {
            delay.onLanguageSelected(3_000)
            assertTrue(visible)
            assertTrue(callbacks.isEmpty())
        }
        assertFalse(visibilityChanges.contains(false))
    }

    @Test
    fun `zero delay reveals immediately and further selections stay visible`() {
        delay.onLanguageSelected(0)
        assertTrue(visible)
        assertTrue(callbacks.isEmpty())
        delay.onLanguageSelected(1_200)
        assertTrue(visible)
        assertTrue(callbacks.isEmpty())
    }

    @Test
    fun `custom delay is respected before first reveal`() {
        delay.onLanguageSelected(1_200)
        advanceBy(1_199)
        assertFalse(visible)
        advanceBy(1)
        assertTrue(visible)
    }

    @Test
    fun `new activity instance must wait again after previous instance revealed confirm`() {
        delay.onLanguageSelected(3_000)
        advanceBy(3_000)
        assertTrue(visible)
        delay.cancel()

        val newActivityDelay = LanguageConfirmDelay(
            postDelayed = { callback, milliseconds -> callbacks[callback] = now + milliseconds },
            removeCallbacks = { callbacks.remove(it) },
            setVisible = { visible = it },
        )
        newActivityDelay.onLanguageSelected(3_000)
        assertFalse(visible)
        advanceBy(2_999)
        assertFalse(visible)
        advanceBy(1)
        assertTrue(visible)
    }

    @Test
    fun `cancel on destroy or apply removes callback and keeps confirm hidden`() {
        delay.onLanguageSelected(3_000)
        val staleCallback = callbacks.keys.single()
        delay.cancel()
        assertTrue(callbacks.isEmpty())
        staleCallback.run()
        advanceBy(3_000)
        assertFalse(visible)
    }

    private fun advanceBy(milliseconds: Long) {
        now += milliseconds
        callbacks.filterValues { it <= now }.keys.toList().forEach {
            callbacks.remove(it)
            it.run()
        }
    }
}
