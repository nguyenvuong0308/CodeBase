package com.core.ads.customviews.ads

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.CountDownTimer
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.core.ads.R as AdsR
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.core.ads.databinding.GntPictureInPictureTemplateViewBinding
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.ads.domain.AdsManager
import com.core.ads.extensions.updateBackgroundColor
import com.core.ads.extensions.updateRadius
import com.core.ads.glidetransformation.RoundedCornersTransformation
import com.core.dimens.R as DimenR
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.NativeAdPlace
import com.core.utilities.dpToPx
import com.core.utilities.isValidGlideContext
import com.google.android.gms.ads.nativead.NativeAd
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.hypot

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface NativePictureInPictureEntryPoint {
    fun adsManager(): AdsManager
}

class NativePictureInPicture @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseNativeTemplateView(context, attrs, defStyleAttr) {

    data class Config(
        val sizeResId: Int = DimenR.dimen._180dp,
        val marginStartDp: Float = DEFAULT_MARGIN_DP,
        val marginTopDp: Float = DEFAULT_TOP_MARGIN_DP,
        val marginEndDp: Float = DEFAULT_MARGIN_DP,
        val marginBottomDp: Float = DEFAULT_MARGIN_DP,
        val initialGravity: Int = Gravity.TOP or Gravity.END,
        val anchorMode: AnchorMode = AnchorMode.Flexible,
        val closeCountDownSeconds: Long = DEFAULT_CLOSE_COUNTDOWN_SECONDS
    )

    enum class AnchorMode(val key: String) {
        Flexible("flexible"),
        Fixed("fixed");

        companion object {
            fun fromKey(key: String?): AnchorMode? {
                return entries.firstOrNull { mode ->
                    mode.key.equals(key, ignoreCase = true)
                }
            }
        }
    }

    private companion object {
        private const val DEFAULT_MARGIN_DP = 20f
        private const val DEFAULT_TOP_MARGIN_DP = 20f
        private const val DEFAULT_CLOSE_COUNTDOWN_SECONDS = 3L
        private const val MIN_SNAP_ANIMATION_DURATION_MS = 160L
        private const val MAX_SNAP_ANIMATION_DURATION_MS = 260L
        private const val ENTRANCE_ANIMATION_DURATION_MS = 320L
        private const val CLOSE_COUNTDOWN_INTERVAL_MILLIS = 50L
        private const val PROGRESS_MAX = 1000
    }

