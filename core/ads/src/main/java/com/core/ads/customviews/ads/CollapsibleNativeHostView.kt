package com.core.ads.customviews.ads

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.appcompat.widget.AppCompatImageView
import com.core.ads.R
import com.core.config.domain.data.NativeExpandTemplate
import com.google.android.gms.ads.nativead.NativeAd
import java.util.Collections
import java.util.WeakHashMap

/**
 * Adds the shared expanded popup behavior to any regular native template.
 *
 * The wrapped template remains responsible only for rendering the inline state. This host owns
 * popup lifecycle and binds the ad to the inline template only after the popup is dismissed, so a
 * [NativeAd] is never registered with two NativeAdViews at the same time.
 */
internal class CollapsibleNativeHostView(
    context: Context,
    private val inlineTemplateView: BaseNativeTemplateView,
) : BaseNativeTemplateView(context) {

    private companion object {
        val lastExpandedCloseTimes = mutableMapOf<String, Long>()
        val expandedShownNativeAds: MutableSet<NativeAd> =
            Collections.newSetFromMap(WeakHashMap<NativeAd, Boolean>())
    }

    private enum class ControlClosePosition {
        LEFT,
        RIGHT,
    }

    private var popupWindow: PopupWindow? = null
    private var popupTemplateView: BaseNativeTemplateView? = null
    private var popupCloseView: AppCompatImageView? = null
    private var expandedPopupNativeAd: NativeAd? = null
    private var currentNativeAd: NativeAd? = null
    private var currentStyles: NativeTemplateStyle? = null
    private var controlClosePosition = ControlClosePosition.RIGHT
    private var collapsibleExpandCooldownSecond = 0
    private var collapsibleExpandCooldownKey = javaClass.name
    private var popupRequestVersion = 0
    private val expandState = CollapsibleExpandState()

    init {
        (inlineTemplateView.parent as? ViewGroup)?.removeView(inlineTemplateView)
        addView(
            inlineTemplateView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        )
        inlineTemplateView.onClose = { onClose?.invoke() }
    }

    override fun setNativeAd(nativeAd: NativeAd) {
        val isSameShowingExpandedNativeAd =
            popupWindow?.isShowing == true && expandedPopupNativeAd === nativeAd
        currentNativeAd = nativeAd
        val shouldExpand = expandState.shouldExpand(
            isSameShowingExpandedNativeAd = isSameShowingExpandedNativeAd,
            isExpandCooldownActive = isExpandCooldownActive(),
            hasNativeAdShownExpanded = hasNativeAdShownExpanded(nativeAd),
        )
        if (shouldExpand) {
            if (!isSameShowingExpandedNativeAd) {
                showExpandedPopup(nativeAd)
            }
        } else {
            if (!isCollapsedInlineVisible()) {
                showCollapsedInline()
            }
        }
    }

    /**
     * Explicitly switches this host between its expanded popup and collapsed inline state.
     *
     * The requested state is retained for subsequently assigned native ads. Explicit expansion
     * bypasses the automatic cooldown because it is initiated by the host application.
     */
    fun setExpanded(expanded: Boolean) {
        expandState.setExpanded(expanded)
        if (expanded) {
            val nativeAd = currentNativeAd ?: return
            val isAlreadyExpanded =
                popupWindow?.isShowing == true && expandedPopupNativeAd === nativeAd
            if (!isAlreadyExpanded) {
                showExpandedPopup(nativeAd)
            }
        } else if (!isCollapsedInlineVisible()) {
            showCollapsedInline()
        }
    }

    override fun applyStyles(styles: NativeTemplateStyle) {
        currentStyles = styles
        collapsibleExpandCooldownSecond =
            styles.collapsibleExpandCooldownSecond?.coerceAtLeast(0) ?: 0
        collapsibleExpandCooldownKey =
            styles.adPlaceName?.takeIf { it.isNotBlank() } ?: javaClass.name
        controlClosePosition = resolveControlClosePosition(styles.controlClosePosition)

        inlineTemplateView.applyStyles(styles)
        popupTemplateView?.applyStyles(styles)
        applyControlClosePosition()
        invalidate()
        requestLayout()
    }

    override fun destroyNativeAd() {
        dismissExpandedPopup()
//        inlineTemplateView.destroyNativeAd()
        currentNativeAd = null
    }

    override fun onHostPause() {
        inlineTemplateView.onHostPause()
    }

    override fun onHostResume() {
        inlineTemplateView.onHostResume()
    }

    override fun onDetachedFromWindow() {
        dismissExpandedPopup()
        super.onDetachedFromWindow()
    }

    private fun showExpandedPopup(nativeAd: NativeAd) {
        val requestVersion = ++popupRequestVersion
        inlineTemplateView.visibility = INVISIBLE

        post {
            if (requestVersion != popupRequestVersion) return@post
            if (!isAttachedToWindow || windowToken == null) {
                showCollapsedInline()
                return@post
            }

            val showingTemplate = popupTemplateView
            if (popupWindow?.isShowing == true && showingTemplate != null) {
                showingTemplate.setNativeAd(nativeAd)
                currentStyles?.let(showingTemplate::applyStyles)
                applyControlClosePosition()
                return@post
            }

            dismissExpandedPopup()

            val expandedTemplate = createExpandedTemplate().apply {
                onClose = { this@CollapsibleNativeHostView.onClose?.invoke() }
                currentStyles?.let(::applyStyles)
                setNativeAd(nativeAd)
            }
            val popupContent = createPopupContent(expandedTemplate)
            val popup = PopupWindow(
                popupContent,
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
                false,
            ).apply {
                isOutsideTouchable = false
                isClippingEnabled = true
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                runCatching {
                    elevation = resources.getDimensionPixelSize(com.core.dimens.R.dimen._8dp).toFloat()
                }
            }

            popupTemplateView = expandedTemplate
            popupWindow = popup
            popup.setOnDismissListener {
                if (popupWindow === popup) {
                    popupWindow = null
                    popupTemplateView = null
                    popupCloseView = null
                    expandedPopupNativeAd = null
//                    expandedTemplate.destroyNativeAd()
                }
            }

            runCatching {
                popup.showAsDropDown(this, 0, -resolvePopupHeight(popupContent))
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

    private fun createExpandedTemplate(): BaseNativeTemplateView {
        return when (currentStyles?.nativeExpandTemplate ?: NativeExpandTemplate.V1) {
            NativeExpandTemplate.V1 -> NativeExpandView(context)
            NativeExpandTemplate.V2 -> NativeExpandViewV2(context)
        }
    }

    private fun createPopupContent(expandedTemplate: BaseNativeTemplateView): FrameLayout {
        return FrameLayout(context).apply {
            addView(
                expandedTemplate,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
            )
            val closeView = AppCompatImageView(context).apply {
                setBackgroundResource(R.drawable.bg_close_collapsible)
                setImageResource(R.drawable.ic_arrow_down_24px)
                imageTintList = ColorStateList.valueOf(Color.BLACK)
                elevation = resources.getDimensionPixelSize(com.core.dimens.R.dimen._5dp).toFloat()
                setOnClickListener { collapseToInline() }
            }
            popupCloseView = closeView
            addView(
                closeView,
                LayoutParams(
                    resources.getDimensionPixelSize(com.core.dimens.R.dimen._24dp),
                    resources.getDimensionPixelSize(com.core.dimens.R.dimen._24dp),
                    resolveCloseGravity(controlClosePosition),
                ).apply {
                    val margin = resources.getDimensionPixelSize(com.core.dimens.R.dimen._10dp)
                    setMargins(margin, margin, margin, margin)
                },
            )
        }
    }

    private fun collapseToInline() {
        if (isCollapsedInlineVisible()) return
        showCollapsedInline(markExpandedClosed = true)
    }

    private fun showCollapsedInline(markExpandedClosed: Boolean = false) {
        if (markExpandedClosed) {
            markExpandedClosed()
        }
        popupRequestVersion++
        dismissExpandedPopup()
        inlineTemplateView.visibility = VISIBLE
        currentNativeAd?.let(inlineTemplateView::setNativeAd)
    }

    private fun isCollapsedInlineVisible(): Boolean {
        return inlineTemplateView.visibility == VISIBLE && popupWindow?.isShowing != true
    }

    private fun dismissExpandedPopup() {
        popupRequestVersion++
        val popup = popupWindow
        val expandedTemplate = popupTemplateView
        popupWindow = null
        popupTemplateView = null
        popupCloseView = null
        expandedPopupNativeAd = null
        popup?.setOnDismissListener(null)
        popup?.dismiss()
//        expandedTemplate?.destroyNativeAd()
    }

    private fun resolvePopupHeight(view: View): Int {
        if (view.minimumHeight > 0) return view.minimumHeight

        val measuredWidth = width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val widthSpec = View.MeasureSpec.makeMeasureSpec(measuredWidth, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(widthSpec, heightSpec)
        return view.measuredHeight
    }

    private fun resolveControlClosePosition(position: String?): ControlClosePosition {
        return when (position?.trim()?.lowercase()) {
            "left", "start" -> ControlClosePosition.LEFT
            "right", "end" -> ControlClosePosition.RIGHT
            else -> ControlClosePosition.RIGHT
        }
    }

    private fun resolveCloseGravity(position: ControlClosePosition): Int {
        val horizontalGravity = when (position) {
            ControlClosePosition.LEFT -> Gravity.START
            ControlClosePosition.RIGHT -> Gravity.END
        }
        return horizontalGravity or Gravity.TOP
    }

    private fun applyControlClosePosition() {
        val closeView = popupCloseView ?: return
        val params = closeView.layoutParams as? LayoutParams ?: return
        params.gravity = resolveCloseGravity(controlClosePosition)
        closeView.layoutParams = params
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
//        expandedShownNativeAds.add(nativeAd)
    }
}

internal class CollapsibleExpandState {
    private var manuallyExpanded: Boolean? = null

    fun setExpanded(expanded: Boolean) {
        manuallyExpanded = expanded
    }

    fun shouldExpand(
        isSameShowingExpandedNativeAd: Boolean,
        isExpandCooldownActive: Boolean,
        hasNativeAdShownExpanded: Boolean,
    ): Boolean {
        manuallyExpanded?.let { return it }
        return isSameShowingExpandedNativeAd ||
            (!isExpandCooldownActive && !hasNativeAdShownExpanded)
    }
}
