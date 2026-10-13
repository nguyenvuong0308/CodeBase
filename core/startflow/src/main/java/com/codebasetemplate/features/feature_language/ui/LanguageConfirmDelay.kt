package com.codebasetemplate.features.feature_language.ui

/** One instance per Activity: selections only restart the delay until the first reveal. */
internal class LanguageConfirmDelay(
    private val postDelayed: (Runnable, Long) -> Unit,
    private val removeCallbacks: (Runnable) -> Unit,
    private val setVisible: (Boolean) -> Unit,
) {
    private var pending: Runnable? = null
    private var hasBeenShown = false

    fun onLanguageSelected(delayMs: Long) {
        if (hasBeenShown) {
            setVisible(true)
            return
        }
        cancel()
        if (delayMs <= 0) {
            showConfirmation()
            return
        }
        val callback = object : Runnable {
            override fun run() {
                if (pending !== this) return
                pending = null
                showConfirmation()
            }
        }
        pending = callback
        postDelayed(callback, delayMs)
    }

    private fun showConfirmation() {
        hasBeenShown = true
        setVisible(true)
    }

    fun cancel() {
        pending?.let(removeCallbacks)
        pending = null
        setVisible(false)
    }
}
