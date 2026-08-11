package com.core.ads.admob

import android.app.Activity

/**
 * Defines app-specific behavior for the transition between background and foreground.
 *
 * The ads module uses this abstraction so it does not need to know which Activity or
 * fullscreen placement is supplied by the host application.
 */
interface ReopenAction {
    /** Opens the host application's custom reopen destination, when one is configured. */
    fun reopenAction(activity: Activity)

    /** Returns whether the host application handles reopen with its custom flow. */
    fun isCustomAction(activity: Activity): Boolean

    /** Starts loading the fullscreen ad required by the custom reopen flow. */
    fun loadAdFull(currentActivity: Activity)
}

/** Default behavior used when the host application does not provide a custom reopen flow. */
internal object DefaultReopenAction : ReopenAction {
    override fun reopenAction(activity: Activity) = Unit

    override fun isCustomAction(activity: Activity): Boolean = false

    override fun loadAdFull(currentActivity: Activity) = Unit
}
