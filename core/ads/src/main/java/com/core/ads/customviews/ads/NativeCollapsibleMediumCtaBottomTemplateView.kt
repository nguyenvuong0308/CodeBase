package com.core.ads.customviews.ads

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.core.ads.databinding.GntMediumCollapsibleCtaBottomTemplateViewBinding
import com.core.ads.extensions.updateBackgroundColor
import com.core.ads.extensions.updateRadius
import com.core.ads.glidetransformation.RoundedCornersTransformation
import com.core.dimens.R
import com.core.utilities.isValidGlideContext
import com.google.android.gms.ads.nativead.NativeAd
import java.util.Collections
import java.util.WeakHashMap

class NativeCollapsibleMediumCtaBottomTemplateView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BaseNativeTemplateView(context, attrs, defStyleAttr) {

    private companion object {
        val lastExpandedCloseTimes = mutableMapOf<String, Long>()
        val expandedShownNativeAds: MutableSet<NativeAd> = Collections.newSetFromMap(WeakHashMap<NativeAd, Boolean>())
    }

    private enum class ControlClosePosition {
        LEFT,
        RIGHT
    }

    private val binding: GntMediumCollapsibleCtaBottomTemplateViewBinding by lazy {
        GntMediumCollapsibleCtaBottomTemplateViewBinding.inflate(LayoutInflater.from(context), this, true)
    }

    private var popupWindow: PopupWindow? = null
    private var popupBinding: GntMediumCollapsibleCtaBottomTemplateViewBinding? = null
    private var expandedPopupNativeAd: NativeAd? = null
    private var currentNativeAd: NativeAd? = null
    private var currentStyles: NativeTemplateStyle? = null
    private var controlClosePosition: ControlClosePosition = ControlClosePosition.RIGHT
    private var collapsibleExpandCooldownSecond: Int = 0
    private var collapsibleExpandCooldownKey: String = javaClass.name
    private var popupRequestVersion = 0

    init {
        initView()
    }

    private fun initView() {
        binding.icCloseCollapse.setOnClickListener {
            collapseToMini()
        }
    }

    override fun setNativeAd(nativeAd: NativeAd) {
        val isSameShowingExpandedNativeAd = popupWindow?.isShowing == true && expandedPopupNativeAd === nativeAd
        currentNativeAd = nativeAd
        prepareCollapsedContent(binding, nativeAd)
        if (isSameShowingExpandedNativeAd) {
            showExpandedPopup(nativeAd)
        } else if (isExpandCooldownActive() || hasNativeAdShownExpanded(nativeAd)) {
            showCollapsedInline()
        } else {
            showExpandedPopup(nativeAd)
        }
    }

    private fun prepareExpandedContent(
        targetBinding: GntMediumCollapsibleCtaBottomTemplateViewBinding,
        nativeAd: NativeAd
    ) {
        targetBinding.nativeAdView.visibility = VISIBLE
        targetBinding.background.visibility = VISIBLE
        targetBinding.backgroundMini.visibility = GONE
        targetBinding.mediaView.visibility = VISIBLE
        targetBinding.icCloseCollapse.visibility = VISIBLE

        targetBinding.nativeAdView.callToActionView = targetBinding.cta
        targetBinding.nativeAdView.headlineView = targetBinding.primary
        targetBinding.nativeAdView.mediaView = targetBinding.mediaView
        targetBinding.nativeAdView.bodyView = targetBinding.body

        targetBinding.primary.text = nativeAd.headline.orEmpty()
        targetBinding.cta.text = nativeAd.callToAction.orEmpty()
        targetBinding.body.text = nativeAd.body.orEmpty()

        targetBinding.icon.visibility = GONE
        nativeAd.icon?.let {
            targetBinding.icon.visibility = VISIBLE
            if (context.isValidGlideContext()) {
                loadIcon(targetBinding.icon, it.drawable)
            }
        }

        targetBinding.nativeAdView.setNativeAd(nativeAd)
    }