    private val binding: GntPictureInPictureTemplateViewBinding by lazy {
        GntPictureInPictureTemplateViewBinding.inflate(LayoutInflater.from(context), this)
    }

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var windowManager: WindowManager? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private var hostActivity: Activity? = null
    private var hostDecorView: View? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var currentConfig = Config()
    private var isAddedToWindowManager = false
    private var downRawX = 0f
    private var downRawY = 0f
    private var downParamX = 0
    private var downParamY = 0
    private var isDragging = false
    private var snapAnimator: ValueAnimator? = null
    private var closeCountDownTimer: CountDownTimer? = null
    private var isCloseEnabled = true
    private var isClosedByUser = false
    private var savedWindowPosition: Pair<Int, Int>? = null
    private var savedWindowPositionHostDecorView: WeakReference<View>? = null
    private var pendingConsumedPlaceName: IAdPlaceName? = null
    private var pendingConsumedNativeAd: NativeAd? = null
    private val adsManager: AdsManager? by lazy(LazyThreadSafetyMode.NONE) {
        runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                NativePictureInPictureEntryPoint::class.java
            ).adsManager()
        }.getOrNull()
    }

    private val hostDetachListener = object : OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) = Unit

        override fun onViewDetachedFromWindow(v: View) {
            dismiss()
        }
    }

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onPause(owner: LifecycleOwner) {
//            dismiss()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            markPendingNativeAdConsumed()
            dismiss()
        }
    }

    init {
        binding.closeButton.isClickable = false
        binding.closeButtonContainer.setOnClickListener {
            if (isCloseEnabled) {
                isClosedByUser = true
                dismiss()
                onClose?.invoke()
            }
        }
    }

    fun processAdResource(
        lifecycleOwner: LifecycleOwner,
        activity: Activity,
        adResource: AdLoadBannerNativeUiResource,
        placeName: IAdPlaceName,
        config: Config = Config()
    ): Boolean {
        if (adResource.commonAdPlaceName != placeName) return false
        if (isClosedByUser) return false

        return when (adResource) {
            is AdLoadBannerNativeUiResource.NativeAdLoaded -> {
                val anchorMode = AnchorMode.fromKey(adResource.nativeAdPlace.pipAnchorMode)
                    ?: config.anchorMode
                val closeCountDownSeconds = adResource.nativeAdPlace.countDownTimer
                    ?.coerceAtLeast(0)
                    ?.toLong()
                    ?: config.closeCountDownSeconds
                val marginDp = adResource.nativeAdPlace.pipMarginDp
                    ?.coerceAtLeast(0f)
                    ?: config.marginStartDp
                val marginTopDp = adResource.nativeAdPlace.pipTopMarginDp
                    ?.coerceAtLeast(0f)
                    ?: config.marginTopDp
                show(
                    lifecycleOwner = lifecycleOwner,
                    activity = activity,
                    nativeAd = adResource.nativeAd,
                    styles = createNativeTemplateStyle(adResource.nativeAdPlace),
                    config = config.copy(
                        anchorMode = anchorMode,
                        closeCountDownSeconds = closeCountDownSeconds,
                        marginStartDp = marginDp,
                        marginTopDp = marginTopDp,
                        marginEndDp = marginDp,
                        marginBottomDp = marginDp
                    )
                ).also { isShown ->
                    if (isShown) {
                        pendingConsumedPlaceName = placeName
                        pendingConsumedNativeAd = adResource.nativeAd
                    }
                }
            }

            else -> {
                dismiss()
                false
            }
        }
    }

    private fun markNativeAdConsumed(placeName: IAdPlaceName, nativeAd: NativeAd) {
        adsManager?.markNativeAdConsumed(placeName, nativeAd)
    }

    private fun markPendingNativeAdConsumed() {
        val placeName = pendingConsumedPlaceName
        val nativeAd = pendingConsumedNativeAd
        pendingConsumedPlaceName = null
        pendingConsumedNativeAd = null
        if (placeName != null && nativeAd != null) {
            markNativeAdConsumed(placeName, nativeAd)
        }
    }

    fun show(
        lifecycleOwner: LifecycleOwner,
        activity: Activity,
        nativeAd: NativeAd,
        styles: NativeTemplateStyle? = null,
        config: Config = Config()
    ): Boolean {
        if (isClosedByUser) return false
        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) return false
        attachLifecycle(lifecycleOwner)
        return show(activity, nativeAd, styles, config)
    }

    fun show(
        activity: Activity,
        nativeAd: NativeAd,
        styles: NativeTemplateStyle? = null,
        config: Config = Config()
    ): Boolean {
        if (isClosedByUser) return false
        setNativeAd(nativeAd)
        styles?.let(::applyStyles)
        return show(activity, config)
    }

    fun show(activity: Activity, config: Config = Config()): Boolean {
        if (isClosedByUser) return false

        if (activity.isFinishing || activity.isDestroyed) {
            return false
        }

        val decorView = activity.window.decorView
        if (decorView.windowToken == null) {
            decorView.post {
                show(activity, config)
            }
            return false
        }

        currentConfig = config
        val manager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = createLayoutParams(activity, config)

        if (isAddedToWindowManager) {
            cancelSnapAnimation()
            cancelEntranceAnimation()
            visibility = VISIBLE
            hostActivity = activity
            windowManager = manager
            windowParams = params
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            runCatching {
                manager.updateViewLayout(this, params)
            }
            startCloseCountdown(config.closeCountDownSeconds)
            return true
        }

        hostDecorView?.removeOnAttachStateChangeListener(hostDetachListener)
        hostDecorView = decorView.also {
            it.addOnAttachStateChangeListener(hostDetachListener)
        }
        hostActivity = activity
        windowManager = manager
        windowParams = params

        val added = runCatching {
            visibility = VISIBLE
            translationX = resolveEntranceHiddenTranslationX(activity, params.x)
            manager.addView(this, params)
            isAddedToWindowManager = true
            startEntranceAnimation()
            startCloseCountdown(config.closeCountDownSeconds)
            true
        }.getOrDefault(false)
        if (!added) {
            cancelEntranceAnimation()
            hostDecorView?.removeOnAttachStateChangeListener(hostDetachListener)
            hostActivity = null
            hostDecorView = null
            windowManager = null
            windowParams = null
        }
        return added
    }

    fun dismiss(destroyAd: Boolean = false) {
        val manager = windowManager
        saveCurrentWindowPosition()
        if (isAddedToWindowManager && manager != null) {
            runCatching {
                manager.removeViewImmediate(this)
            }.recoverCatching {
                manager.removeView(this)
            }
        }

        hostDecorView?.removeOnAttachStateChangeListener(hostDetachListener)
        detachLifecycle()
        hostActivity = null
        hostDecorView = null
        windowManager = null
        windowParams = null
        isAddedToWindowManager = false
        isDragging = false
        cancelEntranceAnimation()
        cancelSnapAnimation()
        cancelCloseCountdown()

        if (destroyAd) {
            destroyNativeAd()
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val params = windowParams ?: return super.dispatchTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                cancelSnapAnimation()
                downRawX = event.rawX
                downRawY = event.rawY
                downParamX = params.x
                downParamY = params.y
                isDragging = false
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                if (isDragging || abs(dx) > touchSlop || abs(dy) > touchSlop) {
                    isDragging = true
                    updateWindowPosition(
                        x = downParamX + dx.toInt(),
                        y = downParamY + dy.toInt(),
                        includeMargins = false
                    )
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    if (event.actionMasked == MotionEvent.ACTION_UP) {
                        applyAnchorMode()
                    }
                    isDragging = false
                    return true
                }
            }
        }

        return super.dispatchTouchEvent(event)
    }

    override fun setNativeAd(nativeAd: NativeAd) {
        binding.nativeAdView.callToActionView = binding.cta
        binding.nativeAdView.headlineView = binding.primary
        binding.nativeAdView.iconView = binding.icon

        binding.primary.text = nativeAd.headline.orEmpty()
        binding.cta.text = nativeAd.callToAction.orEmpty()

        val body = nativeAd.body.orEmpty()
        binding.body.text = body.ifEmpty {
            nativeAd.advertiser ?: nativeAd.store ?: nativeAd.headline.orEmpty()
        }
        binding.nativeAdView.bodyView = binding.body

        val advertiser = nativeAd.advertiser ?: nativeAd.store
        binding.advertiser.visibility = if (advertiser.isNullOrBlank()) GONE else VISIBLE
        binding.advertiser.text = advertiser.orEmpty()
        binding.nativeAdView.advertiserView = binding.advertiser

        binding.icon.visibility = GONE
        nativeAd.icon?.let {
            binding.icon.visibility = VISIBLE
            if (context.isValidGlideContext()) {
                Glide.with(this)
                    .load(it.drawable)
                    .override(resources.getDimensionPixelSize(DimenR.dimen._42dp))
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .apply(
                        RequestOptions.bitmapTransform(
                            RoundedCornersTransformation(
                                context.resources.getDimensionPixelSize(DimenR.dimen._6dp),
                                0,
                                RoundedCornersTransformation.CornerType.ALL
                            )
                        )
                    )
                    .into(binding.icon)
            }
        }

        binding.nativeAdView.setNativeAd(nativeAd)
    }

    override fun destroyNativeAd() {
        binding.nativeAdView.destroy()
    }

    override fun applyStyles(styles: NativeTemplateStyle) {
        styles.mainBackgroundColor?.let {
            binding.background.background = it
            binding.primary.background = it
        }

        styles.primaryTextTypeface?.let {
            binding.primary.typeface = it
        }

        styles.callToActionTextTypeface?.let {
            binding.cta.typeface = it
        }

        styles.primaryTextTypefaceColor?.let {
            binding.primary.setTextColor(it.toColorInt())
        }

        styles.callToActionTypefaceColor?.let {
            binding.cta.setTextColor(it)
            binding.ctaArrow.setColorFilter(it)
        }

        styles.tertiaryTextTypefaceColor?.let {
            val color = it.toColorInt()
            binding.body.setTextColor(color)
            binding.advertiser.setTextColor(color)
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

        styles.callToActionRadius?.let {
            binding.layoutCta.updateRadius(it.toFloat())
        }

        styles.borderColor?.let { color ->
            (binding.background.background as? GradientDrawable)?.setStroke(
                resources.getDimensionPixelSize(DimenR.dimen._1dp),
                color.toColorInt()
            )
        }

        styles.backgroundColor?.let {
            binding.background.updateBackgroundColor(it)
        }

        styles.backgroundResource?.let {
            binding.background.setBackgroundResource(it)
        }

//        styles.backgroundAdsNotifyView?.let {
//            binding.adNotificationView.setBackgroundResource(it)
//        }
        applyAdsNotifyViewStyles(styles, binding.adNotificationView)

        styles.primaryTextBackgroundColor?.let {
            binding.primary.background = it
        }

        styles.backgroundRadius?.let { radius ->
            (binding.background.background as? GradientDrawable)?.cornerRadius =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    radius.toFloat(),
                    resources.displayMetrics
                )
        }

        styles.callToActionBorderColor?.let { color ->
            (binding.layoutCta.background as? GradientDrawable)?.setStroke(
                resources.getDimensionPixelSize(DimenR.dimen._1dp),
                color.toColorInt()
            )
        }

        invalidate()
        requestLayout()
    }

    override fun onHostPause() {
        dismiss()
    }

    private fun attachLifecycle(owner: LifecycleOwner) {
        if (lifecycleOwner === owner) return
        detachLifecycle()
        lifecycleOwner = owner
        owner.lifecycle.addObserver(lifecycleObserver)
    }

    private fun detachLifecycle() {
        lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
        lifecycleOwner = null
    }

    private fun createNativeTemplateStyle(nativeAdPlace: NativeAdPlace): NativeTemplateStyle {
        val borderColor = nativeAdPlace.borderColor ?: "#" + Integer.toHexString(
            ContextCompat.getColor(context, AdsR.color.background_divider)
        )

        return NativeTemplateStyle.Builder()
            .withCallToActionBackgroundColor(nativeAdPlace.backgroundCta)
            .withCallToActionRadius(nativeAdPlace.ctaRadius)
            .withCallToActionTypefaceColor(nativeAdPlace.ctaTextColor)
            .withCallToActionTextSize(nativeAdPlace.ctaTextSizeSp ?: 0f)
            .withCtaBorderColor(nativeAdPlace.ctaBorderColor)
            .withBorderColor(borderColor)
            .withBackgroundColor(nativeAdPlace.backgroundColor)
            .withCountDownSecond(nativeAdPlace.countDownTimer)
            .withCloseStepCount(nativeAdPlace.closeStepCount)
            .withStep1CountDownSecond(nativeAdPlace.step1CountDownTimer)
            .withStep2CountDownSecond(nativeAdPlace.step2CountDownTimer)
            .withBackgroundFullColor(nativeAdPlace.backgroundFullColor)
            .withPrimaryTextTypefaceColor(nativeAdPlace.primaryTextColor)
            .withPrimaryTextSize(nativeAdPlace.primaryTextSizeSp ?: 0f)
            .withTertiaryTextTypefaceColor(nativeAdPlace.bodyTextColor)
            .withTertiaryTextSize(nativeAdPlace.bodyTextSizeSp ?: 0f)
            .withMainBackgroundRadius(nativeAdPlace.backgroundRadius)
            .withBackgroundAdsNotifyView(AdsR.drawable.gnt_rounded_corners_shape)
            .withBackgroundColorAdsNotifyView(nativeAdPlace.backgroundColorAdsNotifyView)
            .withTextColorAdsNotifyView(nativeAdPlace.textColorAdsNotifyView)
            .withMediaBackgroundColor(nativeAdPlace.mediaBackgroundColor)
            .withIsEnableImmersive(nativeAdPlace.isEnableFullScreenImmersive)
            .withHideTextCountDown(nativeAdPlace.hideTextCountDown)
            .withHideTextSkipCountDown(nativeAdPlace.hideTextSkipCountDown)
            .withHideProgressCountDown(nativeAdPlace.hideProgressCountDown)
            .withProgressBarTint(nativeAdPlace.progressBarTint)
            .withControlClosePosition(nativeAdPlace.controlClosePosition)
            .withCollapsibleExpandCooldownSecond(nativeAdPlace.collapsibleExpandCooldownSecond)
            .withAdPlaceName(nativeAdPlace.placeName.name)
            .build()
    }

    private fun createLayoutParams(
        activity: Activity,
        config: Config
    ): WindowManager.LayoutParams {
        val sizePx = resources.getDimensionPixelSize(config.sizeResId)
        measure(
            MeasureSpec.makeMeasureSpec(sizePx, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(sizePx, MeasureSpec.EXACTLY)
        )

        val bounds = resolveHostBounds(activity)
        val marginStartPx = context.dpToPx(config.marginStartDp)
        val marginTopPx = context.dpToPx(config.marginTopDp)
        val marginEndPx = context.dpToPx(config.marginEndDp)
        val marginBottomPx = context.dpToPx(config.marginBottomDp)
        val initialX = if (config.initialGravity and Gravity.END == Gravity.END) {
            bounds.first - sizePx - marginEndPx
        } else {
            marginStartPx
        }
        val initialY = if (config.initialGravity and Gravity.BOTTOM == Gravity.BOTTOM) {
            bounds.second - sizePx - marginBottomPx
        } else {
            marginTopPx
        }

        return WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            token = activity.window.decorView.windowToken
            val savedPosition = resolveSavedWindowPosition(activity)
            x = clampX(activity, savedPosition?.first ?: initialX)
            y = clampY(activity, savedPosition?.second ?: initialY)
        }
    }

    private fun saveCurrentWindowPosition() {
        val params = windowParams ?: return
        val decorView = hostActivity?.window?.decorView ?: hostDecorView ?: return
        savedWindowPosition = params.x to params.y
        savedWindowPositionHostDecorView = WeakReference(decorView)
    }

    private fun resolveSavedWindowPosition(activity: Activity): Pair<Int, Int>? {
        val savedPosition = savedWindowPosition ?: return null
        val savedDecorView = savedWindowPositionHostDecorView?.get()
        if (savedDecorView !== activity.window.decorView) {
            savedWindowPosition = null
            savedWindowPositionHostDecorView = null
            return null
        }
        return savedPosition
    }

    private fun updateWindowPosition(
        x: Int,
        y: Int,
        clampToBounds: Boolean = true,
        includeMargins: Boolean = true
    ) {
        val manager = windowManager ?: return
        val params = windowParams ?: return
        val activity = hostActivity

        params.x = if (activity != null && clampToBounds) {
            clampX(activity, x, includeMargins)
        } else {
            x
        }
        params.y = if (activity != null && clampToBounds) {
            clampY(activity, y, includeMargins)
        } else {
            y
        }
        saveCurrentWindowPosition()
        runCatching {
            manager.updateViewLayout(this, params)
        }
    }

    private fun snapToNearestHorizontalEdge() {
        val activity = hostActivity ?: return
        val params = windowParams ?: return
        val bounds = resolveHostBounds(activity)
        val marginStartPx = context.dpToPx(currentConfig.marginStartDp)
        val marginEndPx = context.dpToPx(currentConfig.marginEndDp)
        val viewWidth = measuredWidth.takeIf { it > 0 } ?: width
        val centerX = params.x + viewWidth / 2

        val targetX = if (centerX < bounds.first / 2) {
            marginStartPx
        } else {
            bounds.first - viewWidth - marginEndPx
        }
        updateWindowPosition(targetX, params.y)
    }

    private fun applyAnchorMode() {
        when (currentConfig.anchorMode) {
            AnchorMode.Flexible -> snapToNearestHorizontalEdge()
            AnchorMode.Fixed -> snapToNearestCorner()
        }
    }

    private fun snapToNearestCorner() {
        val activity = hostActivity ?: return
        val params = windowParams ?: return
        val bounds = resolveHostBounds(activity)
        val marginStartPx = context.dpToPx(currentConfig.marginStartDp)
        val marginTopPx = context.dpToPx(currentConfig.marginTopDp)
        val marginEndPx = context.dpToPx(currentConfig.marginEndDp)
        val marginBottomPx = context.dpToPx(currentConfig.marginBottomDp)
        val viewWidth = measuredWidth.takeIf { it > 0 } ?: width
        val viewHeight = measuredHeight.takeIf { it > 0 } ?: height
        val centerX = params.x + viewWidth / 2
        val centerY = params.y + viewHeight / 2

        val targetX = if (centerX < bounds.first / 2) {
            marginStartPx
        } else {
            bounds.first - viewWidth - marginEndPx
        }
        val targetY = if (centerY < bounds.second / 2) {
            marginTopPx
        } else {
            bounds.second - viewHeight - marginBottomPx
        }
        animateWindowPosition(targetX, targetY)
    }

    private fun animateWindowPosition(
        targetX: Int,
        targetY: Int,
        duration: Long? = null,
        clampToBounds: Boolean = true
    ) {
        val params = windowParams ?: return
        val startX = params.x
        val startY = params.y
        if (startX == targetX && startY == targetY) return

        cancelSnapAnimation()

        val distance = hypot(
            (targetX - startX).toFloat(),
            (targetY - startY).toFloat()
        )
        val actualDuration = duration ?: distance
            .coerceIn(
                MIN_SNAP_ANIMATION_DURATION_MS.toFloat(),
                MAX_SNAP_ANIMATION_DURATION_MS.toFloat()
            )
            .toLong()

        snapAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = actualDuration
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                val nextX = startX + ((targetX - startX) * fraction).toInt()
                val nextY = startY + ((targetY - startY) * fraction).toInt()
                updateWindowPosition(nextX, nextY, clampToBounds)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (snapAnimator == animation) {
                        snapAnimator = null
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                    if (snapAnimator == animation) {
                        snapAnimator = null
                    }
                }
            })
            start()
        }
    }

    private fun cancelSnapAnimation() {
        snapAnimator?.cancel()
        snapAnimator = null
    }

    private fun startEntranceAnimation() {
        animate()
            .translationX(0f)
            .setDuration(ENTRANCE_ANIMATION_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun cancelEntranceAnimation() {
        animate().cancel()
        translationX = 0f
    }

    private fun resolveEntranceHiddenTranslationX(activity: Activity, targetX: Int): Float {
        val bounds = resolveHostBounds(activity)
        val viewWidth = measuredWidth.takeIf { it > 0 }
            ?: width.takeIf { it > 0 }
            ?: resources.getDimensionPixelSize(currentConfig.sizeResId)
        val targetCenterX = targetX + viewWidth / 2
        return if (targetCenterX < bounds.first / 2) {
            -(targetX + viewWidth).toFloat()
        } else {
            (bounds.first - targetX).toFloat()
        }
    }

    private fun startCloseCountdown(countDownSeconds: Long) {
        cancelCloseCountdown()
        if (countDownSeconds <= 0L) {
            updateCloseButtonState(isEnabled = true, progress = PROGRESS_MAX)
            return
        }

        updateCloseButtonState(isEnabled = false, progress = 0)
        val totalMillis = countDownSeconds * 1000L
        closeCountDownTimer = object : CountDownTimer(
            totalMillis,
            CLOSE_COUNTDOWN_INTERVAL_MILLIS
        ) {
            override fun onTick(millisUntilFinished: Long) {
                updateCloseButtonState(
                    isEnabled = false,
                    progress = calculateCloseProgress(totalMillis, millisUntilFinished)
                )
            }

            override fun onFinish() {
                updateCloseButtonState(isEnabled = true, progress = PROGRESS_MAX)
                closeCountDownTimer = null
            }
        }.also { timer ->
            timer.start()
        }
    }

    private fun cancelCloseCountdown() {
        closeCountDownTimer?.cancel()
        closeCountDownTimer = null
    }

    private fun updateCloseButtonState(
        isEnabled: Boolean,
        progress: Int
    ) {
        isCloseEnabled = isEnabled
        binding.closeButton.isEnabled = isEnabled
        binding.closeButton.alpha = if (isEnabled) 1f else 0.65f
        binding.actionProgress.visibility = if (isEnabled) GONE else VISIBLE
        binding.actionProgress.progress = progress.coerceIn(0, PROGRESS_MAX)
    }

    private fun calculateCloseProgress(totalMillis: Long, millisUntilFinished: Long): Int {
        val elapsedMillis = (totalMillis - millisUntilFinished.coerceAtLeast(0L)).coerceAtLeast(0L)
        return ((elapsedMillis * PROGRESS_MAX) / totalMillis.coerceAtLeast(1L))
            .toInt()
            .coerceIn(0, PROGRESS_MAX)
    }

    private fun clampX(activity: Activity, x: Int, includeMargins: Boolean = true): Int {
        val bounds = resolveHostBounds(activity)
        val marginStartPx = if (includeMargins) context.dpToPx(currentConfig.marginStartDp) else 0
        val marginEndPx = if (includeMargins) context.dpToPx(currentConfig.marginEndDp) else 0
        val viewWidth = measuredWidth.takeIf { it > 0 } ?: width
        val maxX = bounds.first - viewWidth - marginEndPx
        return x.coerceIn(marginStartPx, maxX.coerceAtLeast(marginStartPx))
    }

    private fun clampY(activity: Activity, y: Int, includeMargins: Boolean = true): Int {
        val bounds = resolveHostBounds(activity)
        val marginTopPx = if (includeMargins) context.dpToPx(currentConfig.marginTopDp) else 0
        val marginBottomPx = if (includeMargins) context.dpToPx(currentConfig.marginBottomDp) else 0
        val viewHeight = measuredHeight.takeIf { it > 0 } ?: height
        val maxY = bounds.second - viewHeight - marginBottomPx
        return y.coerceIn(marginTopPx, maxY.coerceAtLeast(marginTopPx))
    }

    private fun resolveHostBounds(activity: Activity): Pair<Int, Int> {
        val decorView = activity.window.decorView
        val width = decorView.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val height = decorView.height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
        return width to height
    }
}
