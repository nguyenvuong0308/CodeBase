package com.core.ads.customviews.ads

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.core.ads.R
import com.core.ads.databinding.GntVersionV3Binding
import com.core.ads.extensions.updateBackgroundColor
import com.core.ads.extensions.updateRadius
import com.core.ads.glidetransformation.RoundedCornersTransformation
import com.core.utilities.isValidGlideContext
import com.core.utilities.margin
import com.core.utilities.setOnClickPreventingDouble
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.nativead.NativeAd
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics

class NativeInterstitialV3View @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BaseNativeTemplateView(context, attrs, defStyleAttr) {


    private val binding: GntVersionV3Binding by lazy {
        GntVersionV3Binding.inflate(LayoutInflater.from(context), this)
    }

    private var isEnableImmersive: Boolean = false
    private var closeCountDownTimer: CountDownTimer? = null
    private var step1CountDownSeconds: Long = DEFAULT_CLOSE_COUNTDOWN_SECONDS
    private var step2CountDownSeconds: Long = DEFAULT_CLOSE_COUNTDOWN_SECONDS
    private var remainCloseMillis: Long = step1CountDownSeconds * 1000L
    private var isCloseCountdownFinished: Boolean = false
    private var closeStep: CloseStep = CloseStep.NEXT
    private var closeStepCount: Int = DEFAULT_CLOSE_STEP_COUNT
    private var isStepActionEnabled: Boolean = false
    private var countDownTheme: CountDownTheme = CountDownTheme.DARK

    private var hideTextCountDownUi: Boolean = false
    private var hideTextSkipCountDownUi: Boolean = false
    private var hideProgressCountDownUi: Boolean = false
    private var controlCloseBaseTopMargin: Int? = null
    private var controlCloseBaseHorizontalMargin: Int? = null
    private var controlClosePosition: ControlClosePosition = ControlClosePosition.RIGHT

    companion object {
        private const val DEFAULT_CLOSE_COUNTDOWN_SECONDS = 5L
        private const val DEFAULT_CLOSE_STEP_COUNT = 2
        private const val PROGRESS_MAX = 1000
        private const val COUNTDOWN_INTERVAL_MILLIS = 50L
    }

    private enum class CountDownTheme {
        LIGHT, DARK
    }

    private enum class CloseStep {
        NEXT, CLOSE
    }

    private enum class ControlClosePosition {
        LEFT, RIGHT
    }

    init {
        initView(context)
    }

    private fun initView(context: Context) {
        applyDisplayCutoutSpacing()
    }

    private fun applyDisplayCutoutSpacing() {
        controlCloseBaseTopMargin =
            (binding.controlClose.layoutParams as? ConstraintLayout.LayoutParams)?.topMargin
        controlCloseBaseHorizontalMargin =
            (binding.controlClose.layoutParams as? ConstraintLayout.LayoutParams)?.marginEnd

        ViewCompat.setOnApplyWindowInsetsListener(binding.nativeAdView) { _, insets ->
            val cutoutTop = insets.getInsets(WindowInsetsCompat.Type.displayCutout()).top
            val parentTopPadding = (parent as? View)?.paddingTop ?: 0
            val extraTop = (cutoutTop - parentTopPadding).coerceAtLeast(0)
            val baseTopMargin = controlCloseBaseTopMargin ?: 0

            (binding.controlClose.layoutParams as? ConstraintLayout.LayoutParams)?.let { params ->
                val targetTopMargin = baseTopMargin + extraTop
                if (params.topMargin != targetTopMargin) {
                    params.topMargin = targetTopMargin
                    binding.controlClose.layoutParams = params
                }
            }
            insets
        }
        binding.nativeAdView.post {
            ViewCompat.requestApplyInsets(binding.nativeAdView)
        }
    }

