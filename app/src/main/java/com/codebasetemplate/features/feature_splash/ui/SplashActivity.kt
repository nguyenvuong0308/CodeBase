package com.codebasetemplate.features.feature_splash.ui

import android.view.LayoutInflater
import androidx.activity.viewModels
import com.codebasetemplate.databinding.CoreActivitySplashBinding
import com.codebasetemplate.required.ads.AppAdPlaceName
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.baseui.R
import com.core.baseui.ext.bindLiveData
import com.core.config.domain.data.IAdPlaceName
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashActivity : BaseSplashActivity<CoreActivitySplashBinding>() {

    companion object {
        private const val EARLY_PROGRESS_RATIO = 0.2f
        private const val EARLY_PROGRESS_DURATION_MS = 1_200L
    }
    private var earlyProgressJob: Job? = null
    private var splashCountdownStarted = false
    private var splashProgressMax = 15_000
    private var earlyProgressValue = 0

    private var currentSplashStatus = SplashStatus.FetchingRemoteConfig

    private var messageHashMap = HashMap<Int, Int>()

    private val viewModel by viewModels<SplashLoadDataViewModel>()
    override fun bindingProvider(inflater: LayoutInflater): CoreActivitySplashBinding {
        return CoreActivitySplashBinding.inflate(inflater)
    }

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

    override fun hideLoadingX() {
    }

    override fun onSplashStatusChanged(status: SplashStatus) {
        currentSplashStatus = status
        updateSplashMessage(status)
    }

    override fun onSplashCountdownStarted(max: Int) {
        splashCountdownStarted = true
        splashProgressMax = max
        earlyProgressJob?.cancel()
        earlyProgressJob = null
        applySplashProgress((max * EARLY_PROGRESS_RATIO).toInt(), max)
    }

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

    private fun applySplashProgress(progress: Int, max: Int) {
        viewBinding.progressSplash.max = max
        viewBinding.progressSplash.progress = progress.coerceIn(0, max)
    }

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

    override fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> {
        return listOf(
            AppAdPlaceName.ANCHORED_BOTTOM_SPLASH
        )
    }

    override fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
        viewBinding.bannerNative.processAdResource(adResource, AppAdPlaceName.ANCHORED_BOTTOM_SPLASH)
    }

}
