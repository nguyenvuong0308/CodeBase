package com.core.ads

interface AdsSdkInitializer {
    fun admobAppId(): String

    fun onUpdateGdprConsent(consentGranted: Boolean?)

    fun onAdInitCompleted()
}
