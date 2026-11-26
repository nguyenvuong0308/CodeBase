package com.codebasetemplate.required.ads

import android.content.Context
import android.util.Log
import com.codebasetemplate.R
import com.core.ads.AdsSdkInitializer
import com.core.ads.ConsentDebugConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdmobAdsInitializer @Inject constructor(@ApplicationContext private val context: Context,): AdsSdkInitializer {
    // Set forceEea to true to test the European consent flow in a debug build.
    // For a physical device, paste the UMP hashed device ID printed in Logcat below.
    // Emulators need no ID. Core ignores these settings in non-debuggable builds.
    override val consentDebugConfig = ConsentDebugConfig(
        forceEea = false,
        testDeviceHashedIds = listOf(
            // "YOUR_UMP_TEST_DEVICE_HASHED_ID",
        ),
    )

    // Set from the app's device classification before starting the ads flow.
    @Volatile
    override var isLowDevice: Boolean = false
        set(value) {
            if (field == value) return
            val previousValue = field
            field = value
            Log.d("AdmobManager", "LowDeviceAds: isLowDevice changed from $previousValue to $value")
        }

    override fun admobAppId(): String = context.getString(R.string.admob_app_id)

    override fun onUpdateGdprConsent(consentGranted: Boolean?) {

    }

    override fun onAdInitCompleted() {

    }

}
