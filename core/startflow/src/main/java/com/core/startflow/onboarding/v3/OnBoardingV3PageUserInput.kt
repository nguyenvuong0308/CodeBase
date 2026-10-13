package com.core.startflow.onboarding.v3

/** Keeps preloaded pages from changing the visible page's gesture setting. */
internal class OnBoardingV3PageUserInput(
    private val applyEnabled: (Boolean) -> Unit,
) {
    private var enabled = true
    private var active = false
    private var disposed = false

    fun setEnabled(enabled: Boolean) {
        if (disposed) return
        this.enabled = enabled
        if (active) applyEnabled(enabled)
    }

    fun activate() {
        if (disposed) return
        active = true
        applyEnabled(enabled)
    }

    fun deactivate() {
        active = false
    }

    fun dispose() {
        deactivate()
        disposed = true
    }
}
