package com.codebasetemplate.util

import android.os.Bundle
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics

object EventTracking {
    const val EVENT_SPLASH_VIEW = "start_flow_splash_view"
    const val EVENT_SPLASH_COMPLETE = "start_flow_splash_complete"
    const val EVENT_SPLASH_BEFORE_AD = "start_flow_splash_before_ad"
    const val EVENT_INTER_SPLASH_VIEW = "start_flow_Inter_splash_view"
    const val EVENT_INTER_SPLASH_COMPLETE = "start_flow_Inter_splash_complete"

    const val EVENT_LFO1_VIEW = "start_flow_lfo1_view"
    const val EVENT_LFO1_COMPLETE = "start_flow_lfo1_complete"
    const val EVENT_LFO2_VIEW = "start_flow_lfo2_view"
    const val EVENT_LFO2_COMPLETE = "start_flow_lfo2_complete"
    const val EVENT_LFO3_VIEW = "start_flow_lfo3_view"
    const val EVENT_LFO3_COMPLETE = "start_flow_lfo3_complete"

    const val EVENT_ONBOARD_1_VIEW = "start_flow_onb1_view"
    const val EVENT_ONBOARD_1_COMPLETE = "start_flow_onb1_complete"
    const val EVENT_ONBOARD_2_VIEW = "start_flow_onb2_view"
    const val EVENT_ONBOARD_2_COMPLETE = "start_flow_onb2_complete"
    const val EVENT_ONBOARD_3_VIEW = "start_flow_onb3_view"
    const val EVENT_ONBOARD_3_COMPLETE = "start_flow_onb3_complete"
    const val EVENT_ONBOARD_4_VIEW = "start_flow_onb4_view"
    const val EVENT_ONBOARD_4_COMPLETE = "start_flow_onb4_complete"
    const val EVENT_ONBOARD_INTER_VIEW = "start_flow_onb_inter_view"
    const val EVENT_ONBOARD_INTER_COMPLETE = "start_flow_onb_inter_complete"

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

    fun onboardingViewEvent(screenNumber: Int) = when (screenNumber) {
        1 -> EVENT_ONBOARD_1_VIEW
        2 -> EVENT_ONBOARD_2_VIEW
        3 -> EVENT_ONBOARD_3_VIEW
        4 -> EVENT_ONBOARD_4_VIEW
        else -> "start_flow_onb${screenNumber}_view"
    }

    fun onboardingCompleteEvent(screenNumber: Int) = when (screenNumber) {
        1 -> EVENT_ONBOARD_1_COMPLETE
        2 -> EVENT_ONBOARD_2_COMPLETE
        3 -> EVENT_ONBOARD_3_COMPLETE
        4 -> EVENT_ONBOARD_4_COMPLETE
        else -> "start_flow_onb${screenNumber}_complete"
    }

    fun logEvent(eventName: String, params: Bundle? = null) {
        Log.d("EventTracking", "event=$eventName params=$params")
        Firebase.analytics.logEvent(eventName, params ?: Bundle())
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
