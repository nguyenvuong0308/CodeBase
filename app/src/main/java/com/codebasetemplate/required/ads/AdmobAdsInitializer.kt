package com.codebasetemplate.required.ads

import android.content.Context
import android.util.Log
import com.codebasetemplate.R
import com.core.ads.AdsSdkInitializer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdmobAdsInitializer @Inject constructor(@ApplicationContext private val context: Context,): AdsSdkInitializer {
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
