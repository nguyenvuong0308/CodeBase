package com.core.preference

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
) {
    companion object {
        private const val SHARED_NAME = "private_shared"
        private const val KEY_REMOTE_CONFIG_IS_FIRST_TIME_FETCH = "KEY_REMOTE_CONFIG_IS_FIRST_TIME_FETCH"
        private const val KEY_NUMBER_OF_REQUEST_STORAGE_PERMISSION =
            "KEY_NUMBER_OF_REQUEST_STORAGE_PERMISSION"
        private const val KEY_OPEN_APP_COUNT = "KEY_OPEN_APP_COUNT"
        private const val KEY_RATED_APP = "KEY_RATED_APP"
        private const val KEY_IS_SHOW_INTRO = "isShowIntro"

        private const val KEY_SYSTEM_LANGUAGE_CODE = "KEY_SYSTEM_LANGUAGE_CODE"
        private const val KEY_CURRENT_LANGUAGE_CODE = "KEY_CURRENT_LANGUAGE_CODE"
    }

    private val prefs: SharedPreferences =
        applicationContext.getSharedPreferences(SHARED_NAME, Context.MODE_PRIVATE)
    var navigateAfterChangeLanguage by prefs.boolean(
        key = { "navigateAfterChangeLanguage" },
        defaultValue = false
    )

    var openAppCount by prefs.int(
        key = { KEY_OPEN_APP_COUNT },
        defaultValue = 0
    )
    
    var isRemoteConfigFirstTimeFetch by prefs.boolean(
        key = { KEY_REMOTE_CONFIG_IS_FIRST_TIME_FETCH },
        defaultValue = true
    )

    var systemLanguageCode: String
        get() = prefs.getString(KEY_SYSTEM_LANGUAGE_CODE, "").orEmpty()
            .ifBlank { Locale.getDefault().language }
        set(value) = prefs.edit { putString(KEY_SYSTEM_LANGUAGE_CODE, value) }

    var currentLanguageCode by prefs.string (
        key = { KEY_CURRENT_LANGUAGE_CODE },
        defaultValue = ""
    )

    var timeOfFirstAdClicked by prefs.long()

    var adClickedCount by prefs.int()



    var numberOfRequestStoragePermission by prefs.int(
        key = { KEY_NUMBER_OF_REQUEST_STORAGE_PERMISSION },
        defaultValue = 0
    )

    var isUserRated by prefs.boolean(
        key = { KEY_RATED_APP },
        defaultValue = false
    )
    var isShowIntro by prefs.boolean(key = { KEY_IS_SHOW_INTRO })

}