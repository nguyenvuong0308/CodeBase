package com.codebasetemplate.util

import android.os.Bundle
import android.util.Log

object EventTracking {
    const val VALUE_CLICK = "click"
    const val VALUE_SWIPE = "swipe"
    const val VALUE_FIRST_VIEW = "first_view"
    const val VALUE_REVISIT = "revisit"
    const val VALUE_FORWARD = "forward"
    const val VALUE_BACKWARD = "backward"
    const val VALUE_NEXT_SCREEN = "next_screen"

    const val PARAM_VIEW_TYPE = "view_type"
    const val PARAM_ENGAGEMENT_TIME = "engagement_time"
    const val PARAM_ACTION_METHOD = "action_method"
    const val PARAM_NAV_DIRECTION = "nav_direction"
    const val PARAM_TO_SCREEN = "to_screen"

    fun logEvent(eventName: String, params: Bundle? = null) {
        Log.d("EventTracking", "event=$eventName params=$params")
    }

    fun logEngagementComplete(eventName: String, startMs: Long, endMs: Long) {
        logEvent(
            eventName,
            Bundle().apply {
                putLong(PARAM_ENGAGEMENT_TIME, (endMs - startMs).coerceAtLeast(0L))
            }
        )
    }
}
