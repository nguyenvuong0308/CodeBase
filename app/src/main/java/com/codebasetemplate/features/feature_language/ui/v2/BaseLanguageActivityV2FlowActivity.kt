package com.codebasetemplate.features.feature_language.ui.v2

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import com.core.ads.BaseAdmobApplication
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.analytics.AnalyticsEvent
import com.core.baseui.ext.autoCleared
import com.core.baseui.ext.bindLiveData
import com.core.baseui.recyclerview.NpaLinearLayoutManager
import com.core.baseui.supportedlanguage.SupportedLanguage
import com.core.config.domain.data.IAdPlaceName
import com.core.utilities.getCurrentLanguageCode
import com.core.utilities.gone
import com.core.utilities.visible
import com.codebasetemplate.Navigator
import com.codebasetemplate.databinding.ActivityLanguageV2Binding
import com.codebasetemplate.core.base_ui.CoreActivity
import com.codebasetemplate.features.feature_language.ui.LanguageViewModel
import com.codebasetemplate.features.feature_language.ui.v2.adapter.LanguageGroup
import com.codebasetemplate.features.feature_language.ui.v2.adapter.LanguageOption
import com.codebasetemplate.features.feature_language.ui.v2.adapter.LanguageV2Adapter
import com.codebasetemplate.features.feature_onboarding.ui.helper.OnBoardingConfigFactory
import com.codebasetemplate.required.ads.AppAdPlaceName
import com.codebasetemplate.required.firebase.GetDataFromRemoteUseCaseImpl
import com.codebasetemplate.required.shortcut.AppShortCut
import com.codebasetemplate.util.EventTracking
import java.util.Locale
import javax.inject.Inject

abstract class BaseLanguageActivityV2FlowActivity : CoreActivity<ActivityLanguageV2Binding>() {

    @Inject
    lateinit var getDataFromRemoteUseCase: GetDataFromRemoteUseCaseImpl

    override val isHideStatusBar: Boolean
        get() = true

    override val isSpaceStatusBar: Boolean
        get() = true

    override val isSpaceDisplayCutout: Boolean
        get() = true

    override fun bindingProvider(inflater: LayoutInflater): ActivityLanguageV2Binding {
        return ActivityLanguageV2Binding.inflate(inflater)
    }

    private val viewModel: LanguageViewModel by viewModels()

    protected val isOpenFromSlash: Boolean by lazy {
        intent.extras?.getBoolean(LanguageV2FlowArgs.KEY_IS_OPEN_FROM_SPLASH, false) ?: false
    }

    protected val isFromSetting: Boolean by lazy {
        intent.extras?.getBoolean(LanguageV2FlowArgs.KEY_IS_FROM_SETTING, false) ?: false
    }

    protected val selectedGroupIdArg: String? by lazy {
        intent.extras?.getString(LanguageV2FlowArgs.KEY_SELECTED_GROUP_ID)
    }

    protected val selectedLanguageTagArg: String? by lazy {
        intent.extras?.getString(LanguageV2FlowArgs.KEY_SELECTED_LANGUAGE_TAG)
    }

    private val targetScreenFromShortCut by lazy {
        intent.extras?.getString(AppShortCut.KEY_SHORTCUT_TARGET_SCREEN, "")
    }

    protected val backFromIntroduction by lazy {
        intent.extras?.getBoolean(LanguageV2FlowArgs.KEY_BACK_FROM_INTRODUCTION, false) ?: false
    }

    val isEnableIntroductionScreen: Boolean by lazy {
        remoteConfigRepository.getAppConfig().isEnableIntroductionScreen
    }

    protected var supportedLanguageAdapter by autoCleared<LanguageV2Adapter>()

    private var languageGroups: List<LanguageGroup> = emptyList()
    private var isRecreated = false
    private var hasCheckedNavigation = false
    private var isHandlingBackFromSetting = false
    private var pendingApplyOption: LanguageOption? = null
    private var trackingStartedAtMs = 0L
    private var hasLoggedTrackingComplete = false

