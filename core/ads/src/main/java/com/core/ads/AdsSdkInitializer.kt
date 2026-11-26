package com.core.ads

interface AdsSdkInitializer {
    /** Optional EEA testing configuration, shared by initial consent and retry requests. */
    val consentDebugConfig: ConsentDebugConfig
        get() = ConsentDebugConfig()

    /** App-provided device classification. Set before starting the ads flow. */
    val isLowDevice: Boolean
        get() = false

    fun admobAppId(): String

    fun onUpdateGdprConsent(consentGranted: Boolean?)

    fun onAdInitCompleted()
}
