package com.codebasetemplate.required.ads

import android.app.Activity
import android.util.Log
import com.codebasetemplate.BuildConfig
import com.codebasetemplate.required.firebase.GetDataFromRemoteUseCaseImpl
import com.core.ads.admob.ReopenAction
import com.core.config.domain.RemoteConfigRepository
import com.core.config.domain.data.AppOpenAdTypeConfig
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ReopenActionImpl"
@Singleton
class ReopenActionImpl @Inject constructor(
    private val remoteConfigRepository: RemoteConfigRepository
): ReopenAction {
    override fun reopenAction(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        Log.d(TAG, "reopenAction: open activity")
//        if (activity is TargetActivity) return
//
//        activity.startActivity(Intent(activity, TargetActivity::class.java))

    }

    override fun isCustomAction(activity: Activity): Boolean {
        return remoteConfigRepository.getAppOpenAdConfig().reopenMode == AppOpenAdTypeConfig.REOPEN_MODE_CUSTOM_ACTIVITY
    }
}