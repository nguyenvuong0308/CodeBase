package com.codebasetemplate.required.ads

import android.app.Activity
import android.util.Log
import com.core.ads.admob.ReopenAction
import com.core.ads.domain.AdsManager
import com.core.config.domain.RemoteConfigRepository
import com.core.config.domain.data.AppOpenAdTypeConfig
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ReopenActionImpl"

/**
 * Optional app-owned reopen flow.
 *
 * This implementation only replaces the core default after the app binds it to [ReopenAction]
 * from a Hilt module.
 */
@Singleton
class ReopenActionImpl @Inject constructor(
    private val remoteConfigRepository: RemoteConfigRepository,
    private val adManager: AdsManager
): ReopenAction {
    /**
     * Opens the application-owned reopen screen when the current Activity is still usable.
     * The actual destination is intentionally left for each application to provide.
     */
    override fun reopenAction(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        Log.d(TAG, "reopenAction: open activity")
//        if (activity is TargetActivity) return
//
//        activity.startActivity(Intent(activity, TargetActivity::class.java))

    }

    /**
     * Selects between the application's custom reopen flow and the standard app-open ad.
     * This template currently disables the custom flow until its remote-config rule is enabled.
     */
    override fun isCustomAction(activity: Activity): Boolean {
//        return remoteConfigRepository.getAppOpenAdConfig().reopenMode == AppOpenAdTypeConfig.REOPEN_MODE_CUSTOM_ACTIVITY
        return false
    }

    /**
     * Hook for preloading the fullscreen placement used by the custom reopen flow.
     * It remains inactive until the host application supplies its placement name.
     */
    override fun loadAdFull(currentActivity: Activity) {
//        adManager.loadFullscreenAd(activity = currentActivity, adPlaceName = AppAdPlaceName.FULLSCREEN_WELLCOM_BACK_REOPEN, identifier = "")
    }
}
