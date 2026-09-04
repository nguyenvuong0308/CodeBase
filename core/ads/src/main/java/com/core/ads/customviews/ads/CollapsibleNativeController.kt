package com.core.ads.customviews.ads

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
 * Adds collapsible popup behavior to the inline native template owned by a container.
 *
 * This controller follows the lifecycle of its anchor view. It never owns or destroys the
 * [NativeAd]; it only transfers rendering between the inline and expanded templates.
 */
internal class CollapsibleNativeController(
    private val anchorView: View,
    private val onClose: () -> Unit,
) {

    private companion object {
        val expandRegistry = CollapsibleExpandRegistry<NativeAd>()
    }

    private enum class ControlClosePosition {
        LEFT,
        RIGHT,
    }

    private var inlineTemplateView: BaseNativeTemplateView? = null
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

    /**
     * Closing the ad has to take the popup down with it. The popup lives in its own window, so
     * neither hiding nor detaching the anchor dismisses it, and the host normally reacts to
     * [onClose] by hiding the whole container.
     */
    private val closeAdRequest: () -> Unit = {
        release()
        onClose()
    }

    /**
     * Binds a freshly created inline template together with the native ad it renders.
     *
     * Rebinding the very same ad keeps a showing popup alive: the container recreates its inline
     * template on every load callback, and tearing the popup down for an unchanged ad would make
     * the expanded state flicker away.
     *
     * A pending explicit [setExpanded] request is consumed here, so it decides the state of this
     * ad only. Every later ad starts from the automatic expand rules again, which is what the
     * per-load host view used to give us for free by being recreated.
     */
    fun bind(
        inlineTemplateView: BaseNativeTemplateView,
        nativeAd: NativeAd,
        styles: NativeTemplateStyle,
    ) {
        if (nativeAd !== currentNativeAd) {
            release()
        }
        val isSameShowingExpandedNativeAd =
            popupWindow?.isShowing == true && expandedPopupNativeAd === nativeAd
        this.inlineTemplateView = inlineTemplateView
        currentNativeAd = nativeAd
        applyStyles(styles)
        inlineTemplateView.onClose = closeAdRequest

        val shouldExpand = expandState.shouldExpand(
            isSameShowingExpandedNativeAd = isSameShowingExpandedNativeAd,
            isExpandCooldownActive = isExpandCooldownActive(),
            hasNativeAdShownExpanded = hasNativeAdShownExpanded(nativeAd),
        )
        expandState.reset()
        if (shouldExpand) {
            showExpandedPopup(nativeAd)
        } else {
            showCollapsedInline()
        }
    }

    /**
     * Explicitly expands or collapses the collapsible native ad currently bound, or the next one
     * to be bound when the ad has not arrived yet.
     *
     * Explicit expansion bypasses the automatic cooldown because it is initiated by the host
     * application, so the request is deliberately scoped to a single ad.
     */
    fun setExpanded(expanded: Boolean) {
        expandState.setExpanded(expanded)
        // No ad yet: keep the request pending, bind() consumes it for the ad that arrives.
        val nativeAd = currentNativeAd ?: return
        expandState.reset()
        if (expanded) {
            val isAlreadyExpanded =
                popupWindow?.isShowing == true && expandedPopupNativeAd === nativeAd
            if (!isAlreadyExpanded) {
                showExpandedPopup(nativeAd)
            }
        } else if (!isCollapsedInlineVisible()) {
            showCollapsedInline()
        }
    }

    fun onHostPause() {
        popupTemplateView?.onHostPause()
    }

    fun onHostResume() {
        popupTemplateView?.onHostResume()
    }

    fun onAnchorDetached() {
        collapseWithoutCooldown()
    }

    /**
     * Collapses back to the inline template when the anchor stops being visible. The popup is a
     * separate window, so hiding the anchor alone would leave it floating on screen.
     */
    fun onAnchorHidden() {
        collapseWithoutCooldown()
    }

    /**
     * Dismisses only the expanded popup before an automatic refresh. The current native remains
     * bound to the inline template until its replacement has loaded successfully.
     */
    fun onNativeRefreshStarted() {
        collapseWithoutCooldown()
    }

    private fun collapseWithoutCooldown() {
        // Already inline: rebinding here would only restart the template countdown for nothing.
        if (isCollapsedInlineVisible()) return
        if (inlineTemplateView != null) {
            showCollapsedInline()
        } else {
            dismissExpandedPopup()
        }
    }

    fun release() {
        popupRequestVersion++
        dismissExpandedPopup()
        inlineTemplateView?.onClose = null
        inlineTemplateView = null
        currentNativeAd = null
        currentStyles = null
    }

    private fun applyStyles(styles: NativeTemplateStyle) {
        currentStyles = styles
        collapsibleExpandCooldownSecond =
            styles.collapsibleExpandCooldownSecond?.coerceAtLeast(0) ?: 0
        collapsibleExpandCooldownKey =
            styles.adPlaceName?.takeIf { it.isNotBlank() } ?: javaClass.name
        controlClosePosition = resolveControlClosePosition(styles.controlClosePosition)

        inlineTemplateView?.applyStyles(styles)
        popupTemplateView?.applyStyles(styles)
        applyControlClosePosition()
    }

    private fun showExpandedPopup(nativeAd: NativeAd) {
        val inlineTemplate = inlineTemplateView ?: return
        val requestVersion = ++popupRequestVersion
        inlineTemplate.visibility = View.INVISIBLE

        anchorView.post {
            if (requestVersion != popupRequestVersion) return@post
            if (
                !anchorView.isAttachedToWindow ||
                anchorView.windowToken == null ||
                anchorView.visibility != View.VISIBLE
            ) {
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
                onClose = closeAdRequest
                currentStyles?.let(::applyStyles)
                setNativeAd(nativeAd)
            }
            val popupContent = createPopupContent(expandedTemplate)
            val popup = PopupWindow(
                popupContent,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                false,
            ).apply {
                isOutsideTouchable = false
                isClippingEnabled = true
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                runCatching {
                    elevation = anchorView.resources
                        .getDimensionPixelSize(com.core.dimens.R.dimen._8dp)
                        .toFloat()
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
                }
            }

            runCatching {
                popup.showAsDropDown(anchorView, 0, -resolvePopupHeight(popupContent))
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
            NativeExpandTemplate.V1 -> NativeExpandView(anchorView.context)
            NativeExpandTemplate.V2 -> NativeExpandViewV2(anchorView.context)
        }
    }

    private fun createPopupContent(expandedTemplate: BaseNativeTemplateView): FrameLayout {
        return FrameLayout(anchorView.context).apply {
            addView(
                expandedTemplate,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
            val closeView = AppCompatImageView(context).apply {
                setBackgroundResource(R.drawable.bg_close_collapsible)
                setImageResource(R.drawable.ic_arrow_down_24px)
                imageTintList = ColorStateList.valueOf(Color.BLACK)
                elevation = resources
                    .getDimensionPixelSize(com.core.dimens.R.dimen._5dp)
                    .toFloat()
                setOnClickListener { collapseToInline() }
            }
            popupCloseView = closeView
            addView(
                closeView,
                FrameLayout.LayoutParams(
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
        val inlineTemplate = inlineTemplateView ?: return
        inlineTemplate.visibility = View.VISIBLE
        currentNativeAd?.let(inlineTemplate::setNativeAd)
    }

    private fun isCollapsedInlineVisible(): Boolean {
        return inlineTemplateView?.visibility == View.VISIBLE && popupWindow?.isShowing != true
    }

    private fun dismissExpandedPopup() {
        popupRequestVersion++
        val popup = popupWindow
        popupWindow = null
        popupTemplateView = null
        popupCloseView = null
        expandedPopupNativeAd = null
        popup?.setOnDismissListener(null)
        popup?.dismiss()
    }

    private fun resolvePopupHeight(view: View): Int {
        if (view.minimumHeight > 0) return view.minimumHeight

        val resources = anchorView.resources
        val measuredWidth = anchorView.width.takeIf { it > 0 }
            ?: resources.displayMetrics.widthPixels
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
        val params = closeView.layoutParams as? FrameLayout.LayoutParams ?: return
        params.gravity = resolveCloseGravity(controlClosePosition)
        closeView.layoutParams = params
    }

    private fun isExpandCooldownActive(): Boolean {
        val cooldownMillis = collapsibleExpandCooldownSecond
            .takeIf { it > 0 }
            ?.times(1_000L)
            ?: return false
        return expandRegistry.isCooldownActive(
            key = collapsibleExpandCooldownKey,
            cooldownMillis = cooldownMillis,
            nowMillis = SystemClock.elapsedRealtime(),
        )
    }

    private fun markExpandedClosed() {
        if (collapsibleExpandCooldownSecond <= 0) return
        expandRegistry.markClosed(
            key = collapsibleExpandCooldownKey,
            nowMillis = SystemClock.elapsedRealtime(),
        )
    }

    private fun hasNativeAdShownExpanded(nativeAd: NativeAd): Boolean {
        return expandRegistry.hasExpanded(nativeAd)
    }

    private fun markNativeAdExpandedShown(nativeAd: NativeAd) {
        expandRegistry.markExpanded(nativeAd)
    }
}

internal class CollapsibleExpandState {
    private var manuallyExpanded: Boolean? = null

    fun setExpanded(expanded: Boolean) {
        manuallyExpanded = expanded
    }

    /** Drops a manual request once it has been applied, so the next ad follows the automatic rules. */
    fun reset() {
        manuallyExpanded = null
    }

    /**
     * A newly loaded native ad always gets its expanded state, so the only thing holding an ad
     * back is having been shown expanded already.
     *
     * @param isExpandCooldownActive still reported by the caller but deliberately not part of the
     *   decision: the cooldown must not delay a fresh ad, and an ad that already had its expanded
     *   run is stopped by [hasNativeAdShownExpanded] anyway.
     */
    fun shouldExpand(
        isSameShowingExpandedNativeAd: Boolean,
        isExpandCooldownActive: Boolean,
        hasNativeAdShownExpanded: Boolean,
    ): Boolean {
        manuallyExpanded?.let { return it }
        return isSameShowingExpandedNativeAd || !hasNativeAdShownExpanded
    }
}

internal class CollapsibleExpandRegistry<T : Any> {
    private val lastCloseTimes = mutableMapOf<String, Long>()
    private val expandedItems: MutableSet<T> =
        Collections.newSetFromMap(WeakHashMap<T, Boolean>())

    fun isCooldownActive(
        key: String,
        cooldownMillis: Long,
        nowMillis: Long,
    ): Boolean {
        val lastCloseTime = lastCloseTimes[key] ?: return false
        return nowMillis - lastCloseTime < cooldownMillis
    }

    fun markClosed(key: String, nowMillis: Long) {
        lastCloseTimes[key] = nowMillis
    }

    fun hasExpanded(item: T): Boolean {
        return expandedItems.contains(item)
    }

    fun markExpanded(item: T) {
        expandedItems.add(item)
    }
}
