package com.codebasetemplate.features.feature_language.ui

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import com.core.startflow.R
import com.core.startflow.StartFlowActivity
import com.core.startflow.databinding.StartflowActivityLanguageBinding
import com.codebasetemplate.features.feature_language.ui.adapter.SupportedLanguageAdapter
import com.core.config.domain.data.CoreAdPlaceName
import com.core.startflow.OnBoardingConfigFactory
import com.core.startflow.StartFlowNavigator
import com.core.startflow.StartFlowShortcut
import com.core.ads.BaseAdmobApplication
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.analytics.AnalyticsEvent
import com.core.baseui.executor.AppExecutors
import com.core.baseui.ext.autoCleared
import com.core.baseui.ext.bindLiveData
import com.core.baseui.recyclerview.NpaLinearLayoutManager
import com.core.baseui.supportedlanguage.SupportedLanguage
import com.core.baseui.toolbar.CoreToolbarView
import com.core.config.domain.data.IAdPlaceName
import com.core.utilities.getCurrentLanguageCode
import com.core.utilities.gone
import com.core.utilities.visible
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LanguageActivity : StartFlowActivity<StartflowActivityLanguageBinding>() {

    @Inject
    lateinit var startFlowNavigator: StartFlowNavigator

    override val isHideStatusBar: Boolean
        get() = true

    override val isSpaceStatusBar: Boolean
        get() = true

    override val isSpaceDisplayCutout: Boolean
        get() = true

    override fun bindingProvider(inflater: LayoutInflater): StartflowActivityLanguageBinding {
        return StartflowActivityLanguageBinding.inflate(inflater)
    }

    private val viewModel: LanguageViewModel by viewModels()


    private val isOpenFromSlash: Boolean by lazy {
        intent.extras?.getBoolean(KEY_IS_OPEN_FROM_SPLASH, false) ?: false
    }

    private val isFromSetting: Boolean by lazy {
        intent.extras?.getBoolean(KEY_IS_FROM_SETTING, false) ?: false
    }

    @Inject
    lateinit var appExecutors: AppExecutors


    private var supportedLanguageAdapter by autoCleared<SupportedLanguageAdapter>()


    private val isEnableIntroductionScreen: Boolean by lazy {
        remoteConfigRepository.getAppConfig().isEnableIntroductionScreen
    }


    private val targetScreenFromShortCut by lazy {
        intent.extras?.getString(StartFlowShortcut.KEY_SHORTCUT_TARGET_SCREEN, "")
    }

    private val backFromIntroduction by lazy {
        intent.extras?.getBoolean(KEY_BACK_FROM_INTRODUCTION, false) ?: false
    }

    private var isRecreated = false

    private var hasCheckedNavigation = false

    private var hasUserSelectedLanguage = false

    private var step1AdState = LanguageBannerAdState.PENDING

    private var step2AdState = LanguageBannerAdState.PENDING

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (!hasCheckedNavigation) {
            hasCheckedNavigation = true
            checkIfNavigationIsNeeded()
        }
    }

    override fun showFirstScreen() {
        isRecreated = appPreferences.navigateAfterChangeLanguage
    }

    private fun checkIfNavigationIsNeeded() {
        // Nếu cờ được bật
        if (appPreferences.navigateAfterChangeLanguage) {
            viewBinding.rvLanguage.gone()
            viewBinding.lnApplyLoading.visible()
            viewBinding.toolbar.isEnableBack = false
            appPreferences.navigateAfterChangeLanguage = false
            if (isFromSetting) {
                val intent = Intent(
                    this@LanguageActivity,
                    startFlowNavigator.mainClass()
                )
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                val bundle = Bundle().apply {
                    putString(
                        StartFlowShortcut.KEY_SHORTCUT_TARGET_SCREEN,
                        targetScreenFromShortCut
                    )
                }
                intent.putExtras(bundle)
                this@LanguageActivity.startActivity(intent)
            } else {
                val otherCase = startFlowNavigator.otherLanguageEnd()

                val intent = Intent(
                    this@LanguageActivity,
                    otherCase ?: if (isEnableIntroductionScreen) {
                        OnBoardingConfigFactory.getOnBoardingClass(remoteConfigRepository.getOnBoardingConfig())
                    } else {
                        startFlowNavigator.mainClass()
                    }
                )
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                val bundle = Bundle().apply {
                    putString(
                        StartFlowShortcut.KEY_SHORTCUT_TARGET_SCREEN,
                        targetScreenFromShortCut
                    )
                }
                intent.putExtras(bundle)
                this@LanguageActivity.startActivity(intent)
            }
        }
    }

    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)
        viewBinding.run {
            if (resources.getBoolean(R.bool.startflow_language_show_image_background)) {
                imageBackground.visible()
            } else {
                imageBackground.gone()
            }

            toolbar.onToolbarListener = object : CoreToolbarView.OnToolbarListener {
                override fun onBack() {
                    setupAfterOnBackPressed()
                }

                override fun onTvAction() {
                    if (isFromSetting) {
                        processNextScreen()
                    } else {
                        supportedLanguageAdapter.submitList(supportedLanguageAdapter.currentList.filter { it.isSelected })
                        lnApplyLoading.visible()
                        viewModel.startInitAndNextScreen()
                    }
                }
            }

            supportedLanguageAdapter = SupportedLanguageAdapter(appExecutors).apply {
                setHasStableIds(true)
            }

            supportedLanguageAdapter.onClickListener = {
                toolbar.isEnableTvAction = true
                hasUserSelectedLanguage = true
                updateBannerVisibility()
            }

            rvLanguage.apply {
                setHasFixedSize(false)
                adapter = supportedLanguageAdapter
                layoutManager = NpaLinearLayoutManager(this@LanguageActivity)
                /*addItemDecoration(
                    LinearSpacingItemDecoration(
                        verticalSpacing = resources.getDimensionPixelSize(com.core.dimens.R.dimen._16dp),
                        horizontalSpacing = resources.getDimensionPixelSize(com.core.dimens.R.dimen._16dp)
                    )
                )*/
                itemAnimator = DefaultItemAnimator()
            }
        }

        displayFirstData()

        bindLiveData(viewModel.initDataAndNextScreen) { isNext ->
            if (isNext) {
                processNextScreen()
            }
        }

    }

    // Giả sử bạn có một đối tượng quản lý SharedPreferences
    private fun userChoosesToGoToSettingsAfterLanguageChange(language: SupportedLanguage) {
        // Đánh dấu rằng chúng ta muốn mở màn hình Settings sau khi đổi ngôn ngữ
        appPreferences.navigateAfterChangeLanguage = true

        applyLanguageAndContinue(
            selectedLanguageCode = language.languageCode,
            currentLanguageCode = getCurrentLanguageCode(),
            changeLanguage = { changeLanguage(language) },
            continueAfterLanguageApplied = ::checkIfNavigationIsNeeded,
        )
    }

    private fun processNextScreen() {
        supportedLanguageAdapter.currentList.find { it.isSelected }?.let {
            BaseAdmobApplication.isFirstSaveLanguage =
                isOpenFromSlash && getCurrentLanguageCode().isBlank()
            if (getCurrentLanguageCode().isBlank()) {
                analyticsManager.logEvent(AnalyticsEvent.EVENT_ACTION_SAVE_LANGUAGE_FIRST)
            }
            if (isOpenFromSlash || backFromIntroduction) {
                if (BaseAdmobApplication.isFirstSaveLanguage && it.languageCode != appPreferences.systemLanguageCode) {
                    analyticsManager.logEvent(AnalyticsEvent.CHANGE_LANGUAGE_NOT_DEFAULT)
                    BaseAdmobApplication.isUserSelectLanguageNotDefault = true
                } else {
                    BaseAdmobApplication.isUserSelectLanguageNotDefault = false
                }
            }
            userChoosesToGoToSettingsAfterLanguageChange(it)
        }
    }

    override fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> {
        if (isRecreated) {
            return listOf()
        }
        return mutableListOf<IAdPlaceName>().apply {
            add(CoreAdPlaceName.ANCHORED_CHANGE_LANGUAGE_V1_STEP_1)
            add(CoreAdPlaceName.ANCHORED_CHANGE_LANGUAGE_V1_STEP_2)
            if ((isOpenFromSlash || backFromIntroduction) && isEnableIntroductionScreen) {
                addAll(OnBoardingConfigFactory.getOnBoardingAdPlaceName(remoteConfigRepository.getOnBoardingConfig(), remoteConfigRepository.getAppConfig()))
            }
        }
    }

    override fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
        val adState = when (adResource) {
            is AdLoadBannerNativeUiResource.BannerAdLoaded,
            is AdLoadBannerNativeUiResource.NativeAdLoaded -> LanguageBannerAdState.AVAILABLE

            is AdLoadBannerNativeUiResource.AdFailed,
            is AdLoadBannerNativeUiResource.AdNetworkError -> LanguageBannerAdState.UNAVAILABLE

            is AdLoadBannerNativeUiResource.Loading -> LanguageBannerAdState.PENDING
        }

        when (adResource.commonAdPlaceName) {
            CoreAdPlaceName.ANCHORED_CHANGE_LANGUAGE_V1_STEP_1 -> {
                step1AdState = adState
                viewBinding.layoutBannerNativeStep1.processAdResource(
                    adResource = adResource,
                    placeName = CoreAdPlaceName.ANCHORED_CHANGE_LANGUAGE_V1_STEP_1,
                    canVisible = false,
                    isHideNativeBannerWhenNetworkError = true,
                )
            }

            CoreAdPlaceName.ANCHORED_CHANGE_LANGUAGE_V1_STEP_2 -> {
                step2AdState = adState
                viewBinding.layoutBannerNativeStep2.processAdResource(
                    adResource = adResource,
                    placeName = CoreAdPlaceName.ANCHORED_CHANGE_LANGUAGE_V1_STEP_2,
                    canVisible = false,
                    isHideNativeBannerWhenNetworkError = true,
                )
            }

            else -> return
        }

        updateBannerVisibility()
    }

    private fun updateBannerVisibility() {
        when (
            resolveLanguageBannerStep(
                hasUserSelectedLanguage = hasUserSelectedLanguage,
                step1AdState = step1AdState,
                step2AdState = step2AdState,
            )
        ) {
            LanguageBannerStep.STEP_1 -> {
                viewBinding.layoutBannerNativeStep1.visible()
                viewBinding.layoutBannerNativeStep2.gone()
            }

            LanguageBannerStep.STEP_2 -> {
                viewBinding.layoutBannerNativeStep1.gone()
                viewBinding.layoutBannerNativeStep2.visible()
            }

            null -> {
                viewBinding.layoutBannerNativeStep1.gone()
                viewBinding.layoutBannerNativeStep2.gone()
            }
        }
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
    }

    private fun displayFirstData() {
        val currentLanguageCode = getCurrentLanguageCode()
        viewBinding.toolbar.showBack = isFromSetting

        val allLanguages = arrayListOf(
            SupportedLanguage.ENGLISH,
            SupportedLanguage.SPANISH,
            SupportedLanguage.PORTUGUESE,
            SupportedLanguage.FRENCH,
            SupportedLanguage.GERMAN,
            SupportedLanguage.HINDI,
            SupportedLanguage.ITALIAN,
            SupportedLanguage.INDONESIAN,
            SupportedLanguage.VIETNAMESE,
            SupportedLanguage.TURKISH,
            SupportedLanguage.MALAY,
            SupportedLanguage.THAI,
            SupportedLanguage.BENGALI,
            SupportedLanguage.FINNISH,
            SupportedLanguage.JAPAN,
            SupportedLanguage.CATALAN,
            SupportedLanguage.ESTONIAN,
            SupportedLanguage.ICELANDIC,
            SupportedLanguage.LATVIAN,
            SupportedLanguage.LITHUANIAN,
            SupportedLanguage.FILIPINO,
            SupportedLanguage.KAZAKH,
            SupportedLanguage.KOREAN,
            SupportedLanguage.DUTCH,
            SupportedLanguage.POLISH,
            SupportedLanguage.GREEK,
            SupportedLanguage.BULGARIAN,
            SupportedLanguage.RUSSIAN,
            SupportedLanguage.CZECH,
            SupportedLanguage.DANMARK,
            SupportedLanguage.GUJARATI,
            SupportedLanguage.KANNADA,
            SupportedLanguage.MALAYALAM,
            SupportedLanguage.MARATHI,
            SupportedLanguage.BURMESE,
            SupportedLanguage.HUNGARIAN,
            SupportedLanguage.CROATIAN,
            SupportedLanguage.NORWEGIAN,
            SupportedLanguage.PUNJABI,
            SupportedLanguage.RUMANU,
            SupportedLanguage.SWEDISH,
            SupportedLanguage.SWAHILI,
            SupportedLanguage.SERBIAN,
            SupportedLanguage.SLOVAK,
            SupportedLanguage.UZBEK,
            SupportedLanguage.UKRAINA,
            SupportedLanguage.TAMIL,
            SupportedLanguage.TELUGU,
            SupportedLanguage.CHINA_SIMPLIFIED,
            SupportedLanguage.CHINA_TRADITIONAL,
            SupportedLanguage.ARABIC,
            SupportedLanguage.PERSIAN,
            SupportedLanguage.URDU,
            SupportedLanguage.YIDDISH,
        )


        allLanguages.forEach {
            it.isSelected = false
            println("private const val DATA_${it.languageCode.uppercase()} = \"data_${it.languageCode}\"")
        }

        val systemLanguageCode = appPreferences.systemLanguageCode
        val supportedLanguageSystem = allLanguages.find { it.languageCode == systemLanguageCode }
        supportedLanguageAdapter.systemLanguageCode = systemLanguageCode

        val supportedLanguages = ArrayList<SupportedLanguage>()
        supportedLanguages.addAll(allLanguages)
        val indexSystem = 4

        supportedLanguageSystem?.let {
            supportedLanguages.removeAll { supportedLanguageSystem.languageCode == it.languageCode }
            supportedLanguages.add(indexSystem, supportedLanguageSystem)
        }

        if (isFromSetting) {
            supportedLanguageAdapter.isShowHand = false
            for (i in supportedLanguages.indices) {
                val language = supportedLanguages[i]
                language.isSelected = language.languageCode == currentLanguageCode
            }
        }

        supportedLanguageAdapter.submitList(supportedLanguages.toMutableList())
    }

    private fun changeLanguage(supportedLanguage: SupportedLanguage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSystemService(LocaleManager::class.java)
                .applicationLocales = LocaleList.forLanguageTags(supportedLanguage.languageCode)
        } else {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(
                    supportedLanguage.languageCode
                )
            )
        }
    }

    override fun setupAfterOnBackPressed() {
        if (isFromSetting) {
            super.setupAfterOnBackPressed()
        }
    }

    companion object {
        const val KEY_IS_OPEN_FROM_SPLASH = "KEY_IS_OPEN_FROM_SPLASH"
        const val KEY_IS_FROM_SETTING = "KEY_IS_FROM_SETTING"
        const val KEY_BACK_FROM_INTRODUCTION = "KEY_BACK_FROM_INTRODUCTION"

        fun intentStart(
            context: Context,
            fromSetting: Boolean = false,
            fromSplash: Boolean = false,
            fromIntroduction: Boolean = false,
        ) =
            Intent(context, LanguageActivity::class.java).apply {
                putExtra(KEY_IS_FROM_SETTING, fromSetting)
                putExtra(KEY_IS_OPEN_FROM_SPLASH, fromSplash)
                putExtra(KEY_BACK_FROM_INTRODUCTION, fromIntroduction)
            }
    }
}
