package com.codebasetemplate

import com.core.ads.BaseAdmobApplication
import com.core.ads.admob.ReOpenShowCondition
import com.core.preference.PurchasePreferences
import com.core.rate.RateInApp
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : BaseAdmobApplication() {

    @Inject
    lateinit var purchasePreferences: PurchasePreferences

    @Inject
    lateinit var reOpenShowCondition: ReOpenShowCondition

    init {
        instance = this
    }

    override fun initOtherConfig() {
        RateInApp.instance.registerActivityLifecycle(this)
        RateInApp.instance.rateConfig.apply {
            isHideNavigationBar = true
            isHideStatusBar = true
            isSpaceStatusBar = true
            isSpaceDisplayCutout = true
        }
    }

    companion object {

        lateinit var instance: App
    }
}