package com.core.ads

import android.app.Application
import android.os.Build
import android.text.TextUtils
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import com.core.ads.extensions.getAndroidId
import com.core.ads.extensions.md5
import com.core.preference.AppPreferences
import com.core.preference.SharedPrefs
import com.core.utilities.isAppDebuggable
import com.core.utilities.util.Timber
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.ConsentType
import com.google.firebase.analytics.analytics
import java.util.EnumMap
import java.util.Locale
import javax.inject.Inject

abstract class BaseAdmobApplication : Application() {

    @Inject
    lateinit var appPreferences: AppPreferences
    open val requiredUpdateConsent = true

    private val deviceTestList = mutableListOf<String>()

    override fun onCreate() {
        super.onCreate()

        SharedPrefs.init(context = this, name = "Core")

        if(appPreferences.systemLanguageCode.isBlank()) {
            appPreferences.systemLanguageCode = Locale.getDefault().language
        }

        fixWebView()

        setupDeviceTest()

        setupConsentMode()

        initLogging()

        initOtherConfig()


    }

    /**
     * Sửa lỗi Android Pie (9.0) WebView in multi-process
     *
     * https://stackoverflow.com/questions/51843546/android-pie-9-0-webview-in-multi-process
     */
    open fun fixWebView() {
        if (TextUtils.isEmpty(packageName)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val process = getProcessName()
            if (packageName != process) WebView.setDataDirectorySuffix(process)
        }
    }

    /**
     * Cho thiết bị hiện tại thành thiết bị test
     * @param isDebug = true cho thiết bị thành thành thiết bị test -> hiển thị quảng cáo test
     */
    open fun setupDeviceTest() {
        if (isAppDebuggable()) {
            deviceTestList.add(md5(getAndroidId(this)).uppercase())
            deviceTestList.add("161B90FEB622DEDE256FE344E5585390")

            val requestConfiguration = RequestConfiguration.Builder()
                .setTestDeviceIds(deviceTestList)
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)
        }
    }

    /**
     * Khởi tạo các cấu hình khác
     */
    open fun initOtherConfig() {}

    open fun initLogging() {
        if (Timber.treeCount != 0) return
        if (isAppDebuggable()) Timber.plant(Timber.DebugTree())
    }

    /**
     * Cấu hình analytics cho người dùng châu âu khi có thông báo
     *
     * (End users in the European Economic Area (EEA) must provide consent for their personal data to be shared with Google for advertising purposes.
     * When data is not marked as consented, it may impact ads personalization and measurement. Verify your Firebase consent settings)
     */
    private fun setupConsentMode() {
        if (!requiredUpdateConsent) return
        EnumMap<ConsentType, FirebaseAnalytics.ConsentStatus>(ConsentType::class.java).apply {
            put(ConsentType.ANALYTICS_STORAGE, FirebaseAnalytics.ConsentStatus.GRANTED)
            put(ConsentType.AD_STORAGE, FirebaseAnalytics.ConsentStatus.GRANTED)
            put(ConsentType.AD_USER_DATA, FirebaseAnalytics.ConsentStatus.GRANTED)
            put(ConsentType.AD_PERSONALIZATION, FirebaseAnalytics.ConsentStatus.GRANTED)
        }.let(Firebase.analytics::setConsent)
    }

    companion object {
        var isFirstSaveLanguage = false
        var isUserSelectLanguageNotDefault = false
    }
}