    protected abstract val languageNativeAdPlaceName: IAdPlaceName

    protected open val trackingViewEventName: String? = null

    protected open val trackingCompleteEventName: String? = null

    protected open val shouldHandleLanguageApplyNavigation: Boolean
        get() = false

    protected open fun initialExpandedGroupId(languageGroups: List<LanguageGroup>): String? {
        return null
    }

    protected open fun initialSelectedLanguageTag(): String? {
        return null
    }

    protected open fun initialApplyVisibility(): Boolean {
        return false
    }

    protected open fun initialApplyEnabled(): Boolean {
        return false
    }

    protected open fun onGroupClick(group: LanguageGroup): Boolean {
        return false
    }

    protected open fun onLanguageSelected(option: LanguageOption, group: LanguageGroup) = Unit

    protected abstract fun onApplyClicked()

    override fun onResume() {
        super.onResume()
        if (!shouldHandleLanguageApplyNavigation || hasCheckedNavigation) return

        hasCheckedNavigation = true
        checkIfNavigationIsNeeded()
    }

    override fun showFirstScreen() {
        isRecreated = appPreferences.navigateAfterChangeLanguage
    }

    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)
        trackingStartedAtMs = SystemClock.elapsedRealtime()
        trackingViewEventName?.let { eventName ->
            EventTracking.logEvent(eventName)
        }
        window.statusBarColor = Color.WHITE
        window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        viewBinding.languageBack.visibility = if (isFromSetting) View.VISIBLE else View.GONE
        viewBinding.languageBack.setOnClickListener {
            setupAfterOnBackPressed()
        }
        viewBinding.languageApply.setOnClickListener {
            onApplyClicked()
        }

        supportedLanguageAdapter = LanguageV2Adapter().also { adapter ->
            adapter.onGroupClicked = { group ->
                onGroupClick(group)
            }
            adapter.onSelectionChanged = selection@{ option ->
                val group = languageGroups.firstOrNull { candidate ->
                    candidate.options.any { it.id == option.id }
                } ?: return@selection
                onLanguageSelected(option, group)
            }
        }

        viewBinding.rvLanguage.apply {
            setHasFixedSize(false)
            adapter = supportedLanguageAdapter
            layoutManager = NpaLinearLayoutManager(this@BaseLanguageActivityV2FlowActivity)
            itemAnimator = DefaultItemAnimator().apply {
                addDuration = 170L
                removeDuration = 170L
                moveDuration = 170L
                changeDuration = 130L
            }
        }

        languageGroups = buildLanguageGroups()
        supportedLanguageAdapter.submitGroups(
            languageGroups = languageGroups,
            expandedGroupId = initialExpandedGroupId(languageGroups),
            selectedLanguageTag = initialSelectedLanguageTag()
        )
        setApplyButtonState(
            isVisible = initialApplyVisibility(),
            isEnabled = initialApplyEnabled()
        )

        bindLiveData(viewModel.initDataAndNextScreen) { isNext ->
            if (isNext) {
                pendingApplyOption?.let(::processNextScreen)
            }
        }
    }

    protected fun setApplyButtonState(isVisible: Boolean, isEnabled: Boolean) {
        viewBinding.languageApply.isEnabled = isEnabled
        if (isVisible) {
            viewBinding.languageApply.visible()
        } else {
            viewBinding.languageApply.gone()
        }
    }

    protected fun startStep(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    protected fun selectedOption(): LanguageOption? {
        return supportedLanguageAdapter.selectedOption
    }

    protected fun startApplyFlow() {
        val option = selectedOption() ?: return
        pendingApplyOption = option
        showApplyingLanguageState()
        if (isFromSetting) {
            processNextScreen(option)
        } else {
            viewModel.startInitAndNextScreen()
        }
    }

    protected fun logTrackingComplete() {
        if (hasLoggedTrackingComplete) return
        hasLoggedTrackingComplete = true
        trackingCompleteEventName?.let { eventName ->
            EventTracking.logEngagementComplete(
                eventName,
                trackingStartedAtMs,
                SystemClock.elapsedRealtime()
            )
        }
    }

    override fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> {
        if (isRecreated) return emptyList()

        return buildList {
            add(languageNativeAdPlaceName)
        }
    }

    override fun providerInterAdPlaceName(): List<IAdPlaceName> {
        return if (isFromSetting) {
            listOf(AppAdPlaceName.FULLSCREEN_BACK_LANGUAGE_SETTING)
        } else {
            emptyList()
        }
    }

    override fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
        viewBinding.layoutBannerNative.processAdResource(adResource, languageNativeAdPlaceName)
    }

    override fun setupAfterOnBackPressed() {
        if (isFromSetting) {
            showBackInterAdThenFinish()
        }
    }

    private fun processNextScreen(option: LanguageOption) {
        BaseAdmobApplication.isFirstSaveLanguage =
            isOpenFromSlash && getCurrentLanguageCode().isBlank()
        if (getCurrentLanguageCode().isBlank()) {
            analyticsManager.logEvent(AnalyticsEvent.EVENT_ACTION_SAVE_LANGUAGE_FIRST)
        }
        if (isOpenFromSlash || backFromIntroduction) {
            if (
                BaseAdmobApplication.isFirstSaveLanguage &&
                !LanguageV2Adapter.languageMatches(option.languageTag, appPreferences.systemLanguageCode)
            ) {
                analyticsManager.logEvent(AnalyticsEvent.CHANGE_LANGUAGE_NOT_DEFAULT)
                BaseAdmobApplication.isUserSelectLanguageNotDefault = true
            } else {
                BaseAdmobApplication.isUserSelectLanguageNotDefault = false
            }
        }
        applyLanguageAndNavigate(option)
    }

    private fun applyLanguageAndNavigate(option: LanguageOption) {
        appPreferences.navigateAfterChangeLanguage = true
        showApplyingLanguageState()

        if (languageTagsExactlyMatch(option.languageTag, getCurrentLanguageCode())) {
            checkIfNavigationIsNeeded()
        } else {
            changeLanguage(option.languageTag)
            scheduleNavigationFallbackAfterLanguageChange()
        }
    }

    private fun checkIfNavigationIsNeeded() {
        if (!appPreferences.navigateAfterChangeLanguage) return

        viewBinding.rvLanguage.gone()
        viewBinding.lnApplyLoading.visible()
        viewBinding.languageBack.isEnabled = false
        viewBinding.languageApply.isEnabled = false
        appPreferences.navigateAfterChangeLanguage = false

        val destination = if (isFromSetting) {
            Navigator.mainClass()
        } else if (isEnableIntroductionScreen) {
            OnBoardingConfigFactory.getOnBoardingClass(getDataFromRemoteUseCase.onBoardingConfig)
        } else {
            Navigator.mainClass()
        }
        val nextIntent = Intent(this, destination).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(AppShortCut.KEY_SHORTCUT_TARGET_SCREEN, targetScreenFromShortCut)
        }
        startActivity(nextIntent)
    }

    private fun changeLanguage(languageTag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSystemService(LocaleManager::class.java)
                .applicationLocales = LocaleList.forLanguageTags(languageTag)
        } else {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(languageTag)
            )
        }
    }

    private fun showApplyingLanguageState() {
        viewBinding.rvLanguage.gone()
        viewBinding.lnApplyLoading.visible()
        viewBinding.languageBack.isEnabled = false
        viewBinding.languageApply.isEnabled = false
    }

    private fun scheduleNavigationFallbackAfterLanguageChange() {
        viewBinding.root.postDelayed({
            if (!isFinishing && !isDestroyed && appPreferences.navigateAfterChangeLanguage) {
                checkIfNavigationIsNeeded()
            }
        }, NAVIGATION_AFTER_LANGUAGE_CHANGE_FALLBACK_MS)
    }

    private fun showBackInterAdThenFinish() {
        if (isHandlingBackFromSetting) return
        isHandlingBackFromSetting = true
        showInterAd(AppAdPlaceName.FULLSCREEN_BACK_LANGUAGE_SETTING) {
            finish()
        }
    }

    protected fun buildLanguageGroups(): List<LanguageGroup> {
        return listOf(
            languageGroup(
                id = "fr",
                language = SupportedLanguage.FRENCH,
                options = listOf(
                    option("fr-FR", "France", "FR", SupportedLanguage.FRENCH),
                    option("fr-CA", "Canada", "CA", SupportedLanguage.FRENCH),
                    option("fr-BE", "Belgium", "BE", SupportedLanguage.FRENCH),
                )
            ),
            languageGroup(
                id = "en",
                language = SupportedLanguage.ENGLISH,
                options = listOf(
                    option("en-US", "American", "US", SupportedLanguage.ENGLISH),
                    option("en-GB", "British", "GB", SupportedLanguage.ENGLISH),
                    option("en-PH", "Philippines", "PH", SupportedLanguage.ENGLISH),
                )
            ),
            languageGroup(
                id = "hi",
                language = SupportedLanguage.HINDI,
                options = listOf(option("hi-IN", "India", "IN", SupportedLanguage.HINDI))
            ),
            languageGroup(
                id = "es",
                language = SupportedLanguage.SPANISH,
                options = listOf(
                    option("es-ES", "Spain", "ES", SupportedLanguage.SPANISH),
                    option("es-MX", "Mexico", "MX", SupportedLanguage.SPANISH),
                    option("es-US", "United States", "US", SupportedLanguage.SPANISH),
                )
            ),
            languageGroup(
                id = "pt",
                language = SupportedLanguage.PORTUGUESE,
                options = listOf(
                    option("pt-BR", "Brazil", "BR", SupportedLanguage.PORTUGUESE),
                    option("pt-PT", "Portugal", "PT", SupportedLanguage.PORTUGUESE),
                )
            ),
            languageGroup(
                id = "ru",
                language = SupportedLanguage.RUSSIAN,
                options = listOf(
                    option("ru-RU", "Russia", "RU", SupportedLanguage.RUSSIAN),
                    option("ru-BY", "Belarus", "BY", SupportedLanguage.RUSSIAN),
                    option("ru-UA", "Ukraine", "UA", SupportedLanguage.RUSSIAN),
                )
            ),
            singleLanguageGroup("de", "de-DE", "Germany", "DE", SupportedLanguage.GERMAN),
            singleLanguageGroup("it", "it-IT", "Italy", "IT", SupportedLanguage.ITALIAN),
            singleLanguageGroup("id", "id-ID", "Indonesia", "ID", SupportedLanguage.INDONESIAN),
            singleLanguageGroup("vi", "vi-VN", "Vietnam", "VN", SupportedLanguage.VIETNAMESE),
            singleLanguageGroup("tr", "tr-TR", "Turkey", "TR", SupportedLanguage.TURKISH),
            singleLanguageGroup("ms", "ms-MY", "Malaysia", "MY", SupportedLanguage.MALAY),
            singleLanguageGroup("th", "th-TH", "Thailand", "TH", SupportedLanguage.THAI),
            singleLanguageGroup("bn", "bn-BD", "Bangladesh", "BD", SupportedLanguage.BENGALI),
            singleLanguageGroup("fi", "fi-FI", "Finland", "FI", SupportedLanguage.FINNISH),
            singleLanguageGroup("ja", "ja-JP", "Japan", "JP", SupportedLanguage.JAPAN),
            singleLanguageGroup("ca", "ca-ES", "Catalonia", "ES", SupportedLanguage.CATALAN),
            singleLanguageGroup("et", "et-EE", "Estonia", "EE", SupportedLanguage.ESTONIAN),
            singleLanguageGroup("is", "is-IS", "Iceland", "IS", SupportedLanguage.ICELANDIC),
            singleLanguageGroup("lv", "lv-LV", "Latvia", "LV", SupportedLanguage.LATVIAN),
            singleLanguageGroup("lt", "lt-LT", "Lithuania", "LT", SupportedLanguage.LITHUANIAN),
            singleLanguageGroup("tl", "tl-PH", "Philippines", "PH", SupportedLanguage.FILIPINO),
            singleLanguageGroup("kk", "kk-KZ", "Kazakhstan", "KZ", SupportedLanguage.KAZAKH),
            singleLanguageGroup("ko", "ko-KR", "South Korea", "KR", SupportedLanguage.KOREAN),
            singleLanguageGroup("nl", "nl-NL", "Netherlands", "NL", SupportedLanguage.DUTCH),
            singleLanguageGroup("pl", "pl-PL", "Poland", "PL", SupportedLanguage.POLISH),
            singleLanguageGroup("el", "el-GR", "Greece", "GR", SupportedLanguage.GREEK),
            singleLanguageGroup("bg", "bg-BG", "Bulgaria", "BG", SupportedLanguage.BULGARIAN),
            singleLanguageGroup("cs", "cs-CZ", "Czechia", "CZ", SupportedLanguage.CZECH),
            singleLanguageGroup("da", "da-DK", "Denmark", "DK", SupportedLanguage.DANMARK),
            singleLanguageGroup("gu", "gu-IN", "India", "IN", SupportedLanguage.GUJARATI),
            singleLanguageGroup("kn", "kn-IN", "India", "IN", SupportedLanguage.KANNADA),
            singleLanguageGroup("ml", "ml-IN", "India", "IN", SupportedLanguage.MALAYALAM),
            singleLanguageGroup("mr", "mr-IN", "India", "IN", SupportedLanguage.MARATHI),
            singleLanguageGroup("my", "my-MM", "Myanmar", "MM", SupportedLanguage.BURMESE),
            singleLanguageGroup("hu", "hu-HU", "Hungary", "HU", SupportedLanguage.HUNGARIAN),
            singleLanguageGroup("hr", "hr-HR", "Croatia", "HR", SupportedLanguage.CROATIAN),
            singleLanguageGroup("no", "no-NO", "Norway", "NO", SupportedLanguage.NORWEGIAN),
            singleLanguageGroup("pa", "pa-IN", "India", "IN", SupportedLanguage.PUNJABI),
            singleLanguageGroup("ro", "ro-RO", "Romania", "RO", SupportedLanguage.RUMANU),
            singleLanguageGroup("sv", "sv-SE", "Sweden", "SE", SupportedLanguage.SWEDISH),
            singleLanguageGroup("sw", "sw-KE", "Kenya", "KE", SupportedLanguage.SWAHILI),
            singleLanguageGroup("sr", "sr-RS", "Serbia", "RS", SupportedLanguage.SERBIAN),
            singleLanguageGroup("sk", "sk-SK", "Slovakia", "SK", SupportedLanguage.SLOVAK),
            singleLanguageGroup("uz", "uz-UZ", "Uzbekistan", "UZ", SupportedLanguage.UZBEK),
            singleLanguageGroup("uk", "uk-UA", "Ukraine", "UA", SupportedLanguage.UKRAINA),
            singleLanguageGroup("ta", "ta-IN", "India", "IN", SupportedLanguage.TAMIL),
            singleLanguageGroup("te", "te-IN", "India", "IN", SupportedLanguage.TELUGU),
            languageGroup(
                id = "zh",
                language = SupportedLanguage.CHINA_SIMPLIFIED,
                options = listOf(
                    option("zh-CN", "China", "CN", SupportedLanguage.CHINA_SIMPLIFIED),
                    option("zh-TW", "Taiwan", "TW", SupportedLanguage.CHINA_TRADITIONAL),
                )
            ),
            languageGroup(
                id = "ar",
                language = SupportedLanguage.ARABIC,
                options = listOf(
                    option("ar-SA", "Saudi Arabia", "SA", SupportedLanguage.ARABIC),
                    option("ar-EG", "Egypt", "EG", SupportedLanguage.ARABIC),
                    option("ar-AE", "United Arab Emirates", "AE", SupportedLanguage.ARABIC),
                )
            ),
            singleLanguageGroup("fa", "fa-IR", "Iran", "IR", SupportedLanguage.PERSIAN),
            singleLanguageGroup("ur", "ur-PK", "Pakistan", "PK", SupportedLanguage.URDU),
            singleLanguageGroup("yi", "yi-001", "World", "IL", SupportedLanguage.YIDDISH),
        ).sortedByDescending { it.options.size > 1 }
    }

    private fun singleLanguageGroup(
        id: String,
        languageTag: String,
        optionTitle: String,
        countryCode: String,
        language: SupportedLanguage,
    ): LanguageGroup {
        return languageGroup(
            id = id,
            language = language,
            options = listOf(option(languageTag, optionTitle, countryCode, language))
        )
    }

    private fun languageGroup(
        id: String,
        language: SupportedLanguage,
        options: List<LanguageOption>,
    ): LanguageGroup {
        val baseTag = options.firstOrNull()?.languageTag ?: language.languageCode
        val locale = Locale.forLanguageTag(baseTag)
        val title = locale.getDisplayLanguage(Locale.ENGLISH)
            .replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString()
            }
            .ifBlank { language.displayName }
        val nativeName = locale.getDisplayLanguage(locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            .ifBlank { language.displayName }
        return LanguageGroup(
            id = id,
            title = title,
            nativeName = nativeName,
            language = language,
            options = options,
        )
    }

    private fun option(
        languageTag: String,
        title: String,
        countryCode: String,
        language: SupportedLanguage,
    ): LanguageOption {
        return LanguageOption(
            id = languageTag,
            title = title,
            languageTag = languageTag,
            countryCode = countryCode,
            language = language,
        )
    }

    private fun languageTagsExactlyMatch(left: String, right: String): Boolean {
        return left.replace('_', '-').equals(right.replace('_', '-'), ignoreCase = true)
    }

    companion object {
        private const val NAVIGATION_AFTER_LANGUAGE_CHANGE_FALLBACK_MS = 700L
    }
}

