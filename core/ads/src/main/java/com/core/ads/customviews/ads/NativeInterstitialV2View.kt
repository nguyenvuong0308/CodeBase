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
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.core.ads.R
import com.core.ads.databinding.GntVersionV2Binding
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

class NativeInterstitialV2View @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BaseNativeTemplateView(context, attrs, defStyleAttr) {


    private val binding: GntVersionV2Binding by lazy {
        GntVersionV2Binding.inflate(LayoutInflater.from(context), this)
    }

    private var isEnableImmersive: Boolean = false
    private var closeCountDownTimer: CountDownTimer? = null
    private var closeCountDownSeconds: Long = DEFAULT_CLOSE_COUNTDOWN_SECONDS
    private var remainCloseMillis: Long = closeCountDownSeconds * 1000L
    private var isCloseCountdownFinished: Boolean = false
    private var countDownTheme: CountDownTheme = CountDownTheme.DARK

    private var hideTextCountDownUi: Boolean = false
    private var hideTextSkipCountDownUi: Boolean = false
    private var hideProgressCountDownUi: Boolean = false

    companion object {
        private const val DEFAULT_CLOSE_COUNTDOWN_SECONDS = 5L
    }

    private enum class CountDownTheme {
        LIGHT, DARK
    }

    init {
        initView(context)
    }

    private fun initView(context: Context) {}

    override fun setNativeAd(nativeAd: NativeAd) {
        binding.tvClose.setOnClickPreventingDouble {
            onClose?.invoke()
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
                binding.primary.setTextColor(it.toColorInt())
            }

            styles.tertiaryTextTypefaceColor?.let {
                binding.body.setTextColor(it.toColorInt())
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

            styles.backgroundResource?.let {
                binding.background.setBackgroundResource(it)
            }


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

            styles.backgroundAdsNotifyView?.let {
                binding.adNotificationView.setBackgroundResource(it)
            }
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
            closeCountDownSeconds = styles.countDownSecond
                ?.takeIf { it >= 0 }
                ?.toLong()
                ?: DEFAULT_CLOSE_COUNTDOWN_SECONDS
            hideTextCountDownUi = styles.hideTextCountDown == true
            hideTextSkipCountDownUi = styles.hideTextSkipCountDown == true
            hideProgressCountDownUi = styles.hideProgressCountDown == true
            countDownTheme = resolveCountDownTheme(styles)
            applyCountDownTheme()
            runCatching {
                styles.progressBarTint?.toColorInt()?.let { color ->
                    binding.progressCountDown.progressTintList = ColorStateList.valueOf(color)
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

    private fun resetCloseCountDown() {
        stopCloseCountDownTimer()
        isCloseCountdownFinished = false
        if (closeCountDownSeconds == 0L) {
            showCloseImmediately()
            return
        }
        remainCloseMillis = closeCountDownSeconds * 1000L
        binding.tvClose.visibility = GONE

        binding.lnCountDown.visibility = if (hideTextCountDownUi) GONE else VISIBLE
        binding.tvCountDownTitle.visibility = if (hideTextSkipCountDownUi) GONE else VISIBLE
        binding.progressCountDown.visibility = if (hideProgressCountDownUi) GONE else VISIBLE
        updateCountDownText(remainCloseMillis)
        updateCountDownProgress(remainCloseMillis)
    }

    private fun startCloseCountDown() {
        if (isCloseCountdownFinished) return
        if (remainCloseMillis <= 0L) {
            showCloseImmediately()
            return
        }
        stopCloseCountDownTimer()
        closeCountDownTimer = object : CountDownTimer(remainCloseMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainCloseMillis = millisUntilFinished
                updateCountDownText(millisUntilFinished)
                updateCountDownProgress(millisUntilFinished)
            }

            override fun onFinish() {
                remainCloseMillis = 0L
                isCloseCountdownFinished = true
                binding.lnCountDown.visibility = INVISIBLE
                binding.progressCountDown.visibility = GONE
                binding.tvClose.visibility = VISIBLE
            }
        }.start()
    }

    private fun showCloseImmediately() {
        stopCloseCountDownTimer()
        remainCloseMillis = 0L
        isCloseCountdownFinished = true
        binding.lnCountDown.visibility = INVISIBLE
        binding.progressCountDown.visibility = GONE
        binding.tvClose.visibility = VISIBLE
    }

    private fun updateCountDownText(millis: Long) {
        val seconds = ((millis + 999L) / 1000L).coerceAtLeast(0L)
        binding.tvCountDownTime.text =
            context.getString(R.string.ads_countdown_time_format, seconds.toInt())
    }

    private fun updateCountDownProgress(millis: Long) {
        val totalMillis = (closeCountDownSeconds * 1000L).coerceAtLeast(1L)
        val elapsedMillis = (totalMillis - millis.coerceAtLeast(0L)).coerceAtLeast(0L)
        val progress = ((elapsedMillis * 1000L) / totalMillis).toInt().coerceIn(0, 1000)
        binding.progressCountDown.progress = progress
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
                binding.lnCountDown.setBackgroundResource(R.drawable.bg_countdown_v1)
                binding.tvClose.setBackgroundResource(R.drawable.bg_close_v1)
                binding.tvClose.setColorFilter(
                    ContextCompat.getColor(context, R.color.countdown_close_icon_dark)
                )
                binding.tvCountDownTitle.setTextColor(
                    ContextCompat.getColor(context, R.color.countdown_dark_title)
                )
                binding.tvCountDownTime.setTextColor(
                    ContextCompat.getColor(context, R.color.countdown_dark_time)
                )
                binding.progressCountDown.progressTintList =
                    ContextCompat.getColorStateList(context, R.color.countdown_dark_time)
//                binding.mediaView.setBackgroundColor(
//                    ContextCompat.getColor(context, R.color.media_theme_dark_background)
//                )
            }

            CountDownTheme.LIGHT -> {
                binding.lnCountDown.setBackgroundResource(R.drawable.bg_countdown_light_v1)
                binding.tvClose.setBackgroundResource(R.drawable.bg_close_light_v1)
                binding.tvClose.setColorFilter(
                    ContextCompat.getColor(context, R.color.countdown_close_icon_light)
                )
                binding.tvCountDownTitle.setTextColor(
                    ContextCompat.getColor(context, R.color.countdown_light_title)
                )
                binding.tvCountDownTime.setTextColor(
                    ContextCompat.getColor(context, R.color.countdown_light_time)
                )
                binding.progressCountDown.progressTintList =
                    ContextCompat.getColorStateList(context, R.color.countdown_light_time)
//                binding.mediaView.setBackgroundColor(
//                    ContextCompat.getColor(context, R.color.media_theme_light_background)
//                )
            }
        }
    }

    private fun applyCountDownVisibility() {
        if (isCloseCountdownFinished) {
            binding.lnCountDown.visibility = INVISIBLE
            binding.progressCountDown.visibility = GONE
            binding.tvClose.visibility = VISIBLE
            return
        }
        binding.tvClose.visibility = GONE
        binding.lnCountDown.visibility = if (hideTextCountDownUi) GONE else VISIBLE
        binding.tvCountDownTitle.visibility =
            if (hideTextSkipCountDownUi) GONE else VISIBLE
        binding.progressCountDown.visibility =
            if (hideProgressCountDownUi) GONE else VISIBLE
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
