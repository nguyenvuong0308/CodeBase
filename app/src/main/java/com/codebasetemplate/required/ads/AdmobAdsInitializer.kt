package com.codebasetemplate.required.ads

import android.content.Context
import com.codebasetemplate.R
import com.core.ads.AdsSdkInitializer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdmobAdsInitializer @Inject constructor(@ApplicationContext private val context: Context,): AdsSdkInitializer {
    override fun admobAppId(): String = context.getString(R.string.admob_app_id)

    override fun onUpdateGdprConsent(consentGranted: Boolean?) {

    }

    override fun onAdInitCompleted() {

    }

}