    private fun prepareCollapsedContent(
        targetBinding: GntMediumCollapsibleCtaBottomTemplateViewBinding,
        nativeAd: NativeAd
    ) {
        targetBinding.primaryMini.text = nativeAd.headline.orEmpty()
        targetBinding.ctaMini.text = nativeAd.callToAction.orEmpty()
        targetBinding.bodyMini.text = nativeAd.body.orEmpty()

        targetBinding.iconMini.visibility = GONE
        nativeAd.icon?.let {
            targetBinding.iconMini.visibility = VISIBLE
            if (context.isValidGlideContext()) {
                loadIcon(targetBinding.iconMini, it.drawable)
            }
        }
    }

    private fun showExpandedPopup(nativeAd: NativeAd) {
        val requestVersion = ++popupRequestVersion
        hideInlineViewWhileExpanded()

        post {
            if (requestVersion != popupRequestVersion) return@post

            if (!isAttachedToWindow || windowToken == null) {
                showCollapsedInline()
                return@post
            }

            val showingPopupBinding = popupBinding
            if (popupWindow?.isShowing == true && showingPopupBinding != null) {
                prepareExpandedContent(showingPopupBinding, nativeAd)
                currentStyles?.let { styles ->
                    applyStylesToBinding(showingPopupBinding, styles)
                    applyAdsNotifyViewStyles(
                        styles,
                        binding.adNotificationView,
                        binding.adNotificationViewMini,
                        showingPopupBinding.adNotificationView,
                        showingPopupBinding.adNotificationViewMini
                    )
                }
                applyControlClosePosition(showingPopupBinding)
                return@post
            }

            dismissExpandedPopup()
            val expandedBinding = GntMediumCollapsibleCtaBottomTemplateViewBinding.inflate(
                LayoutInflater.from(context)
            )
            popupBinding = expandedBinding

            expandedBinding.icCloseCollapse.setOnClickListener {
                collapseToMini()
            }
            prepareExpandedContent(expandedBinding, nativeAd)
            currentStyles?.let { styles ->
                applyStylesToBinding(expandedBinding, styles)
                applyAdsNotifyViewStyles(
                    styles,
                    binding.adNotificationView,
                    binding.adNotificationViewMini,
                    expandedBinding.adNotificationView,
                    expandedBinding.adNotificationViewMini
                )
            }
            applyControlClosePosition(expandedBinding)

            val popup = PopupWindow(
                expandedBinding.root,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                false
            ).apply {
                isOutsideTouchable = false
                isClippingEnabled = true
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                runCatching {
                    elevation = resources.getDimensionPixelSize(R.dimen._8dp).toFloat()
                }
            }

            popupWindow = popup
            popup.setOnDismissListener {
                if (popupWindow === popup) {
                    popupWindow = null
                    popupBinding = null
                    expandedPopupNativeAd = null
                }
            }
            runCatching {
                popup.showAsDropDown(this, 0, -resolvePopupHeight(expandedBinding.root))
                expandedPopupNativeAd = nativeAd
                markNativeAdExpandedShown(nativeAd)
            }.onFailure {
                if (popupWindow === popup) {
                    dismissExpandedPopup()
                }
                showCollapsedInline()
            }
        }
    }

    private fun hideInlineViewWhileExpanded() {
        binding.nativeAdView.visibility = INVISIBLE
        binding.background.visibility = GONE
        binding.backgroundMini.visibility = VISIBLE
        binding.icCloseCollapse.visibility = GONE
    }

