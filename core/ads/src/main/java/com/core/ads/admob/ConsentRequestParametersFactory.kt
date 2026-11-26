package com.core.ads.admob

import android.content.Context
import com.core.ads.AdsSdkInitializer
import com.core.utilities.isDebug
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentRequestParameters

internal fun AdsSdkInitializer.createConsentRequestParameters(context: Context): ConsentRequestParameters {
    val builder = ConsentRequestParameters.Builder()
        .setTagForUnderAgeOfConsent(false)
    if (context.isDebug()) {
        val config = consentDebugConfig
        if (config.forceEea) {
            val debugSettings = ConsentDebugSettings.Builder(context)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            config.testDeviceHashedIds.forEach { debugSettings.addTestDeviceHashedId(it) }
            builder.setConsentDebugSettings(debugSettings.build())
        }
    }
    return builder.build()
}