    override fun setNativeAd(nativeAd: NativeAd) {
        binding.tvClose.setOnClickPreventingDouble {
            if (closeStep == CloseStep.CLOSE && isStepActionEnabled) {
                onClose?.invoke()
            }
        }
        binding.tvNext.setOnClickPreventingDouble {
            if (closeStep == CloseStep.NEXT && isStepActionEnabled) {
                showCloseStep()
            }
        }
        resetCloseCountDown()
        startCloseCountDown()
        binding.nativeAdView.callToActionView = binding.cta
        binding.nativeAdView.headlineView = binding.primary
        binding.nativeAdView.mediaView = binding.mediaView
        binding.mediaView.setImageScaleType(ImageView.ScaleType.FIT_CENTER)

        binding.primary.text = nativeAd.headline
        binding.cta.text = nativeAd.callToAction

        binding.icon.visibility = GONE
        nativeAd.icon?.let {
            binding.icon.visibility = VISIBLE
            if (context.isValidGlideContext()) {
                Glide.with(this)
                    .load(it.drawable)
                    .override(resources.getDimensionPixelSize(com.core.dimens.R.dimen._44dp))
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .apply(
                        RequestOptions.bitmapTransform(
                            RoundedCornersTransformation(
                                context.resources.getDimensionPixelSize(
                                    com.core.dimens.R.dimen._8dp
                                ), 0, RoundedCornersTransformation.CornerType.ALL
                            )
                        )
                    )
                    .into(binding.icon)
            }
        }

        nativeAd.body?.let {
            binding.body.text = it
            binding.nativeAdView.bodyView = binding.body
        }

//        val extras = nativeAd.extras
//        if (extras.containsKey(FacebookMediationAdapter.KEY_SOCIAL_CONTEXT_ASSET)) {
//            val socialContext = extras.get(FacebookMediationAdapter.KEY_SOCIAL_CONTEXT_ASSET)
//            if (socialContext is String) {
//                if (binding.primary.text.isBlank()) {
//                    binding.primary.text = socialContext
//                } else {
//                    if (binding.body.text.isBlank()) {
//                        binding.body.text = socialContext
//                    }
//                }
//            }
//        }

        binding.nativeAdView.setNativeAd(nativeAd)

        // Get the video controller for the ad. One will always be provided,
        // even if the ad doesn't have a video asset.
        val videoController = nativeAd.mediaContent?.videoController ?: return

        // Updates the UI to say whether or not this ad has a video asset.
        if (videoController.hasVideoContent()) {
            // Create a new VideoLifecycleCallbacks object and pass it to the VideoController.
            // The VideoController will call methods on this object when events occur in the
            // video lifecycle.
            videoController.videoLifecycleCallbacks =
                object : VideoController.VideoLifecycleCallbacks() {
                }
        }

        if (isEnableImmersive && (nativeAd.mediaContent?.aspectRatio
                ?: 1f) < 1f
        ) { // Nếu bật chế độ trong suốt và mediaview dạng dọc thì hiển thị native dạng trong suốt
            binding.background.margin(left = 0, right = 0)
            binding.background.background = null
            binding.adNotificationView.setBackgroundResource(R.drawable.gnt_rounded_bottom_corner_shape)
            reapplyAdsNotifyViewStyles(binding.adNotificationView)
            binding.primary.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.neutral_dark_primary
                )
            )
            binding.primary.setShadowLayer(10f, 2f, 2f, Color.BLACK)
            binding.body.setTextColor(ContextCompat.getColor(context, R.color.neutral_dark_primary))
            binding.body.setShadowLayer(10f, 2f, 2f, Color.BLACK)
            return
        }

    }

    /**
     * To prevent memory leaks, make sure to destroy your ad when you don't need it anymore. This
     * method does not destroy the template view.
     * https://developers.google.com/admob/adroid/native-unified#destroy_ad
     */
    override fun destroyNativeAd() {
        stopCloseCountDownTimer()
        binding.nativeAdView.destroy()
    }

    override fun onHostPause() {
        pauseCloseCountDown()
    }

    override fun onHostResume() {
        resumeCloseCountDown()
    }

    override fun applyStyles(styles: NativeTemplateStyle) {
        runCatching {
            styles.mainBackgroundColor?.let {
                binding.background.background = it
                binding.primary.background = it
                binding.body.background = it
            }

            styles.mediaBackgroundColor?.let {
                run {
                    val color = it.toColorInt()
                    binding.mediaView.setBackgroundColor(color)
                }
            }

            styles.primaryTextTypeface?.let {
                binding.primary.typeface = it
            }

            styles.tertiaryTextTypeface?.let {
                binding.body.typeface = it
            }

            styles.callToActionTextTypeface?.let {
                binding.cta.typeface = it
            }

            styles.primaryTextTypefaceColor?.let {
                // V3 luôn dùng panel dưới màu tối nên không lấy màu chữ cũ vốn dành cho card sáng.
                binding.primary.setTextColor(ContextCompat.getColor(context, R.color.white))
            }

            styles.tertiaryTextTypefaceColor?.let {
                binding.body.setTextColor("#BDBDBD".toColorInt())
            }

            styles.callToActionTypefaceColor?.let {
                binding.cta.setTextColor(it)
            }

            val ctaTextSize = styles.callToActionTextSize
            if (ctaTextSize > 0) {
                binding.cta.textSize = ctaTextSize
            }

            val primaryTextSize = styles.primaryTextSize
            if (primaryTextSize > 0) {
                binding.primary.textSize = primaryTextSize
            }


            val tertiaryTextSize = styles.tertiaryTextSize
            if (tertiaryTextSize > 0) {
                binding.body.textSize = tertiaryTextSize
            }

            styles.callToActionBackgroundColor?.let {
                binding.layoutCta.updateBackgroundColor(it)
            }

            binding.background.setBackgroundResource(R.drawable.bg_native_interstitial_bottom)


            styles.callToActionRadius?.let {
                binding.layoutCta.updateRadius(it.toFloat())
            }

            styles.borderColor?.let {
                (binding.background.background as GradientDrawable).setStroke(
                    resources.getDimensionPixelSize(
                        com.core.dimens.R.dimen._1dp
                    ), it.toColorInt()
                )
            }


            styles.backgroundColor?.let {
                binding.background.updateBackgroundColor(it)
            }

            styles.backgroundFullColor?.let {
                binding.nativeAdView.setBackgroundColor(
                    runCatching { it.toColorInt() }.getOrNull() ?: Color.WHITE
                )
            }

            binding.adNotificationView.setBackgroundResource(R.drawable.bg_native_interstitial_ad_badge)
            applyAdsNotifyViewStyles(styles, binding.adNotificationView)

            styles.primaryTextBackgroundColor?.let {
                binding.primary.background = it
            }

            styles.tertiaryTextBackgroundColor?.let {
                binding.body.background = it
            }

            styles.isEnableImmersive?.let {
                isEnableImmersive = it
            }
            // Template chỉ hỗ trợ 1 bước (close) hoặc 2 bước (next rồi close).
            // Giá trị ngoài miền hợp lệ sẽ dùng mặc định 2 bước để tránh vô tình bỏ qua step next.
            closeStepCount = styles.closeStepCount
                ?.takeIf { it in 1..2 }
                ?: DEFAULT_CLOSE_STEP_COUNT
            val fallbackCountDownSeconds = styles.countDownSecond
                ?.takeIf { it >= 0 }
                ?: DEFAULT_CLOSE_COUNTDOWN_SECONDS.toInt()
            step1CountDownSeconds = (styles.step1CountDownSecond
                ?.takeIf { it >= 0 }
                ?: fallbackCountDownSeconds).toLong()
            step2CountDownSeconds = (styles.step2CountDownSecond
                ?.takeIf { it >= 0 }
                ?: fallbackCountDownSeconds).toLong()
            hideTextCountDownUi = styles.hideTextCountDown == true
            hideTextSkipCountDownUi = styles.hideTextSkipCountDown == true
            hideProgressCountDownUi = styles.hideProgressCountDown == true
            countDownTheme = resolveCountDownTheme(styles)
            applyCountDownTheme()
            controlClosePosition = resolveControlClosePosition(styles.controlClosePosition)
            applyControlClosePosition()
            runCatching {
                styles.progressBarTint?.toColorInt()?.let { color ->
                    binding.progressCountDown.progressTintList = ColorStateList.valueOf(color)
                    binding.progressCountDownSecond.progressTintList = ColorStateList.valueOf(color)
                }
            }
            applyCountDownVisibility()

            styles.backgroundRadius?.let { radius ->
                val bg = binding.nativeAdView.background
                if (bg is GradientDrawable) {
                    val radiusPx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        radius.toFloat(),
                        resources.displayMetrics
                    )
                    bg.cornerRadius = radiusPx
                }
            }

            styles.callToActionBorderColor?.let {
                (binding.layoutCta.background as? GradientDrawable)?.setStroke(
                    resources.getDimensionPixelSize(
                        com.core.dimens.R.dimen._1dp
                    ), it.toColorInt()
                )
            }
            invalidate()
            requestLayout()
        }.onFailure {
            Firebase.analytics.logEvent("error_native_style", null)
        }
    }

    private fun resolveControlClosePosition(position: String?): ControlClosePosition {
        return when (position?.trim()?.lowercase()) {
            "left", "start" -> ControlClosePosition.LEFT
            "right", "end" -> ControlClosePosition.RIGHT
            else -> ControlClosePosition.RIGHT
        }
    }

    private fun applyControlClosePosition() {
        val params =
            binding.controlClose.layoutParams as? ConstraintLayout.LayoutParams ?: return
        val horizontalMargin = controlCloseBaseHorizontalMargin
            ?: resources.getDimensionPixelSize(com.core.dimens.R.dimen._16dp)

        params.startToStart = ConstraintLayout.LayoutParams.UNSET
        params.startToEnd = ConstraintLayout.LayoutParams.UNSET
        params.endToStart = ConstraintLayout.LayoutParams.UNSET
        params.endToEnd = ConstraintLayout.LayoutParams.UNSET

        when (controlClosePosition) {
            ControlClosePosition.LEFT -> {
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                params.marginStart = horizontalMargin
                params.marginEnd = 0
            }

            ControlClosePosition.RIGHT -> {
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                params.marginStart = 0
                params.marginEnd = horizontalMargin
            }
        }

        binding.controlClose.layoutParams = params
    }

    private fun resetCloseCountDown() {
        stopCloseCountDownTimer()
        // Chế độ 1 bước đếm ngược trực tiếp cho nút close; chế độ 2 bước bắt đầu bằng next.
        closeStep = if (closeStepCount == 1) CloseStep.CLOSE else CloseStep.NEXT
        isStepActionEnabled = false
        isCloseCountdownFinished = false
        remainCloseMillis = getCurrentStepCountDownSeconds() * 1000L
        binding.actionProgress.progress = 0
        binding.progressCountDown.progress = 0
        binding.progressCountDownSecond.progress = 0
        binding.lnCountDown.visibility = GONE
        binding.tvCountDownTitle.visibility = GONE
        binding.tvCountDownTime.visibility = GONE
        updateCountDownText(remainCloseMillis)
        updateCountDownProgress(remainCloseMillis, closeStep)
        applyCountDownVisibility()
        if (getCurrentStepCountDownSeconds() == 0L) {
            completeCurrentStep()
        }
    }

    private fun startCloseCountDown() {
        if (isCloseCountdownFinished || isStepActionEnabled) return
        if (remainCloseMillis <= 0L) {
            completeCurrentStep()
            return
        }
        stopCloseCountDownTimer()
        closeCountDownTimer = object : CountDownTimer(remainCloseMillis, COUNTDOWN_INTERVAL_MILLIS) {
            override fun onTick(millisUntilFinished: Long) {
                remainCloseMillis = millisUntilFinished
                updateCountDownText(millisUntilFinished)
                updateCountDownProgress(millisUntilFinished, closeStep)
            }

            override fun onFinish() {
                remainCloseMillis = 0L
                completeCurrentStep()
            }
        }.start()
    }

    private fun completeCurrentStep() {
        stopCloseCountDownTimer()
        remainCloseMillis = 0L
        isStepActionEnabled = true
        updateCountDownProgress(0L, closeStep)
        applyCountDownVisibility()
    }

    private fun showCloseStep() {
        stopCloseCountDownTimer()
        closeStep = CloseStep.CLOSE
        isStepActionEnabled = false
        isCloseCountdownFinished = false
        remainCloseMillis = getCurrentStepCountDownSeconds() * 1000L
        binding.actionProgress.progress = 0
        binding.progressCountDown.progress = PROGRESS_MAX
        binding.progressCountDownSecond.progress = 0
        updateCountDownText(remainCloseMillis)
        updateCountDownProgress(remainCloseMillis, CloseStep.CLOSE)
        applyCountDownVisibility()
        if (getCurrentStepCountDownSeconds() == 0L) {
            completeCurrentStep()
        } else {
            startCloseCountDown()
        }
    }

    private fun updateCountDownText(millis: Long) {
        val seconds = ((millis + 999L) / 1000L).coerceAtLeast(0L)
        binding.tvCountDownTime.text =
            context.getString(R.string.ads_countdown_time_format, seconds.toInt())
    }

    private fun updateCountDownProgress(millis: Long, step: CloseStep) {
        val totalMillis = (getCurrentStepCountDownSeconds() * 1000L).coerceAtLeast(1L)
        val elapsedMillis = (totalMillis - millis.coerceAtLeast(0L)).coerceAtLeast(0L)
        val progress = ((elapsedMillis * PROGRESS_MAX) / totalMillis).toInt().coerceIn(0, PROGRESS_MAX)
        binding.actionProgress.progress = progress
        // Chế độ 1 bước chỉ cập nhật progress đầu; progress thứ hai dành cho bước close của chế độ 2 bước.
        if (closeStepCount == 1) {
            binding.progressCountDown.progress = progress
            binding.progressCountDownSecond.progress = 0
            return
        }
        when (step) {
            CloseStep.NEXT -> {
                binding.progressCountDown.progress = progress
                binding.progressCountDownSecond.progress = 0
            }

            CloseStep.CLOSE -> {
                binding.progressCountDown.progress = PROGRESS_MAX
                binding.progressCountDownSecond.progress = progress
            }
        }
    }

    private fun getCurrentStepCountDownSeconds(): Long {
        return when (closeStep) {
            CloseStep.NEXT -> step1CountDownSeconds
            CloseStep.CLOSE -> if (closeStepCount == 1) step1CountDownSeconds else step2CountDownSeconds
        }
    }

    private fun resolveCountDownTheme(styles: NativeTemplateStyle): CountDownTheme {
        val colorString =
            styles.backgroundFullColor ?: styles.backgroundColor ?: return CountDownTheme.DARK
        val bgColor =
            runCatching { colorString.toColorInt() }.getOrNull() ?: return CountDownTheme.DARK
        return if (ColorUtils.calculateLuminance(bgColor) >= 0.6) {
            CountDownTheme.LIGHT
        } else {
            CountDownTheme.DARK
        }
    }

    private fun applyCountDownTheme() {
        when (countDownTheme) {
            CountDownTheme.DARK -> {
                binding.tvNext.setBackgroundResource(R.drawable.bg_native_interstitial_action)
                binding.tvClose.setBackgroundResource(R.drawable.bg_native_interstitial_action)
                binding.tvNext.setColorFilter(ContextCompat.getColor(context, R.color.white))
                binding.tvClose.setColorFilter(
                    ContextCompat.getColor(context, R.color.white)
                )
                binding.progressCountDown.progressTintList =
                    ContextCompat.getColorStateList(context, R.color.white)
                binding.progressCountDownSecond.progressTintList =
                    ContextCompat.getColorStateList(context, R.color.white)
                binding.actionProgress.setIndicatorColor("#6F6F6F".toColorInt())
            }

            CountDownTheme.LIGHT -> {
                binding.tvNext.setBackgroundResource(R.drawable.bg_native_interstitial_action)
                binding.tvClose.setBackgroundResource(R.drawable.bg_native_interstitial_action)
                binding.tvNext.setColorFilter(ContextCompat.getColor(context, R.color.white))
                binding.tvClose.setColorFilter(
                    ContextCompat.getColor(context, R.color.white)
                )
                binding.progressCountDown.progressTintList =
                    ContextCompat.getColorStateList(context, R.color.white)
                binding.progressCountDownSecond.progressTintList =
                    ContextCompat.getColorStateList(context, R.color.white)
                binding.actionProgress.setIndicatorColor("#6F6F6F".toColorInt())
            }
        }
    }

    private fun applyCountDownVisibility() {
        binding.lnCountDown.visibility = GONE
        binding.tvCountDownTitle.visibility = GONE
        binding.tvCountDownTime.visibility = GONE
        binding.actionProgress.visibility = if (hideProgressCountDownUi) GONE else VISIBLE
        binding.progressContainer.visibility = if (hideProgressCountDownUi) GONE else VISIBLE
        // Chế độ 1 bước ẩn progress thứ hai và bỏ luôn khoảng cách giữa hai progress.
        binding.progressCountDownSecond.visibility =
            if (hideProgressCountDownUi || closeStepCount == 1) GONE else VISIBLE
        (binding.progressCountDown.layoutParams as? LinearLayout.LayoutParams)?.let { params ->
            params.marginEnd = if (closeStepCount == 1) {
                0
            } else {
                resources.getDimensionPixelSize(com.core.dimens.R.dimen._4dp)
            }
            binding.progressCountDown.layoutParams = params
        }

        val isNextStep = closeStep == CloseStep.NEXT
        binding.tvNext.visibility = if (isNextStep) VISIBLE else GONE
        binding.tvClose.visibility = if (isNextStep) GONE else VISIBLE
        binding.tvNext.isEnabled = isNextStep && isStepActionEnabled
        binding.tvClose.isEnabled = !isNextStep && isStepActionEnabled
        binding.tvNext.alpha = if (isNextStep && isStepActionEnabled) 1f else 0.65f
        binding.tvClose.alpha = if (!isNextStep && isStepActionEnabled) 1f else 0.65f
        isCloseCountdownFinished = !isNextStep && isStepActionEnabled
    }

    private fun stopCloseCountDownTimer() {
        closeCountDownTimer?.cancel()
        closeCountDownTimer = null
    }

    fun pauseCloseCountDown() {
        if (isCloseCountdownFinished) return
        stopCloseCountDownTimer()
    }

    fun resumeCloseCountDown() {
        if (isCloseCountdownFinished || closeCountDownTimer != null) return
        startCloseCountDown()
    }
}