    private fun resolvePopupHeight(view: View): Int {
        if (view.minimumHeight > 0) return view.minimumHeight

        val measuredWidth = width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val widthSpec = View.MeasureSpec.makeMeasureSpec(measuredWidth, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(widthSpec, heightSpec)
        return view.measuredHeight
    }

    private fun collapseToMini() {
        if (isCollapsedInlineVisible()) return
        showCollapsedInline(markExpandedClosed = true)
    }

    private fun showCollapsedInline(markExpandedClosed: Boolean = false) {
        if (markExpandedClosed) {
            markExpandedClosed()
        }
        popupRequestVersion++
        dismissExpandedPopup()

        binding.nativeAdView.visibility = VISIBLE
        binding.mediaView.visibility = GONE
        binding.background.visibility = GONE
        binding.backgroundMini.visibility = VISIBLE
        binding.icCloseCollapse.visibility = GONE

        binding.nativeAdView.callToActionView = binding.ctaMini
        binding.nativeAdView.headlineView = binding.primaryMini
        binding.nativeAdView.bodyView = binding.bodyMini
        currentNativeAd?.let { nativeAd ->
            prepareCollapsedContent(binding, nativeAd)
            binding.nativeAdView.setNativeAd(nativeAd)
        }
    }

    private fun isCollapsedInlineVisible(): Boolean {
        return binding.nativeAdView.visibility == VISIBLE &&
            binding.backgroundMini.visibility == VISIBLE &&
            popupWindow?.isShowing != true
    }

    private fun dismissExpandedPopup() {
        popupRequestVersion++
        popupWindow?.dismiss()
        popupWindow = null
        popupBinding = null
        expandedPopupNativeAd = null
    }

    private fun loadIcon(imageView: ImageView, drawable: Any?) {
        Glide.with(this)
            .load(drawable)
            .override(resources.getDimensionPixelSize(R.dimen._44dp))
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .apply(
                RequestOptions.bitmapTransform(
                    RoundedCornersTransformation(
                        context.resources.getDimensionPixelSize(
                            R.dimen._8dp
                        ), 0, RoundedCornersTransformation.CornerType.ALL
                    )
                )
            )
            .into(imageView)
    }

    override fun destroyNativeAd() {
        dismissExpandedPopup()
        binding.nativeAdView.destroy()
    }

    override fun onDetachedFromWindow() {
        dismissExpandedPopup()
        super.onDetachedFromWindow()
    }

    override fun applyStyles(styles: NativeTemplateStyle) {
        currentStyles = styles
        collapsibleExpandCooldownSecond = styles.collapsibleExpandCooldownSecond?.coerceAtLeast(0) ?: 0
        collapsibleExpandCooldownKey = styles.adPlaceName?.takeIf { it.isNotBlank() } ?: javaClass.name
        applyStylesToBinding(binding, styles)
        popupBinding?.let { applyStylesToBinding(it, styles) }

        styles.backgroundAdsNotifyView?.let {
            binding.adNotificationView.setBackgroundResource(it)
            binding.adNotificationViewMini.setBackgroundResource(it)
            popupBinding?.adNotificationView?.setBackgroundResource(it)
            popupBinding?.adNotificationViewMini?.setBackgroundResource(it)
        }

        popupBinding?.let { expandedBinding ->
            applyAdsNotifyViewStyles(
                styles,
                binding.adNotificationView,
                binding.adNotificationViewMini,
                expandedBinding.adNotificationView,
                expandedBinding.adNotificationViewMini
            )
        } ?: applyAdsNotifyViewStyles(styles, binding.adNotificationView, binding.adNotificationViewMini)

        controlClosePosition = resolveControlClosePosition(styles.controlClosePosition)
        applyControlClosePosition(binding)
        popupBinding?.let { applyControlClosePosition(it) }

        invalidate()
        requestLayout()
    }

    private fun applyStylesToBinding(
        targetBinding: GntMediumCollapsibleCtaBottomTemplateViewBinding,
        styles: NativeTemplateStyle
    ) {
        styles.mainBackgroundColor?.let {
            targetBinding.background.background = it
            targetBinding.backgroundMini.background = it
            targetBinding.primary.background = it
            targetBinding.primaryMini.background = it
            targetBinding.body.background = it
            targetBinding.bodyMini.background = it
        }

        styles.primaryTextTypeface?.let {
            targetBinding.primary.typeface = it
            targetBinding.primaryMini.typeface = it
        }

        styles.tertiaryTextTypeface?.let {
            targetBinding.body.typeface = it
            targetBinding.bodyMini.typeface = it
        }

        styles.callToActionTextTypeface?.let {
            targetBinding.cta.typeface = it
            targetBinding.ctaMini.typeface = it
        }

        styles.primaryTextTypefaceColor?.let {
            targetBinding.primary.setTextColor(it.toColorInt())
            targetBinding.primaryMini.setTextColor(it.toColorInt())
        }

        styles.tertiaryTextTypefaceColor?.let {
            targetBinding.body.setTextColor(it.toColorInt())
            targetBinding.bodyMini.setTextColor(it.toColorInt())
        }

        styles.callToActionTypefaceColor?.let {
            targetBinding.cta.setTextColor(it)
            targetBinding.ctaMini.setTextColor(it)
        }

        val ctaTextSize = styles.callToActionTextSize
        if (ctaTextSize > 0) {
            targetBinding.cta.textSize = ctaTextSize
            targetBinding.ctaMini.textSize = ctaTextSize
        }

        val primaryTextSize = styles.primaryTextSize
        if (primaryTextSize > 0) {
            targetBinding.primary.textSize = primaryTextSize
            targetBinding.primaryMini.textSize = primaryTextSize
        }

        val tertiaryTextSize = styles.tertiaryTextSize
        if (tertiaryTextSize > 0) {
            targetBinding.body.textSize = tertiaryTextSize
            targetBinding.bodyMini.textSize = tertiaryTextSize
        }

        styles.callToActionBackgroundColor?.let {
            targetBinding.layoutCta.updateBackgroundColor(it)
            targetBinding.layoutCtaMini.updateBackgroundColor(it)
        }

        styles.callToActionRadius?.let {
            targetBinding.layoutCta.updateRadius(it.toFloat())
            targetBinding.layoutCtaMini.updateRadius(it.toFloat())
        }

        styles.borderColor?.let {
            (targetBinding.nativeAdView.background as? GradientDrawable)?.setStroke(
                resources.getDimensionPixelSize(R.dimen._1dp),
                it.toColorInt()
            )
        }

        styles.backgroundColor?.let {
            (targetBinding.nativeAdView.background as? GradientDrawable)?.setColor(it.toColorInt())
        }

        styles.backgroundResource?.let {
            targetBinding.background.setBackgroundResource(it)
            targetBinding.backgroundMini.setBackgroundResource(it)
        }

        styles.primaryTextBackgroundColor?.let {
            targetBinding.primary.background = it
            targetBinding.primaryMini.background = it
        }

        styles.tertiaryTextBackgroundColor?.let {
            targetBinding.body.background = it
            targetBinding.bodyMini.background = it
        }

        styles.backgroundRadius?.let { radius ->
            val bg = targetBinding.nativeAdView.background
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
            (targetBinding.layoutCta.background as? GradientDrawable)?.setStroke(
                resources.getDimensionPixelSize(R.dimen._1dp),
                it.toColorInt()
            )
            (targetBinding.layoutCtaMini.background as? GradientDrawable)?.setStroke(
                resources.getDimensionPixelSize(R.dimen._1dp),
                it.toColorInt()
            )
        }
    }

    private fun resolveControlClosePosition(position: String?): ControlClosePosition {
        return when (position?.trim()?.lowercase()) {
            "left", "start" -> ControlClosePosition.LEFT
            "right", "end" -> ControlClosePosition.RIGHT
            else -> ControlClosePosition.RIGHT
        }
    }

    private fun applyControlClosePosition(targetBinding: GntMediumCollapsibleCtaBottomTemplateViewBinding) {
        val params = targetBinding.icCloseCollapse.layoutParams as? FrameLayout.LayoutParams ?: return
        params.gravity = when (controlClosePosition) {
            ControlClosePosition.LEFT -> Gravity.START
            ControlClosePosition.RIGHT -> Gravity.END
        }
        targetBinding.icCloseCollapse.layoutParams = params
    }

    private fun isExpandCooldownActive(): Boolean {
        val cooldownMillis = collapsibleExpandCooldownSecond
            .takeIf { it > 0 }
            ?.times(1_000L)
            ?: return false
        val lastCloseTime = lastExpandedCloseTimes[collapsibleExpandCooldownKey] ?: return false
        return SystemClock.elapsedRealtime() - lastCloseTime < cooldownMillis
    }

    private fun markExpandedClosed() {
        if (collapsibleExpandCooldownSecond <= 0) return
        lastExpandedCloseTimes[collapsibleExpandCooldownKey] = SystemClock.elapsedRealtime()
    }

    private fun hasNativeAdShownExpanded(nativeAd: NativeAd): Boolean {
        return expandedShownNativeAds.contains(nativeAd)
    }

    private fun markNativeAdExpandedShown(nativeAd: NativeAd) {
        expandedShownNativeAds.add(nativeAd)
    }
}
