package com.core.ads.customviews.ads

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.CountDownTimer
import android.os.SystemClock
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.graphics.toColorInt
import androidx.core.graphics.ColorUtils
import com.core.ads.R
import com.core.ads.extensions.updateBackgroundColor
import com.core.ads.extensions.updateRadius
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.google.android.material.progressindicator.CircularProgressIndicator

/** Shared ad binding for V4–V6. Geometry belongs to their portrait/landscape XML resources. */
abstract class NativeInterstitialConceptView(
    context: Context, attrs: AttributeSet?, defStyleAttr: Int, @LayoutRes layout: Int,
) : BaseNativeTemplateView(context, attrs, defStyleAttr) {
    init { LayoutInflater.from(context).inflate(layout, this, true) }

    private val nativeView = findViewById<NativeAdView>(R.id.native_ad_view)
    private val media = findViewById<MediaView>(R.id.media_view)
    private val card = findViewById<View>(R.id.background)
    private val primary = findViewById<TextView>(R.id.primary)
    private val body = findViewById<TextView>(R.id.body)
    private val icon = findViewById<ImageView>(R.id.icon)
    private val cta = findViewById<TextView>(R.id.cta)
    private val ctaContainer = findViewById<View>(R.id.layout_cta)
    private val close = findViewById<ImageView>(R.id.tvClose)
    private val closeContainer = findViewById<View>(R.id.controlClose)
    private val countdown = findViewById<View>(R.id.lnCountDown)
    private val countdownText = findViewById<TextView>(R.id.tvCountDownTime)
    private val closeProgress = findViewById<CircularProgressIndicator>(R.id.actionProgress)
    private val badge = findViewById<TextView>(R.id.ad_notification_view)
    private var timer: CountDownTimer? = null
    private var durationMs = 5_000L
    private var remainingMs = 0L
    private var deadlineMs = 0L
    private var started = false
    private var hostPaused = false
    private var closed = false
    private var hideCountdown = false
    private var showCloseButton = false
    private var hideProgress = false
    private var activeDurationMs = 0L
    private var closePositionConfig: String? = null
    private val closePositionResolver = NativeClosePositionResolver()
    private var compactCloseWhenReady = false
    private var closeBackground = closeContainer.background
    private var baseStyle: NativeTemplateStyle? = null
    private val themeResolver = NativeThemeResolver()
    private var restoreBaseColors: (() -> Unit)? = null
    var resolvedNativeTheme: String? = null
        private set

    init {
        close.setOnClickListener {
            if (started && remainingMs == 0L && !closed) {
                closed = true
                renderCountdown()
                onClose?.invoke()
            }
        }
        // The compact icon is 28dp; retain the existing invisible 48dp touch area.
        closeContainer.setOnClickListener { if (close.isEnabled) close.performClick() }
    }

    override fun setNativeAd(nativeAd: NativeAd) {
        primary.text = nativeAd.headline
        body.text = nativeAd.body
        body.visibility = if (nativeAd.body.isNullOrBlank()) GONE else VISIBLE
        cta.text = nativeAd.callToAction
        ctaContainer.visibility = if (nativeAd.callToAction.isNullOrBlank()) GONE else VISIBLE
        icon.setImageDrawable(nativeAd.icon?.drawable)
        icon.visibility = if (nativeAd.icon == null) GONE else VISIBLE
        nativeView.headlineView = primary
        nativeView.bodyView = body
        nativeView.callToActionView = cta
        nativeView.iconView = icon
        media.imageScaleType = ImageView.ScaleType.FIT_CENTER
        nativeView.registerNativeAd(nativeAd, media)
        startCloseCountdown()
    }

    // Kept separate from SDK registration so lifecycle behavior can be tested without a live ad.
    internal fun startCloseCountdown() {
        timer?.cancel()
        timer = null
        remainingMs = durationMs
        activeDurationMs = durationMs
        started = true
        closed = false
        closePositionResolver.beginAd()
        themeResolver.beginAd()
        baseStyle?.let { applyStyles(it) }
        applyControlClosePosition()
        renderCountdown()
        resumeCountdown()
    }

    override fun applyStyles(styles: NativeTemplateStyle) {
        baseStyle = styles
        resolvedNativeTheme = if (started) themeResolver.resolve(styles.nativeTheme) else null
        // Remove the previous palette before applying base fields. In particular, missing
        // or invalid fields must never inherit colors from the preceding random theme.
        restoreBaseColors?.invoke()
        restoreBaseColors = null
        applyResolvedStyles(styles)
        val themed = styles.forTheme(resolvedNativeTheme)
        if (themed !== styles) {
            restoreBaseColors = captureBaseColors()
            applyResolvedStyles(themed)
        }
    }

    private fun captureBaseColors(): () -> Unit {
        // Copy drawable state before palette styling mutates fills, gradients or strokes.
        val backgrounds = listOf<View>(nativeView, card, media, ctaContainer, badge).map { view ->
            view to view.background?.let { it.constantState?.newDrawable(resources)?.mutate() ?: it }
        }
        val closeSurface = closeBackground?.let { it.constantState?.newDrawable(resources)?.mutate() ?: it }
        val textColors = listOf(primary, body, cta, countdownText, badge).map { it to it.textColors }
        val closeFilter = close.colorFilter
        return {
            backgrounds.forEach { (view, background) -> view.background = background }
            closeBackground = closeSurface
            textColors.forEach { (view, colors) -> view.setTextColor(colors) }
            close.colorFilter = closeFilter
        }
    }

    private fun applyResolvedStyles(styles: NativeTemplateStyle) {
        // Restore the surface before recoloring even if the previous ready style hid it.
        closeContainer.background = closeBackground
        styles.mainBackgroundColor?.let { card.background = it.constantState?.newDrawable()?.mutate() }
        styles.backgroundResource?.let { card.setBackgroundResource(it) }
        styles.backgroundColor?.let { card.updateBackgroundColor(it); closeContainer.updateBackgroundColor(it) }
        closeBackground = closeContainer.background
        styles.backgroundFullColor.toColorIntOrNull()?.let { nativeView.setBackgroundColor(it) }
        styles.mediaBackgroundColor?.let {
            if (media.background == null || media.background is android.graphics.drawable.ColorDrawable) {
                it.toColorIntOrNull()?.let { color -> media.setBackgroundColor(color) }
            }
            else media.updateBackgroundColor(it)
        }
        styles.backgroundRadius?.let { card.updateRadius(it.toFloat()) }
        styles.borderColor.toColorIntOrNull()?.let { (card.background as? GradientDrawable)?.setStroke(dp(1), it) }
        styles.primaryTextTypeface?.let { primary.typeface = it }
        styles.tertiaryTextTypeface?.let { body.typeface = it }
        styles.callToActionTextTypeface?.let { cta.typeface = it }
        styles.primaryTextTypefaceColor.toColorIntOrNull()?.let {
            primary.setTextColor(it)
            close.setColorFilter(it)
            countdownText.setTextColor(it)
        }
        styles.tertiaryTextTypefaceColor.toColorIntOrNull()?.let { body.setTextColor(it) }
        styles.callToActionTypefaceColor?.let { cta.setTextColor(it) }
        styles.primaryTextBackgroundColor?.let { primary.background = it }
        styles.tertiaryTextBackgroundColor?.let { body.background = it }
        primary.applyTextSizeFromDpDimen(styles.primaryTextSize)
        body.applyTextSizeFromDpDimen(styles.tertiaryTextSize)
        cta.applyTextSizeFromDpDimen(styles.callToActionTextSize)
        styles.callToActionBackgroundColor?.let { ctaContainer.updateBackgroundColor(it) }
        styles.callToActionRadius?.let { ctaContainer.updateRadius(it.toFloat()) }
        styles.callToActionBorderColor.toColorIntOrNull()?.let { (ctaContainer.background as? GradientDrawable)?.setStroke(dp(1), it) }
        styles.backgroundAdsNotifyView?.let { badge.setBackgroundResource(it) }
        applyAdsNotifyViewStyles(styles, badge)
        durationMs = (styles.countDownSecond ?: 5).coerceAtLeast(0).toLong() * 1_000L
        hideCountdown = styles.hideTextCountDown == true
        showCloseButton = styles.controlCloseStyle == "close"
        compactCloseWhenReady = styles.controlCloseReadyStyle == "compact"
        hideProgress = styles.hideProgressCountDown == true
        val progressColor = styles.progressBarTint.toColorIntOrNull()
            ?: styles.primaryTextTypefaceColor.toColorIntOrNull()
            ?: countdownText.currentTextColor
        closeProgress.setIndicatorColor(progressColor)
        closeProgress.trackColor = ColorUtils.setAlphaComponent(progressColor, Color.alpha(progressColor) / 4)
        closePositionConfig = styles.controlClosePosition
        applyControlClosePosition()
        renderCountdown()
        requestLayout()
    }

    // Remote colors are optional overrides: a bad field must not abort ad binding.
    private fun String?.toColorIntOrNull(): Int? = this?.let {
        runCatching { it.toColorInt() }.getOrNull()
    }

    private fun applyControlClosePosition() {
        // Do not draw a random value during pre-bind styling. Bind chooses once for this ad.
        val waitingForShuffle = !started && closePositionConfig?.trim()?.equals("shuffle", ignoreCase = true) == true
        val closeOnLeft = !waitingForShuffle && closePositionResolver.isLeft(closePositionConfig)
        (closeContainer.layoutParams as FrameLayout.LayoutParams).gravity =
            Gravity.CENTER_VERTICAL or if (closeOnLeft) Gravity.START else Gravity.END
        (badge.layoutParams as FrameLayout.LayoutParams).gravity =
            Gravity.CENTER_VERTICAL or if (closeOnLeft) Gravity.END else Gravity.START
        requestLayout()
    }

    private fun renderCountdown() {
        val ready = started && remainingMs == 0L
        val compact = ready && compactCloseWhenReady
        close.visibility = if (ready || showCloseButton) VISIBLE else INVISIBLE
        close.isEnabled = ready && !closed
        closeContainer.isEnabled = close.isEnabled
        closeContainer.isClickable = compact && close.isEnabled
        closeContainer.background = if (compact) null else closeBackground
        val size = resources.getDimensionPixelSize(if (compact) R.dimen.native_interstitial_close_compact_size else R.dimen.native_interstitial_close_default_size)
        val padding = resources.getDimensionPixelSize(if (compact) R.dimen.native_interstitial_close_compact_padding else R.dimen.native_interstitial_close_default_padding)
        val params = close.layoutParams as FrameLayout.LayoutParams
        if (params.width != size || params.height != size || params.gravity != Gravity.CENTER) {
            params.width = size
            params.height = size
            params.gravity = Gravity.CENTER
            close.layoutParams = params
        }
        if (close.paddingLeft != padding) close.setPadding(padding, padding, padding, padding)
        close.alpha = if (showCloseButton && !ready) 0.65f else 1f
        countdown.visibility = if (ready || hideCountdown || showCloseButton) INVISIBLE else VISIBLE
        closeProgress.visibility = if (started && showCloseButton && !hideProgress && !compact) VISIBLE else INVISIBLE
        closeProgress.progress = if (ready) 1000 else if (activeDurationMs > 0) {
            ((activeDurationMs - remainingMs).coerceIn(0, activeDurationMs) * 1000 / activeDurationMs).toInt()
        } else 0
        countdownText.text = context.getString(R.string.ads_countdown_time_format, ((remainingMs + 999) / 1000).toInt())
    }

    private fun resumeCountdown() {
        if (!started || hostPaused || timer != null || remainingMs <= 0) return
        deadlineMs = SystemClock.elapsedRealtime() + remainingMs
        timer = object : CountDownTimer(remainingMs, 100) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMs = millisUntilFinished
                renderCountdown()
            }
            override fun onFinish() {
                remainingMs = 0
                timer = null
                renderCountdown()
            }
        }.start()
    }

    private fun pauseCountdown() {
        if (timer != null) remainingMs = (deadlineMs - SystemClock.elapsedRealtime()).coerceAtLeast(0)
        timer?.cancel()
        timer = null
    }

    override fun onHostPause() { hostPaused = true; pauseCountdown() }
    override fun onHostResume() { hostPaused = false; renderCountdown(); resumeCountdown() }
    override fun onDetachedFromWindow() { pauseCountdown(); super.onDetachedFromWindow() }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); renderCountdown(); resumeCountdown() }
    override fun destroyNativeAd() {
        pauseCountdown()
        started = false
        renderCountdown()
        nativeView.destroy()
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

class NativeInterstitialV4View @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    NativeInterstitialConceptView(context, attrs, defStyleAttr, R.layout.gnt_version_v4)

class NativeInterstitialV5View @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    NativeInterstitialConceptView(context, attrs, defStyleAttr, R.layout.gnt_version_v5)

class NativeInterstitialV6View @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    NativeInterstitialConceptView(context, attrs, defStyleAttr, R.layout.gnt_version_v6)
