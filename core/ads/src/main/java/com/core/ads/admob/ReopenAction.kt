package com.core.ads.admob

import android.app.Activity

interface ReopenAction {
    fun reopenAction(activity: Activity)
    fun isCustomAction(): Boolean
}