object LanguageV2FlowArgs {
    const val KEY_IS_OPEN_FROM_SPLASH = "KEY_IS_OPEN_FROM_SPLASH"
    const val KEY_IS_FROM_SETTING = "KEY_IS_FROM_SETTING"
    const val KEY_BACK_FROM_INTRODUCTION = "KEY_BACK_FROM_INTRODUCTION"
    const val KEY_SELECTED_GROUP_ID = "KEY_SELECTED_GROUP_ID"
    const val KEY_SELECTED_LANGUAGE_TAG = "KEY_SELECTED_LANGUAGE_TAG"

    fun fillCommonExtras(
        intent: Intent,
        fromSetting: Boolean,
        fromSplash: Boolean,
        fromIntroduction: Boolean,
    ) {
        intent.putExtra(KEY_IS_FROM_SETTING, fromSetting)
        intent.putExtra(KEY_IS_OPEN_FROM_SPLASH, fromSplash)
        intent.putExtra(KEY_BACK_FROM_INTRODUCTION, fromIntroduction)
    }

    fun buildIntent(
        context: Context,
        target: Class<*>,
        fromSetting: Boolean,
        fromSplash: Boolean,
        fromIntroduction: Boolean,
    ): Intent {
        return Intent(context, target).apply {
            fillCommonExtras(this, fromSetting, fromSplash, fromIntroduction)
        }
    }
}
