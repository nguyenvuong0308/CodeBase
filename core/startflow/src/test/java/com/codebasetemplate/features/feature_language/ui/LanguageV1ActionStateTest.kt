package com.codebasetemplate.features.feature_language.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageV1ActionStateTest {
    @Test
    fun `always show reveals both action modes before selection and skips subsequent delays`() {
        for (useText in listOf(false, true)) {
            var hasSelectedLanguage = false
            var state = resolveLanguageV1ActionState(useText, ready = false, hasSelectedLanguage)
            val delay = LanguageConfirmDelay(
                postDelayed = { _, _ -> error("Always show must not schedule a delay") },
                removeCallbacks = {},
                setVisible = { state = resolveLanguageV1ActionState(useText, it, hasSelectedLanguage) },
            )
            delay.initialize(alwaysShow = true)
            assertFalse(state.canConfirm)
            assertFalse(state.enableIcon)
            assertFalse(state.enableText)
            assertEquals(!useText, state.showIcon)
            assertEquals(useText, state.showText)
            repeat(3) {
                hasSelectedLanguage = true
                delay.onLanguageSelected(3_000)
                assertTrue(state.canConfirm)
                assertEquals(!useText, state.enableIcon)
                assertEquals(useText, state.enableText)
                assertEquals(!useText, state.showIcon)
                assertEquals(useText, state.showText)
            }
            delay.cancel()
            assertFalse(state.canConfirm)
            assertFalse(state.showIcon)
            assertFalse(state.showText)
            assertFalse(state.enableIcon)
            assertFalse(state.enableText)
        }
    }

    @Test
    fun `only a visible action with a selected language is enabled`() {
        for (useText in listOf(false, true)) {
            for (ready in listOf(false, true)) {
                for (selected in listOf(false, true)) {
                    val state = resolveLanguageV1ActionState(useText, ready, selected)
                    assertEquals(ready && !useText, state.showIcon)
                    assertEquals(ready && useText, state.showText)
                    assertEquals(ready && selected, state.canConfirm)
                    assertEquals(ready && selected && !useText, state.enableIcon)
                    assertEquals(ready && selected && useText, state.enableText)
                }
            }
        }
    }

    @Test
    fun `always show enables a preselected settings language immediately`() {
        for (useText in listOf(false, true)) {
            var state = resolveLanguageV1ActionState(useText, ready = false, hasSelectedLanguage = true)
            val delay = LanguageConfirmDelay(
                postDelayed = { _, _ -> error("Preselected language must not wait in always show mode") },
                removeCallbacks = {},
                setVisible = { state = resolveLanguageV1ActionState(useText, it, hasSelectedLanguage = true) },
            )
            delay.initialize(alwaysShow = true)
            assertTrue(state.canConfirm)
            assertEquals(!useText, state.enableIcon)
            assertEquals(useText, state.enableText)
        }
    }

    @Test
    fun `neither action can confirm while waiting in either mode`() {
        for (useText in listOf(false, true)) {
            val state = resolveLanguageV1ActionState(useText, ready = false, hasSelectedLanguage = true)
            assertFalse(state.showIcon)
            assertFalse(state.showText)
            assertFalse(state.canConfirm)
        }
    }

    @Test
    fun `ready action displays only the configured mode and can confirm`() {
        for (useText in listOf(false, true)) {
            val state = resolveLanguageV1ActionState(useText, ready = true, hasSelectedLanguage = true)
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
            var state = resolveLanguageV1ActionState(useText, ready = false, hasSelectedLanguage = true)
            val delay = LanguageConfirmDelay(
                postDelayed = { callback, milliseconds -> callbacks[callback] = now + milliseconds },
                removeCallbacks = { callbacks.remove(it) },
                setVisible = { state = resolveLanguageV1ActionState(useText, it, hasSelectedLanguage = true) },
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
                setVisible = { state = resolveLanguageV1ActionState(useText, it, hasSelectedLanguage = true) },
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
            var state = resolveLanguageV1ActionState(useText, ready = false, hasSelectedLanguage = true)
            val delay = LanguageConfirmDelay(
                postDelayed = { _, _ -> error("Zero delay must not schedule a callback") },
                removeCallbacks = {},
                setVisible = { state = resolveLanguageV1ActionState(useText, it, hasSelectedLanguage = true) },
            )
            delay.onLanguageSelected(0)
            assertTrue(state.canConfirm)
            assertEquals(useText, state.showText)
            assertEquals(!useText, state.showIcon)
        }
    }
}
