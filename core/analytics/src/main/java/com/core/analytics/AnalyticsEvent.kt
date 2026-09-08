package com.core.analytics

object AnalyticsEvent {
    const val EVENT_REMOTE_CONFIG_FETCH = "remote_config_fetch"
    const val EVENT_REMOTE_CONFIG_FETCH_TIMEOUT_FIRST = "remote_config_fetch_timeout_first"
    const val EVENT_REMOTE_CONFIG_FETCH_TIMEOUT = "remote_config_fetch_timeout"

    const val NETWORK_AVAILABLE = "network_open_available"
    const val NETWORK_NOT_AVAILABLE = "network_open_not_available"
    const val NETWORK_OFF_TO_ON = "network_off_to_on"
    const val NETWORK_ON_TO_OFF = "network_on_to_off"

    const val ACTION_SPLASH_RETRY_TURN_ON = "splash_retry_turn_on"

    const val CHANGE_LANGUAGE_NOT_DEFAULT = "set_language_not_default"
    const val CHANGE_LANGUAGE_NOT_DEFAULT_CLICK_IN_MAIN = "set_language_not_default_click_in_main"

    const val EVENT_ACTION_SAVE_LANGUAGE_FIRST = "save_language_first"
    const val EVENT_ACTION_PASS_INTRO = "pass_intro_first"
    const val EVENT_ACTION_PASS_INTRO_FIRST_NEW_USER = "pass_intro_first_new_user"

    const val EVENT_CLICK_SHORT_CUT_1 = "short_cut_1"
    const val EVENT_CLICK_SHORT_CUT= "short_cut_"
    const val EVENT_CLICK_SHORT_CUT_2 = "short_cut_2"
    const val EVENT_CLICK_SHORT_CUT_3 = "short_cut_3"
    const val EVENT_CLICK_SHORT_CUT_UNINSTALL = "short_cut_uninstall"
    const val EVENT_CLICK_SHORT_CUT_UNINSTALL_BEFORE_SET_LANGUAGE = "short_cut_uninstall_none_language"

    const val EVENT_UNINSTALL_SCREEN_QUESTION = "uninstall_screen_question"
    const val EVENT_UNINSTALL_SCREEN_REASON = "uninstall_screen_reason"
    const val EVENT_UNINSTALL_REASON_DIFFICULT_USE = "uninstall_reason_difficult_use"
    const val EVENT_UNINSTALL_REASON_MANY_ADS = "uninstall_reason_many_ads"
    const val EVENT_UNINSTALL_REASON_CANNOT_FIND = "uninstall_reason_cannot_find"
    const val EVENT_UNINSTALL_REASON_NOT_CORRECTLY = "uninstall_reason_not_correctly"
    const val EVENT_UNINSTALL_REASON_DIFFICULT_RECOVER_FILE = "uninstall_reason_difficulty_recover_file"
    const val EVENT_UNINSTALL_REASON_OTHER = "uninstall_reason_difficulty_recover_file"


    const val EVENT_ACTION_RATE_APP = "rate_app"
    const val EVENT_ACTION_OPEN_PROMPT_ASSISTANT = "open_prompt_assistant"


    const val EVENT_ACTION_OPEN_CAPACITY = "open_capacity"
}