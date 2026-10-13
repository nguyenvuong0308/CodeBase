package com.codebasetemplate.features.feature_language.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageV1ActionStateTest {
    @Test
    fun `neither action can confirm while waiting in either mode`() {
        for (useText in listOf(false, true)) {
            val state = resolveLanguageV1ActionState(useText, ready = false)
            assertFalse(state.showIcon)
            assertFalse(state.showText)
            assertFalse(state.canConfirm)
        }
    }

    @Test
    fun `ready action displays only the configured mode and can confirm`() {
        for (useText in listOf(false, true)) {
            val state = resolveLanguageV1ActionState(useText, ready = true)
            assertEquals(!useText, state.showIcon)
            assertEquals(useText, state.showText)
            assertTrue(state.canConfirm)
        }
    }

    @Test
    fun `both modes stay ready after first reveal until cancelled and a new instance waits again`() {
        for (useText in listOf(false, true)) {
            var now = 0L
            val callbacks = linkedMapOf<Runnable, Long>()
            var state = resolveLanguageV1ActionState(useText, ready = false)
            val delay = LanguageConfirmDelay(
                postDelayed = { callback, milliseconds -> callbacks[callback] = now + milliseconds },
                removeCallbacks = { callbacks.remove(it) },
                setVisible = { state = resolveLanguageV1ActionState(useText, it) },
            )
            fun advanceBy(milliseconds: Long) {
                now += milliseconds
                callbacks.filterValues { it <= now }.keys.toList().forEach {
                    callbacks.remove(it)
                    it.run()
                }
            }

            delay.onLanguageSelected(3_000)
            advanceBy(2_000)
            assertFalse(state.canConfirm)
            delay.onLanguageSelected(3_000)
            advanceBy(2_999)
            assertFalse(state.canConfirm)
            advanceBy(1)
            assertTrue(state.canConfirm)
            assertEquals(useText, state.showText)
            assertEquals(!useText, state.showIcon)

            delay.onLanguageSelected(3_000)
            assertTrue(state.canConfirm)
            assertEquals(useText, state.showText)
            assertEquals(!useText, state.showIcon)
            assertTrue(callbacks.isEmpty())
            delay.cancel()
            advanceBy(3_000)
            assertFalse(state.canConfirm)

            val newActivityDelay = LanguageConfirmDelay(
                postDelayed = { callback, milliseconds -> callbacks[callback] = now + milliseconds },
                removeCallbacks = { callbacks.remove(it) },
                setVisible = { state = resolveLanguageV1ActionState(useText, it) },
            )
            newActivityDelay.onLanguageSelected(3_000)
            assertFalse(state.canConfirm)
            advanceBy(2_999)
            assertFalse(state.canConfirm)
            advanceBy(1)
            assertTrue(state.canConfirm)
            assertEquals(useText, state.showText)
            assertEquals(!useText, state.showIcon)
        }
    }

    @Test
    fun `zero delay enables the configured action immediately`() {
        for (useText in listOf(false, true)) {
            var state = resolveLanguageV1ActionState(useText, ready = false)
            val delay = LanguageConfirmDelay(
                postDelayed = { _, _ -> error("Zero delay must not schedule a callback") },
                removeCallbacks = {},
                setVisible = { state = resolveLanguageV1ActionState(useText, it) },
            )
            delay.onLanguageSelected(0)
            assertTrue(state.canConfirm)
            assertEquals(useText, state.showText)
            assertEquals(!useText, state.showIcon)
        }
    }
}
