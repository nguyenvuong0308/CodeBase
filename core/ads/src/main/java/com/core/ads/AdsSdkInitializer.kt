package com.core.ads

interface AdsSdkInitializer {
    /** App-provided device classification. Set before starting the ads flow. */
    val isLowDevice: Boolean
        get() = false

    fun admobAppId(): String

    fun onUpdateGdprConsent(consentGranted: Boolean?)

    fun onAdInitCompleted()
}
