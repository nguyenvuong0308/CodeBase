package com.core.config

/** Local overrides for debuggable apps. Release apps always use Remote Config. */
object DebugRemoteConfig {
    /**
     * Overrides `is_turn_on_ad_places_disabled_when_detect_test_ad` in debug builds.
     * Set before loading ads (for example in Application.initOtherConfig()).
     * `true` enables protection, `false` disables it, and `null` uses Remote Config.
     * Restart the app after changing this setting to reset detected test-ad state.
     */
    @Volatile
    var isTurnOnAdPlacesDisabledWhenDetectTestAd: Boolean? = null
}
