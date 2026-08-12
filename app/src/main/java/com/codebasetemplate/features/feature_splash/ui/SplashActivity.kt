package com.codebasetemplate.features.feature_splash.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.viewModels
import com.codebasetemplate.features.feature_language.ui.LanguageActivityNavigator
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.baseui.R
import com.core.baseui.ext.bindLiveData
import com.core.config.domain.data.CoreAdPlaceName
import com.core.config.domain.data.IAdPlaceName
import com.core.startflow.OnBoardingConfigFactory
import com.core.startflow.StartFlowScreenType
import com.core.startflow.StartFlowShortcut
import com.core.startflow.databinding.StartflowActivitySplashBinding
import com.core.utilities.getCurrentLanguageCode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "SplashActivity"

@AndroidEntryPoint
class SplashActivity : BaseSplashActivity<StartflowActivitySplashBinding>() {

    companion object {
        private const val EARLY_PROGRESS_RATIO = 0.4f
        private const val EARLY_PROGRESS_DURATION_MS = 1_200L
    }

    private var earlyProgressJob: Job? = null
    private var splashCountdownStarted = false
    private var splashProgressMax = 15_000
    private var earlyProgressValue = 0

    private var currentSplashStatus = SplashStatus.FetchingRemoteConfig

    private var messageHashMap = HashMap<Int, Int>()

    private val viewModel by viewModels<SplashLoadDataViewModel>()

    /**
     * Tạo ViewBinding cho màn Splash từ layout trong module core:startflow.
     */
    override fun bindingProvider(inflater: LayoutInflater): StartflowActivitySplashBinding {
        return StartflowActivitySplashBinding.inflate(inflater)
    }

    /**
     * Khởi tạo dữ liệu nhẹ của app, chạy progress sớm và chờ ViewModel báo sẵn sàng.
     */
    override fun initData() {
        viewModel.initData()
        startEarlyProgress()
        bindLiveData(viewModel.initData) { isReady ->
            if (isReady) {
                if (!viewModel.isInitData) {
                    onDataReady()
                }
                viewModel.isInitData = true
            }
        }
    }

    /**
     * Không hiển thị loading phụ vì màn Splash đã có progress riêng.
     */
    override fun hideLoadingX() {
    }

    /**
     * Nhận trạng thái mới từ BaseSplashActivity và cập nhật message tương ứng trên UI.
     */
    override fun onSplashStatusChanged(status: SplashStatus) {
        currentSplashStatus = status
        updateSplashMessage(status)
    }

    /**
     * Đánh dấu countdown chính thức đã bắt đầu và chuyển progress sang vùng sau mốc khởi động sớm.
     */
    override fun onSplashCountdownStarted(max: Int) {
        splashCountdownStarted = true
        splashProgressMax = max
        earlyProgressJob?.cancel()
        earlyProgressJob = null
        applySplashProgress((max * EARLY_PROGRESS_RATIO).toInt(), max)
    }

    /**
     * Tăng progress ban đầu trong lúc Splash đang chờ remote config, consent hoặc dữ liệu local.
     */
    private fun startEarlyProgress() {
        if (earlyProgressJob?.isActive == true) return
        val targetProgress = (splashProgressMax * EARLY_PROGRESS_RATIO).toInt()
        earlyProgressJob = CoroutineScope(coroutineContext).launch {
            val stepDelay = 16L
            val steps = (EARLY_PROGRESS_DURATION_MS / stepDelay).toInt().coerceAtLeast(1)
            for (step in 1..steps) {
                if (!isActive || splashCountdownStarted) break
                earlyProgressValue = targetProgress * step / steps
                applySplashProgress(earlyProgressValue, splashProgressMax)
                delay(stepDelay)
            }
            if (!splashCountdownStarted) {
                earlyProgressValue = targetProgress
                applySplashProgress(earlyProgressValue, splashProgressMax)
            }
        }
    }

    /**
     * Gán giá trị progress lên thanh loading và giới hạn giá trị trong khoảng hợp lệ.
     */
    private fun applySplashProgress(progress: Int, max: Int) {
        viewBinding.progressSplash.max = max
        viewBinding.progressSplash.progress = progress.coerceIn(0, max)
    }

    /**
     * Hiển thị message theo trạng thái hiện tại của Splash và tránh lặp lại message đã hiển thị.
     */
    private fun updateSplashMessage(status: SplashStatus) {
        val messageRes = when (status) {
            SplashStatus.FetchingRemoteConfig -> R.string.splash_fetching_remote_config
            SplashStatus.WaitingForInternet -> R.string.splash_waiting_for_internet
            SplashStatus.WaitingForConsent -> R.string.splash_waiting_for_consent
            SplashStatus.CountdownRunning -> R.string.splash_countdown_running
            SplashStatus.AdLoaded -> R.string.splash_ad_loaded
            SplashStatus.AdsUnavailable -> R.string.splash_ads_unavailable
            SplashStatus.ShowingAd -> R.string.splash_showing_ad
            SplashStatus.ReadyToEnterApp -> R.string.splash_ready_to_enter_app
        }
        if (messageHashMap.containsKey(messageRes)) return
        messageHashMap[messageRes] = messageRes
        viewBinding.tvMascotMessage.text = getString(messageRes)
    }

