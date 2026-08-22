package com.codebasetemplate

import com.core.ads.BaseAdmobApplication
import com.core.ads.admob.ReOpenShowCondition
import com.core.rate.RateInApp
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : BaseAdmobApplication() {

    @Inject
    lateinit var reOpenShowConditionLazy: Lazy<ReOpenShowCondition>

    val reOpenShowCondition: ReOpenShowCondition
        get() = reOpenShowConditionLazy.get()

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