    /**
     * Cập nhật message theo tiến độ countdown để nội dung loading thay đổi tự nhiên hơn.
     */
    private fun updateCountdownMessage(progress: Int, max: Int) {
        if (currentSplashStatus != SplashStatus.CountdownRunning &&
            currentSplashStatus != SplashStatus.AdLoaded
        ) {
            return
        }
        val ratio = if (max <= 0) 0f else progress.toFloat() / max.toFloat()
        val messageRes = when {
            ratio < 0.45f -> R.string.splash_countdown_early
            ratio < 0.8f -> R.string.splash_countdown_mid
            else -> R.string.splash_countdown_late
        }
        if (messageHashMap.containsKey(messageRes)) return
        messageHashMap[messageRes] = messageRes
        viewBinding.tvMascotMessage.text = getString(messageRes)
    }

    /**
     * Quy đổi progress thật của countdown sang progress hiển thị, giữ lại phần progress đã chạy sớm.
     */
    override fun updateSplashProgress(progress: Int, max: Int) {
        splashProgressMax = max
        val displayProgress = if (splashCountdownStarted) {
            val initialOffset = (max * EARLY_PROGRESS_RATIO).toInt()
            val scaledCountdownProgress =
                ((progress.toFloat() / max.toFloat()) * (max - initialOffset)).toInt()
            initialOffset + scaledCountdownProgress
        } else {
            earlyProgressValue
        }
        applySplashProgress(displayProgress, max)
        if (splashCountdownStarted) {
            updateCountdownMessage(displayProgress, max)
        }
    }

    /**
     * Cung cấp vị trí quảng cáo banner/native cần preload và hiển thị trên màn Splash.
     */
    override fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> {
        return listOf(
            CoreAdPlaceName.ANCHORED_BOTTOM_SPLASH
        )
    }

    /**
     * Nhận kết quả load quảng cáo và bind vào view banner/native của Splash.
     */
    override fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
        viewBinding.bannerNative.processAdResource(
            adResource,
            CoreAdPlaceName.ANCHORED_BOTTOM_SPLASH
        )
    }

    /**
     * Tạo Intent đến màn tiếp theo theo cấu hình language/onboarding hiện tại.
     */
    private fun createSplashIntent(): Intent {
        return if (isEnableLanguageScreen) {
            timeShowIntro = System.currentTimeMillis()
            LanguageActivityNavigator.intentStart(
                this@SplashActivity,
                config = remoteConfigRepository.getLanguageActivityConfig(),
                fromSplash = true
            )
        } else if (!isEnableLanguageScreen && isEnableIntroductionScreen) {
            timeShowIntro = System.currentTimeMillis()
            Intent(
                this@SplashActivity,
                OnBoardingConfigFactory.getOnBoardingClass(remoteConfigRepository.getOnBoardingConfig())
            )
        } else {
            Intent(this@SplashActivity, startFlowNavigator.mainClass())
        }
    }

    /**
     * Xác định màn cần mở sau Splash, ưu tiên shortcut rồi mới xét luồng language/onboarding/main.
     */
    override fun openNextScreen() {
        val intentNext =
            when {
                targetScreenFromShortCut == StartFlowScreenType.Uninstall.screenName -> {
                    Intent(
                        this@SplashActivity,
                        startFlowNavigator.uninstallClass() ?: startFlowNavigator.mainClass()
                    ).apply {
                        val bundle = Bundle().apply {
                            putString(
                                StartFlowShortcut.KEY_SHORTCUT_TARGET_SCREEN,
                                targetScreenFromShortCut
                            )
                        }
                        putExtras(bundle)
                    }
                }

                /** Những case shortcut khác. */
                targetScreenFromShortCut?.isNotBlank() == true -> {
                    Intent(this@SplashActivity, startFlowNavigator.mainClass()).apply {
                        val bundle = Bundle().apply {
                            putString(
                                StartFlowShortcut.KEY_SHORTCUT_TARGET_SCREEN,
                                targetScreenFromShortCut
                            )
                        }
                        putExtras(bundle)
                    }
                }

                /** Case chưa vào màn main lần nào. */
                getCurrentLanguageCode().isBlank() && !appPreferences.isShowIntro -> {
                    Log.d(
                        TAG,
                        "checkAbleToNextScreen: getCurrentLanguageCode() ${getCurrentLanguageCode()} appPreferences.isShowIntro ${appPreferences.isShowIntro}"
                    )
                    createSplashIntent()
                }

                isAlwaysShowIntroAndLanguageScreen && !purchasePreferences.isUserVip() -> {
                    createSplashIntent()
                }

                else -> {
                    Intent(this@SplashActivity, startFlowNavigator.mainClass())
                }
            }
        intentNext.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        this@SplashActivity.startActivity(intentNext)
    }
}
